package it.grational.cache

import spock.lang.*
import java.time.Duration
import it.grational.compression.Gzip
import it.grational.compression.NoCompression

/**
 * Test the correct behaviour of the public methods of 
 * CacheFile class
 */
class CacheFileUSpec extends Specification {
	@Shared
	File tmpFile = new File(System.properties.'java.io.tmpdir','cache.test')

	@Shared
	String fileContent = 'This is the content of the temporary file.'

	def setupSpec() {

		tmpFile.createNewFile()

		tmpFile.write(fileContent)

		def yesterday = new Date() - 1
		tmpFile.setLastModified(yesterday.getTime())

		Number.metaClass.getSeconds { delegate * 1000 }
		Number.metaClass.getMinutes { delegate.seconds * 60 }
		Number.metaClass.getHours   { delegate.minutes * 60 }
		Number.metaClass.getDays    { delegate.hours * 24 }
	}

	def cleanupSpec() {
		tmpFile.delete()
	}

	@Unroll
	def "valid() method should correctly handle lease time"() {
		when: 'create a new CacheFile from the 24h old temp file'
			CacheFile cf = new CacheFile(tmpFile,compressor)
		then: 'A lease time less than 24 hours return false'
			cf.valid(Duration.ofMillis(12.hours)) == false
		and:  'A lease time longer then 24 hours return true'
			cf.valid(Duration.ofMillis(25.hours)) == true
		where:
			compressor << [new Gzip(), new NoCompression()]
	}

	@Unroll
	def "valid() method should recognize actual files from directories"() {
		given: 'a temporary file'
			File tmpDir = new File(System.properties.'java.io.tmpdir')
		when: 'create a new CacheFile from the temporary directory'
			CacheFile cf = new CacheFile(tmpDir,compressor)
		then: 'the valid method return false regardless of the lease time'
			cf.valid(Duration.ofMillis(12.hours)) == false
		and: 'A lease time longer then 24 hours return true'
			cf.valid(Duration.ofMillis(25.hours)) == false
		where:
			compressor << [new Gzip(), new NoCompression()]
	}

	@Unroll
	def "Try to write() some content and retrieve it from the file"() {
		given: 'A CacheFile created from a temporary file'
			CacheFile cf = new CacheFile(tmpFile, compressor)
		when: 'We write the fileContent to the cache file'
			cf.write(fileContent)
		then: 'The content retrieved is equal to that previously written'
			cf.content() == fileContent
		where:
			compressor << [new Gzip(), new NoCompression()]
	}

	def "An empty file should not be valid"() {
		given: 'A file not yet created'
			File randomEmptyFile = this.randomEmptyFile()
		when: 'A CacheFile created from a temporary file'
			CacheFile cf = new CacheFile(randomEmptyFile)
		then: 'The cacheFile is invalid since the file i'
			cf.valid(Duration.ofMillis(5.seconds)) == false

		when:
			cf.write(fileContent)
		then:
			cf.valid(Duration.ofMillis(5.seconds)) == true

		cleanup:
			randomEmptyFile.delete()
	}

	@Unroll
	def "It should be possibile to invalidate a file cache"() {
		when: 'create a new CacheFile from the 24h old temp file'
			CacheFile cf = new CacheFile()
		and: 
			cf.invalidate()
		then: 'after the invalidation the valid method returns always false'
			cf.valid(Duration.ofMillis(12.hours)) == false
			cf.valid(Duration.ofMillis(25.hours)) == false

		where:
			compressor << [new Gzip(), new NoCompression()]
	}

	private File randomEmptyFile() {
		def randomSeed = new Random()
		def randomFilename = "${randomSeed.nextInt()}.${randomSeed.nextInt()}"
		def randomFile = new File (
			String.format (
				"%s/%s",
				System.properties.'java.io.tmpdir',
				randomFilename
			)
		)
		randomFile.createNewFile()
		return randomFile
	}
}
// vim: fdm=indent
