package it.italiaonline.rnd.cache

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GZipEngine {

	GZipEngine() {}

	String zip(String input){

		def targetStream = new ByteArrayOutputStream()

		def zipStream = new GZIPOutputStream(targetStream)
		zipStream.write(input.getBytes('UTF-8'))
		zipStream.close()

		def zippedBytes = targetStream.toByteArray()
		targetStream.close()

		return zippedBytes.encodeBase64()
	}

	String unzip(String gzInput) {
		def inflaterStream = new GZIPInputStream(
		                       new ByteArrayInputStream(
		                         gzInput.decodeBase64()
		                       )
		                     )
		def uncompressedString = inflaterStream.getText('UTF-8')
		return uncompressedString
	}
}
