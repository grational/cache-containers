package it.italiaonline.rnd.cache

final class CacheFile {

	private final File file

	CacheFile(File cfile) {
		this.file = cfile
	}

	Boolean valid(BigInteger leaseTime) {
		(this.file.isFile() && this.newer(leaseTime))
	}

	String content() {
		new GZipEngine().unzip(this.file.text)
	}

	void write(
		String input,
		String charset = 'UTF-8'
	) {
		this.file.getParentFile()?.mkdirs()
		this.file.write(
			new GZipEngine().zip(input),
			charset
		)
	}

	private Boolean newer(BigInteger leaseTime) {
		Long currentEpoch = new Date().getTime()
		Long lastModified = this.file.lastModified()
		( (currentEpoch - lastModified) < leaseTime )
	}

}
