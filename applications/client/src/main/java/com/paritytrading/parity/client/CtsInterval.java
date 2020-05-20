package com.paritytrading.parity.client;

import java.time.*;

public class CtsInterval {
	public static Duration duration = Duration.ZERO;
	public static Instant dtStart;
	
	/*
	 *  Construct Interval from java.time.Duration and Instant
	 * 	instant should be constructed and passed in after application of ZonedDateTime.toInstant() elsewhere
	 *  duration is number of minutes, converted in the constructor
	 */
	public CtsInterval(long durationInMinutes, Instant dtStart){
		this.duration = Duration.ofSeconds(60*durationInMinutes);
		this.dtStart = dtStart;
	}
	
	@Override
	public String toString()	{
		Duration duration = this.getDuration();
		
		return ("Interval duration " + duration.toString() +
				" dtStart " + 
				this.dtStart.toString());
	}

	public static Duration getDuration() {
		return duration;
	}

	public static void setDuration(Duration duration) {
		CtsInterval.duration = duration;
	}

	public static Instant getDtStart() {
		return dtStart;
	}

	public static void setDtStart(Instant dtStart) {
		CtsInterval.dtStart = dtStart;
	}
}
