package it.italiaonline.rnd.cache

import spock.lang.Specification
import spock.lang.Shared

/**
 * The Sha1Hash class returns the sha1hash of a string passed
 * as parameter.
 */
class Sha1SumSpec extends Specification {

	@Shared
	String shortString = 'testing string'

	@Shared
	String longString = """\
	|This is a very long sentence, so long that can't be
	|placed on a single line but it needs to be splitted
	|on multiple lines. To keep the number of columns
	|used as low as possible in addiction a margin is used
	|with the groovy stripMargin() method to remove it
	|when needed.""".stripMargin()

	/**
	 * The hash returned by the class should be equal to the
	 * precalculated one
	 */
	def "The hash returned by Sha1Sum should be equal to the precalculated one"() {

		expect:
			String generatedHash = new Sha1Sum(input).digest()
			generatedHash == precalculatedHash

		where:
			input       | precalculatedHash
			longString  | '63ad60bbcf52f3683a542e2cd09d10aaa3873bd1'
			shortString | '8ddc1738150524a38bbb2c3e667c2c14f091534d'
			''          | 'da39a3ee5e6b4b0d3255bfef95601890afd80709'
	}

}
