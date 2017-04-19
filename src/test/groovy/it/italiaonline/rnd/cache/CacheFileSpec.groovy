package it.italiaonline.rnd.cache

import spock.lang.Specification
import spock.lang.Shared
import java.time.Duration
import it.italiaonline.rnd.compression.GZipEngine
import it.italiaonline.rnd.compression.NoCompression

/**
 * Test the correct behaviour of the public methods of 
 * CacheFile class
 */
class CacheFileSpec extends Specification {
	@Shared
	File tmpFile = new File(System.properties.'java.io.tmpdir','cache.test')

	@Shared
	String fileContent = 'This is the content of the temporary file.'

	def setupSpec() {

		tmpFile.createNewFile()

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

	def "valid() method should correctly handle lease time"() {
		when: 'create a new CacheFile from the 24h old temp file'
			CacheFile cf = new CacheFile(tmpFile,compressor)
		then: 'A lease time less than 24 hours return false'
			cf.valid(Duration.ofMillis(12.hours)) == false
		and:  'A lease time longer then 24 hours return true'
			cf.valid(Duration.ofMillis(25.hours)) == true
		where:
			compressor << [new GZipEngine(), new NoCompression()]
	}

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
			compressor << [new GZipEngine(), new NoCompression()]
	}

	def "Try to write() some content and retrieve it from the file"() {
		given: 'A CacheFile created from a temporary file'
			CacheFile cf = new CacheFile(tmpFile, compressor)
		when: 'We write the fileContent to the cache file'
			cf.write(fileContent)
		then: 'The content retrieved is equal to that previously written'
			cf.content() == fileContent
		where:
			compressor << [new GZipEngine(), new NoCompression()]
	}
}
// vim: fdm=indent
