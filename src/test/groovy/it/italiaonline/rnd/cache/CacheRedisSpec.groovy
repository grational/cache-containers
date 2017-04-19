package it.italiaonline.rnd.cache

import spock.lang.Specification
import spock.lang.Shared
import spock.lang.Unroll
import java.time.Duration
import redis.clients.jedis.Jedis
import redis.clients.jedis.exceptions.JedisDataException
import it.italiaonline.rnd.compression.GZipEngine
import it.italiaonline.rnd.compression.NoCompression

/**
 * Test the correct behaviour of the public methods of
 * CacheRedis class
 */
class CacheRedisSpec extends Specification {

	@Shared Jedis    jedis
	@Shared String   existingKey   = 'existingKey'
	@Shared String   keyContent    = 'this is the content of the cache file'
	@Shared Long     keyTTL        = Duration.ofHours(100).seconds
	@Shared Duration expireTime    = Duration.ofDays(5)

	@Unroll
	def 'Should be a valid object if all parameters are ok using #compressor'() {
		given: 'a stub jedis client implementation'
			jedis = Stub()
		when: 'instanciate a CacheRedis class with all the fields not null'
			CacheRedis cr = new CacheRedis (
				jedis,        // Jedis jedis
				existingKey,  // String key
				compressor,   // CompressionEngine ce
				expireTime    // Duration expireTime
			)
		then: 'no exception is thrown'
			noExceptionThrown()
		where:
			compressor << [new GZipEngine(), new NoCompression()]
	}

	@Unroll
	def 'Should raise NullPointerException if one among #jd, #key, #time is null'() {
		given: 'a stub jedis client implementation'
			jedis = Stub()
		when: 'the constructor with no password is invoked with at lease one param equal to null'
			CacheRedis cr = new CacheRedis (
				jd,         // jedis object
				key,        // key
				compressor, // CompressionEngine ce
				time        // expireTime
			)

		then: 'a NullPointerException is thrown'
			def error = thrown(expectedException)

		where:
			jd    | key         | time       || expectedException
			null  | existingKey | expireTime || NullPointerException
			jedis | null        | expireTime || NullPointerException
			jedis | existingKey | null       || NullPointerException
			compressor << [new GZipEngine(), new NoCompression(), new GZipEngine()]
	}

	@Unroll
	def "valid() method should correctly handle lease time using #compressor"() {
		given: 'a mock jedis client implementation'
			jedis = Mock()
		and: 'a CacheRedis implementation'
			CacheRedis cr = new CacheRedis (
				jedis,        // Jedis jedis
				existingKey,  // String key
				compressor,   // CompressionEngine ce
				expireTime    // Duration expireTime
			)
		and:
			long fifteenHoursAgo = java.time.Instant.now().epochSecond - Duration.ofHours(15).seconds

		when: 'it fails trying to validate the key as newer then 12 hours'
			def result = cr.valid(Duration.ofHours(12))
		then: 'A lease time less than 24 hours return false'
			1 * jedis.exists("${existingKey}:content") >> true
			1 * jedis.exists("${existingKey}:timestamp") >> true
			1 * jedis.get("${existingKey}:timestamp") >> fifteenHoursAgo
			result == false

		when: 'it succeds trying to validate the key as newer then 25 hours'
			result = cr.valid(Duration.ofHours(25))
		then:  'A lease time longer then 24 hours return true'
			1 * jedis.exists("${existingKey}:content") >> true
			1 * jedis.exists("${existingKey}:timestamp") >> true
			1 * jedis.get("${existingKey}:timestamp") >> fifteenHoursAgo
			result == true

		where:
			compressor << [new GZipEngine(), new NoCompression()]
	}

	@Unroll
	def "Should raise an exception if an existing key content is requested using #compressor"() {
		given: 'a mock jedis implementation and a non existing key'
			jedis = Mock()
			String nonExistingKey = 'nonExistingKey'
		and: 'a CacheRedis implementation'
			CacheRedis cr = new CacheRedis (
				jedis,          // Jedis jedis
				nonExistingKey, // String key
				compressor,     // CompressionEngine ce
				expireTime      // Duration expireTime
			)
		when: 'call the content method'
			cr.content()
		then:
			1 * jedis.get("${nonExistingKey}:content") >> null
			thrown(IllegalStateException)
		where:
			compressor << [new GZipEngine(), new NoCompression()]
	}

	@Unroll
	def "Should return the compressed content of an existing key using #compressor"() {
		given: 'a mock jedis implementation and a non existing key'
			jedis = Mock()
		and: 'a CacheRedis implementation'
			CacheRedis cr = new CacheRedis (
				jedis,        // Jedis jedis
				existingKey,  // String key
				compressor,   // CompressionEngine ce
				expireTime    // Duration expireTime
			)
		when: 'ask for the content of the existing key'
			def result = cr.content()
		then:
			1 * jedis.get("${existingKey}:content") >> {
				compressor.compress(keyContent)
			}
			result == keyContent
		where:
			compressor << [new GZipEngine(), new NoCompression()]
	}

	@Unroll
	def "Should be possible to write a key and retrieve its content"() {
		given: 'a mock jedis implementation and a non existing key'
			jedis = Mock()
		and: 'an about to be inserted key'
			String newKey = 'newKey'
		and: 'a CacheRedis implementation'
			CacheRedis cr = new CacheRedis (
				jedis,        // Jedis jedis
				newKey,       // String key
				compressor,   // CompressionEngine ce
				expireTime    // Duration expireTime
			)
		when: 'check for the key to exists'
			cr.valid(Duration.ofDays(2))
		then: 'obtain false'
			1 * jedis.exists("${newKey}:content") >> false

		when: 'actually write the content and try to retrieve it'
			cr.write(keyContent)
			def result = cr.content()
		then:
			1 * jedis.set("${newKey}:content",compressor.compress(keyContent))
			1 * jedis.expire("${newKey}:content",expireTime.seconds as Integer)
			1 * jedis.get("${newKey}:content") >> {
				compressor.compress(keyContent)
			}
			result == keyContent
		where:
			compressor << [new GZipEngine(), new NoCompression()]
	}
}
// vim: fdm=indent
