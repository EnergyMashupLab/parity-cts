package com.paritytrading.parity.client;

import java.time.*;

public class CtsInterval {
	public Duration duration = Duration.ZERO;
	public Instant dtStart;
	
	/*
	 *  Construct Interval from java.time.Duration and Instant
	 * 	instant should be constructed and passed in after application of ZonedDateTime.toInstant() elsewhere
	 *  duration is number of minutes, converted in the constructor
	 */
	/*
	 * System default 60 minute intervals
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

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public Instant getDtStart() {
		return dtStart;
	}

	public void setDtStart(Instant dtStart) {
		this.dtStart = dtStart;
	}
}
