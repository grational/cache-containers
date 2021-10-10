package it.grational.cache

import java.time.Duration
import it.grational.compression.Compressor
import it.grational.compression.NoCompression

final class CacheFile implements CacheContainer {

	private final File file
	private final Compressor compressor

	CacheFile (
		File cfile = File.createTempFile('temp','.cache'),
		Compressor ce = new NoCompression()
	) {
		this.file = cfile
		if (!this.file.getParentFile().isDirectory())
			throw new IllegalArgumentException("The parent directory of '${cfile} does not exists!")
		this.compressor = ce
	}

	Boolean valid(Duration leaseTime) {
		this.file.isFile() && this.newer(leaseTime)
	}

	String content() {
		this.compressor.uncompress(this.file.text)
	}

	void write(String input, String charset = 'UTF-8') {
		this.file.write (
			this.compressor.compress(input),
			charset
		)
	}

	private Boolean newer(Duration leaseTime) {
		Long currentEpoch = new Date().getTime()
		Long lastModified = this.file.lastModified()
		( (currentEpoch - lastModified) < leaseTime.toMillis() )
	}

}
