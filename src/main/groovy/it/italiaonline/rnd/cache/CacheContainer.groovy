package it.italiaonline.rnd.cache

import java.time.Duration

interface CacheContainer {
	Boolean valid(Duration leaseTime)

	String content()

	void write(String input)
}
