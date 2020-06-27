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

import java.time.Duration;
import java.time.Instant;
import java.time.*;

import com.paritytrading.parity.client.CtsInterval;

/*
 * Sent by the LME to the Market with information to be
 * mapped and inserted in a market engine order.
 * 
 * Information is from an EiCreateTenderPayload received from the LMA
 * and used as constructor parameters by the LME.
 * 
 * MANUAL SYNCHRONIZATION BETWEEN SEPARATE PARITY and CTS Applications
 */

public class MarketCreateTenderPayload {	
	private String info = "MarketCreateTenderPayload";
	private SideType side;
	private long quantity;
	private long price;
	private long ctsTenderId;
	BridgeInterval bridgeInterval;
	BridgeInstant expireTime;

	MarketCreateTenderPayload(SideType side,
			long quantity,
			long price,
			long ctsTenderId,
			CtsInterval interval,
			Instant expireTime)	{
		/*
		 * Ensure that the number of decimal fraction digits
		 * in price and quantity align with the global one which is planned to be 3
		 */
		this.side = side;
		this.quantity = quantity;
		this.price = price;
		this.ctsTenderId = ctsTenderId;
		this.bridgeInterval = new BridgeInterval(interval);
		this.expireTime = new BridgeInstant(expireTime);	
	}
	
	// Default constructor for JSON
	MarketCreateTenderPayload()	{
	}
	
	@Override
	public String toString()	{
		SideType tempSide = this.side;
		String tempString;	

		tempString = (tempSide == SideType.BUY)? "B" : "S";	
		
//		System.err.println(
//				"MarketCreateTenderPayload.toString interval to instrument '" +
//				this.getBridgeInterval().toInstrumentName() + "' " + 
//				" " + this.getBridgeInterval().asInterval().toString() + "' as cts interval "
//				);
		
		return (info + " side " + tempString + " quantity " +
				quantity + " price " + price +
				" CtsTenderId " + ctsTenderId +
				" bridgeInterval " + this.bridgeInterval.asInterval().toString()     +
				" expireTime " + this.expireTime.toString()
				);
		
		}
	
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MarketCreateTenderPayload tenderPayload = (MarketCreateTenderPayload) o;
        
        return	info.equals(tenderPayload.info) 	&&
        		side == tenderPayload.side 			&&
        		quantity == tenderPayload.quantity 	&&
        		price == tenderPayload.price		&&
        		ctsTenderId == tenderPayload.ctsTenderId;
        	// failed, debug ok
        	//	&& expireTime.equals(tenderPayload.expireTime);
    }

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public SideType getSide() {
		return side;
	}

	public void setSide(SideType side) {
		this.side = side;
	}

	public long getQuantity() {
		return quantity;
	}

	public void setQuantity(long quantity) {
		this.quantity = quantity;
	}

	public long getPrice() {
		return price;
	}

	public void setPrice(long price) {
		this.price = price;
	}

	public long getCtsTenderId() {
		return ctsTenderId;
	}

	public void setCtsTenderId(long ctsTenderId) {
		this.ctsTenderId = ctsTenderId;
	}

	public BridgeInterval getBridgeInterval() {
		return bridgeInterval;
	}

	public void setBridgeInterval(BridgeInterval bridgeInterval) {
		this.bridgeInterval = bridgeInterval;
	}

	public BridgeInstant getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(BridgeInstant expireTime) {
		this.expireTime = expireTime;
	}

}