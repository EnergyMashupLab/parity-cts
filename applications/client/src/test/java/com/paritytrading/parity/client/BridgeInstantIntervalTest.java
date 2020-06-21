package com.paritytrading.parity.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Bridge Instant/Interval tests")
public class BridgeInstantIntervalTest {

	@Nested
	@DisplayName("BridgeInstant tests")
	class BridgeInstantTest {
		@Test
		@DisplayName("Instant to String instrument name")
		void toInstrumentNameTest() {
			BridgeInstant brInstant = new BridgeInstant(Instant.parse("2020-06-20T12:00:00Z"));

			assertEquals("06201200", brInstant.toInstrumentName());
		}

		@Test
		@DisplayName("Instant to instrument name with 24 hour format")
		void toInstrumentName24hFormatTest() {
			BridgeInstant brInstant = new BridgeInstant(Instant.parse("2020-06-20T13:00:00Z"));

			assertEquals("06201300", brInstant.toInstrumentName());
		}

		@Test
		@DisplayName("Instant to packedLong instrument name")
		void toPackedLongTest() {
			BridgeInstant brInstant = new BridgeInstant(Instant.parse("2020-06-20T12:00:00Z"));

			assertEquals(3474019345128108080L, brInstant.toPackedLong());
		}

		@Test
		@DisplayName("Instant to packedLong instrument name with 24 hour format")
		void toPackedLong24hFormatTest() {
			BridgeInstant brInstant = new BridgeInstant(Instant.parse("2020-06-20T13:00:00Z"));

			assertEquals(3474019345128173616L, brInstant.toPackedLong());
		}
	}


	@Nested
	@DisplayName("BridgeInterval tests")
	class BridgeIntervalTest {
		@Test
		@DisplayName("Instant to instrument name with 24 hour format")
		void toInstrumentNameTest() {
			BridgeInterval brInterval =
					new BridgeInterval(60, Instant.parse("2020-06-20T12:00:00Z"));

			assertEquals("06201200", brInterval.toInstrumentName());
		}

		@Test
		@DisplayName("Instant to instrument name with 24 hour format")
		void toInstrumentName24hFormatTest() {
			BridgeInterval brInterval =
					new BridgeInterval(60, Instant.parse("2020-06-20T13:00:00Z"));

			assertEquals("06201300", brInterval.toInstrumentName());
		}

		@Test
		@DisplayName("Instant to packedLong instrument name")
		void toPackedLongTest() {
			BridgeInterval brInterval =
					new BridgeInterval(60, Instant.parse("2020-06-20T12:00:00Z"));

			assertEquals(3474019345128108080L, brInterval.toPackedLong());
		}

		@Test
		@DisplayName("Instant to packedLong instrument name with 24 hour format")
		void toPackedLong24hFormatTest() {
			BridgeInterval brInterval =
					new BridgeInterval(60, Instant.parse("2020-06-20T13:00:00Z"));

			assertEquals(3474019345128173616L, brInterval.toPackedLong());
		}
	}
}
