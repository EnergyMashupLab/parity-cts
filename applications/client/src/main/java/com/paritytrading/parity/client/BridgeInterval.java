package com.paritytrading.parity.client;

/*
 * DIFFERS from CTS Interval due to JSON serialization issues
 * 
 * 	Instant works fine
 * 	Duration does not
 * 
 * duration attribute is integral minutes, rather than JDK8 java.time.Duration
 */

/*
 * This class provides a workaround Jackson serialization problem.
 * 
 * 		Investigate SpringBoot - it doesn't have this issue, unravel imports
 *
 *		Inside CtsBridge/Client use this specialized interval that maintains string instant and duration
 *		in minutes.
 *
 *		Setters and Getters create java.time Instant and Duration for other integration.
 */

import java.time.Duration;
import java.time.Instant;
import java.time.*;
import java.time.Duration;

// doesn't help
import com.fasterxml.jackson.datatype.jsr310.*;

public class BridgeInterval {
	// only for JSON serialization over sockets between CTS and Parity
	//	- see note on Spring Boot
	
	private long durationInMinutes = 0;	//integral minutes
	private BridgeInstant dtStart;
	
	// TODO compare with CTS implementation; Jackson can't serialize java.time.Duration. First add
	// explicit import java.time.Duration
	
	/*
	 *  Construct Interval from java.time.Duration and Instant
	 * 	instant should be constructed and passed in after application of ZonedDateTime.toInstant() elsewhere
	 *  duration is number of minutes, converted in the constructor
	 */
	
	BridgeInterval()	{
		dtStart = new BridgeInstant(Instant.now());	// a reasonable default
	}
	
	BridgeInterval(long durationInMinutes, Instant dtStart){
		this.durationInMinutes = durationInMinutes;
		this.dtStart = new BridgeInstant(dtStart);
	}
	
	BridgeInterval(CtsInterval ctsInterval)	{
		this.durationInMinutes = ctsInterval.getDuration().toMinutes();
		this.dtStart = new BridgeInstant(ctsInterval.dtStart);
	}
	
	
	CtsInterval asInterval()	{
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

	
	
}
