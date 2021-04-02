/*
 * Copyright 2019-2020 The Energy Mashup Lab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.paritytrading.parity.client;

/*
 * DIFFERS from CTS Interval due to JSON serialization issues
 * 
 * Instant works fine Duration does not
 * 
 * duration attribute is integral minutes, rather than JDK8 java.time.Duration
 */

/*
 * This class provides a workaround Jackson serialization problem.
 * 
 * Investigate SpringBoot - it doesn't have this issue, unravel imports
 *
 * Inside CtsBridge/Client use this specialized interval that maintains string instant and duration
 * in minutes.
 *
 * Setters and Getters create java.time Instant and Duration for other integration.
 */

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.paritytrading.foundation.ASCII;

public class BridgeInterval {
	// only for JSON serialization over sockets between CTS and Parity
	// - see note on Spring Boot

	private long durationInMinutes = 0; // integral minutes
	private BridgeInstant dtStart;
	private static final DateTimeFormatter INSTANT_INSTRUMENT_FORMATTER =
			DateTimeFormatter.ofPattern("MMddHHmm").withZone(ZoneId.of("Z"));

	// TODO compare with CTS implementation; Jackson can't serialize java.time.Duration. First add
	// explicit import java.time.Duration

	/*
	 * Construct Interval from java.time.Duration and Instant instant should be constructed and
	 * passed in after application of ZonedDateTime.toInstant() elsewhere duration is number of
	 * minutes, converted in the constructor
	 */

	public BridgeInterval() {
		dtStart = new BridgeInstant(Instant.now()); // a reasonable default
	}

	BridgeInterval(long durationInMinutes, Instant dtStart) {
		this.durationInMinutes = durationInMinutes;
		this.dtStart = new BridgeInstant(dtStart);
	}

	BridgeInterval(CtsInterval ctsInterval) {
		this.durationInMinutes = ctsInterval.getDuration().toMinutes();
		this.dtStart = new BridgeInstant(ctsInterval.dtStart);
	}

	CtsInterval asInterval() {
		return new CtsInterval(durationInMinutes, dtStart.asInstant());
	}

	public long getDurationInMinutes() {
		return durationInMinutes;
	}

	public void setDurationInMinutes(long durationInMinutes) {
		this.durationInMinutes = durationInMinutes;
	}

	public BridgeInstant getDtStart() {
		return dtStart;
	}

	public void setDtStart(BridgeInstant dtStart) {
		this.dtStart = dtStart;
	}

	/**
	 * Converts dtStart to a packed long using {@link ASCII}.
	 * 
	 * @return packed long
	 */
	public long toPackedLong() {
		return ASCII.packLong(INSTANT_INSTRUMENT_FORMATTER.format(this.dtStart.asInstant()));
	}

	/**
	 * Converts {@link #dtStart} to an instrument name.
	 * 
	 * @return string matching the form {@code MMddHHmm}
	 */
	public String toInstrumentName() {
		return INSTANT_INSTRUMENT_FORMATTER.format(this.dtStart.asInstant());
	}
}
