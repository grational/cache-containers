package it.grational.cache

import java.time.Duration
import redis.clients.jedis.commands.JedisCommands
import it.grational.compression.Compressor
import it.grational.compression.NoCompression
import java.time.Instant

final class CacheRedis implements CacheContainer {

	private final String            cacheKey
	private final JedisCommands     jedis
	private final Duration          expireTime
	private final Compressor compressor

	CacheRedis (
		JedisCommands jd,
		String key,
		Duration expire = null,
		Compressor ce = new NoCompression()
	) {
		this.jedis      = Objects.requireNonNull(jd)
		this.cacheKey   = Objects.requireNonNull(key)
		this.expireTime = expire
		this.compressor = ce
	}

	@Override
	Boolean valid(Duration leaseTime) {
		this.jedis.exists("${this.cacheKey}:content")
		&& this.jedis.exists("${this.cacheKey}:timestamp")
		&& this.newer(Objects.requireNonNull(leaseTime))
	}

	@Override
	String content() {
		def result = this.jedis.get("${this.cacheKey}:content")
		if (result == null)
			throw new IllegalStateException("No value for key '${this.cacheKey}:content'")
		this.compressor.uncompress(result)
	}

	@Override
	void write(String input) {
		[
			content: this.compressor.compress(input),
			timestamp: Instant.now().epochSecond
		].each { subkey, value ->
			def key = "${this.cacheKey}:${subkey}" as String
			this.jedis.set(key, value as String)
			if ( this.expireTime ) {
				this.jedis.expire(key,this.expireTime.seconds)
			}
		}
	}

	@Override
	void invalidate() {
		this.jedis.del("${this.cacheKey}:content")
		this.jedis.del("${this.cacheKey}:timestamp")
	}

	private Boolean newer(Duration leaseTime) {
		def keyCreationTime = this.jedis.get("${this.cacheKey}:timestamp") as long
		def currentTime  = Instant.now().epochSecond
		def howOldInSeconds = currentTime - keyCreationTime
		( howOldInSeconds < leaseTime.seconds )
	}
}
