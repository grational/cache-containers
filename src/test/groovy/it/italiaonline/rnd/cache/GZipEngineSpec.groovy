package it.italiaonline.rnd.cache

import spock.lang.Specification
import spock.lang.Shared

/**
 * The ZipEngine class should comress and encode base64
 * the string passes as input
 */
class GZipEngineSpec extends Specification {

	@Shared
	GZipEngine gze = new GZipEngine()

	@Shared
	String longString = """\
	|This is a very long sentence, so long that can't be
	|placed on a single line but it needs to be splitted
	|on multiple lines. To keep the number of columns
	|used as low as possible in addiction a margin is used
	|with the groovy stripMargin() method to remove it
	|when needed""".stripMargin()

	/**
	 * The compressed string should be smaller that the
	 * uncompressed one.
	 */
	def "Should correctly reduce the length of a string"() {
		when:
			String compressed = gze.zip(longString)
		then:
			compressed.size() < longString.size()
	}

	/**
	 * Uncompressing a compressed string should return the
	 * original string.
	 */
	def "Uncompressing a compressed string should return the original string"() {
		when:
			String compressed   = gze.zip(longString)
			String uncompressed = gze.unzip(compressed)
		then:
			longString == uncompressed
	}

}
