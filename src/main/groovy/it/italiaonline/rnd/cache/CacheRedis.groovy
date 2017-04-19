package it.italiaonline.rnd.cache

import java.time.Duration
import redis.clients.jedis.Jedis
import it.italiaonline.rnd.compression.CompressionEngine
import java.time.Instant

final class CacheRedis implements CacheContainer {

	private final String            cacheKey
	private final Jedis             jedis
	private final Duration          expireTime
	private final CompressionEngine compressor

	CacheRedis (
		Jedis             jd,
		String            key,
		CompressionEngine ce,
		Duration          expire
	) {
		this.jedis      = Objects.requireNonNull(jd)
		this.cacheKey   = Objects.requireNonNull(key)
		this.compressor = Objects.requireNonNull(ce)
		this.expireTime = Objects.requireNonNull(expire)
	}

	Boolean valid(Duration leaseTime) {
		this.jedis.exists("${this.cacheKey}:content") && this.jedis.exists("${this.cacheKey}:timestamp") && this.newer(Objects.requireNonNull(leaseTime))
	}

	String content() {
		def result = this.jedis.get("${this.cacheKey}:content")
		if (result == null)
			throw new IllegalStateException("No value for key '${this.cacheKey}:content'")
		this.compressor.uncompress(result)
	}

	void write(String input) {
		[
			content: this.compressor.compress(input),
			timestamp: Instant.now().epochSecond
		].each { subkey, value ->
			def key = "${this.cacheKey}:${subkey}" as String
			def seconds = this.expireTime.seconds as Integer
			this.jedis.set(key, value as String)
			this.jedis.expire(key,seconds)
		}
	}

	private Boolean newer(Duration leaseTime) {
		def keyCreationTime = this.jedis.get("${this.cacheKey}:timestamp") as long
		def currentTime  = Instant.now().epochSecond
		def howOldInSeconds = currentTime - keyCreationTime
		( howOldInSeconds < leaseTime.seconds )
	}
}
