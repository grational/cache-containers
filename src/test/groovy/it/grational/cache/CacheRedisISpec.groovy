package it.grational.cache

import spock.lang.*
import java.time.Duration
import java.time.Instant
import redis.clients.jedis.commands.JedisCommands
import redis.clients.jedis.exceptions.JedisDataException
import it.grational.compression.Gzip
import it.grational.compression.NoCompression

/**
 * Test the correct behaviour of the public methods of
 * CacheRedis class
 */
class CacheRedisISpec extends Specification {

	@Shared JedisCommands jedis
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
				expireTime,   // Duration expireTime
				compressor    // Compressor ce
			)
		then: 'no exception is thrown'
			noExceptionThrown()
		where:
			compressor << [new Gzip(), new NoCompression()]
	}

	@Unroll
	def 'Should raise NullPointerException if one among #jd, #key, #time is null'() {
		given: 'a stub jedis client implementation'
			jedis = Stub()
		when: 'the constructor with no password is invoked with at lease one param equal to null'
			CacheRedis cr = new CacheRedis (
				jd,         // jedis object
				key,        // key
				time,       // expireTime
				compressor  // Compressor ce
			)

		then: 'a NullPointerException is thrown'
			def error = thrown(expectedException)

		where:
			jd    | key         | time       || expectedException
			null  | existingKey | expireTime || NullPointerException
			jedis | null        | expireTime || NullPointerException
			compressor << [new Gzip(), new NoCompression()]
	}

	@Unroll
	def "valid() method should correctly handle lease time using #compressor"() {
		given: 'a mock jedis client implementation'
			jedis = Mock()
		and: 'a CacheRedis implementation'
			CacheRedis cr = new CacheRedis (
				jedis,        // Jedis jedis
				existingKey,  // String key
				expireTime,   // Duration expireTime
				compressor    // Compressor ce
			)
		and:
			long fifteenHoursAgo = Instant.now().epochSecond - Duration.ofHours(15).seconds

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
			compressor << [new Gzip(), new NoCompression()]
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
				expireTime,     // Duration expireTime
				compressor      // Compressor ce
			)
		when: 'call the content method'
			cr.content()
		then:
			1 * jedis.get("${nonExistingKey}:content") >> null
			thrown(IllegalStateException)
		where:
			compressor << [new Gzip(), new NoCompression()]
	}

	@Unroll
	def "Should return the compressed content of an existing key using #compressor"() {
		given: 'a mock jedis implementation and a non existing key'
			jedis = Mock()
		and: 'a CacheRedis implementation'
			CacheRedis cr = new CacheRedis (
				jedis,        // Jedis jedis
				existingKey,  // String key
				expireTime,   // Duration expireTime
				compressor    // Compressor ce
			)
		when: 'ask for the content of the existing key'
			def result = cr.content()
		then:
			1 * jedis.get("${existingKey}:content") >> {
				compressor.compress(keyContent)
			}
			result == keyContent
		where:
			compressor << [new Gzip(), new NoCompression()]
	}

	@Unroll
	def "Should be possible to write a key and retrieve its content"() {
		given: 'a mock jedis implementation and a non existing key'
			jedis = Mock()
		and: 'an about to be inserted key'
			String newKey = 'newKey'
		and: 'a CacheRedis implementation'
			CacheRedis cr = new CacheRedis (
				jedis,      // JedisCommands jedis
				newKey,     // String key
				expireTime, // Duration expireTime
				compressor  // Compressor ce
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
			1 * jedis.get("${newKey}:content") >> {
				compressor.compress(keyContent)
			}
			result == keyContent
		where:
			// compressor << [new Gzip(), new NoCompression()]
			compressor << [new NoCompression()]
	}

	@Unroll
	def "Should be possible to invalidate a valid key"() {
		given: 'a mock jedis implementation and an existing key'
			jedis = Mock()
		and: 'an about to be inserted key'
			String existingKey = 'existingKey'
		and: 'a CacheRedis implementation'
			CacheRedis cr = new CacheRedis (
				jedis,        // Jedis jedis
				existingKey,  // String key
				expireTime,   // Duration expireTime
				compressor    // Compressor ce
			)
		when: 'check for the key to exists'
			cr.valid(Duration.ofDays(2))
		then: 'obtain false'
			1 * jedis.exists("${existingKey}:content") >> true
			1 * jedis.exists("${existingKey}:timestamp") >> true
			1 * jedis.get("${existingKey}:timestamp") >> Instant.now().epochSecond

		when: 'call the invalidate key'
			cr.invalidate()
		then:
			cr.valid() == false
			1 * jedis.del("${existingKey}:content")
			1 * jedis.del("${existingKey}:timestamp")
		where:
			compressor << [new Gzip(), new NoCompression()]
	}

	def "Make the expire parameter optional"() {
		given: 'a mock jedis implementation and an existing key'
			jedis = Mock()
		and: 'an about to be inserted key'
			String newKey = 'newKey'
		and: 'a CacheRedis implementation'
			CacheRedis cr = new CacheRedis (
				jedis,        // Jedis jedis
				newKey        // String key
			)
		when: 'check for the key to exists'
			cr.valid(Duration.ofDays(2))
		then: 'obtain false'
			1 * jedis.exists("${newKey}:content") >> false

		when: 'actually write the content and try to retrieve it'
			cr.write(keyContent)
			def result = cr.content()
		then:
			1 * jedis.set("${newKey}:content",keyContent)
			0 * jedis.expire("${newKey}:content",Instant.now().epochSecond)
			1 * jedis.get("${newKey}:content") >> keyContent
			result == keyContent
	}

}
// vim: fdm=indent
