package com.paritytrading.parity.client;

import java.time.Duration;
import java.time.Instant;

/*
 * Sent by the LME to the Market with information to be
 * mapped and inserted in the market engine
 * 
 * Information is from an EiCreateTenderPayload received from the LMA
 * and used as constructor parameters by the LME Actor.
 */

public class MarketCreateTenderPayload {	
	protected String info = "MarketCreateTenderPayload";
	protected SideType side;
	protected long quantity;
	protected long price;
	protected long ctsTenderId;
	protected Instant expireTime = null;

	MarketCreateTenderPayload(SideType side, long quantity, long price, long ctsTenderId, Instant expireTime)	{
		/*
		 * Ensure that the number of decimal fraction digits
		 * in price and quantity align with the global one which is planned to be 3
		 */
		this.side = side;
		this.quantity = quantity;
		this.price = price;
		this.ctsTenderId = ctsTenderId;
		this.expireTime = expireTime;
//		System.err.println(this.toString());
	}
	
	// Default constructor for JSON
	MarketCreateTenderPayload()	{
	}
	
	@Override
	public String toString()	{
		SideType tempSide = this.side;
		String tempString;	

	tempString = (tempSide == SideType.BUY)? "B" : "S";
		
		return (info + " side " + tempString + " quantity " +
				quantity + " price " + price);
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

	public Instant getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Instant expireTime) {
		this.expireTime = expireTime;
	}
    
}