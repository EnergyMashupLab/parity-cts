package com.paritytrading.parity.client;

/*
 * Sent by the Market to the LME with information from a market trade
 * 
 * Information will define an EiCreateTransactionPayload via
 * this MarketCreateTransactionPayload
 * 
 * MANUAL SYNCHRONIZATION BETWEEN SEPARATE PARITY and CTS Applications
 */
public class MarketCreateTransactionPayload {
	private String info = "MarketCreateTransactionPayload";
	private SideType side;
	long quantity = 0;	// quantity transacted. At least two Transactions created for a match
	long price = 0;		//price may not be that tendered
	long ctsTenderId = 0;	// set before sending -- Map maintained by CtsBridge	
	String parityOrderId = null;
	long matchNumber = 0;	// parity matchNumber for this match
	
	
	MarketCreateTransactionPayload()	{	// json
	}
	
	MarketCreateTransactionPayload(String parityOrderId,
			long ctsTenderId,
			long quantity,
			long price,
			long matchNumber,
			SideType side)	{
		this.parityOrderId = parityOrderId;
		this.ctsTenderId = ctsTenderId;
		this.quantity = quantity;
		this.price = price;
		this.matchNumber = matchNumber;
		this.side = side;
	}
	
	@Override
	public String toString()	{
		SideType tempSide = this.side;
		String tempString;	

		tempString = (tempSide == SideType.BUY)? "B" : "S";		
		return (info + " parityOrderId " + parityOrderId +
				" ctsTenderId " + ctsTenderId +
				" side " + tempString +
				" quantity " + quantity 
				+ " price " + price +
				" matchNumber " + matchNumber);
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

	public String getParityOrderId() {
		return parityOrderId;
	}

	public void setParityOrderId(String parityOrderId) {
		this.parityOrderId = parityOrderId;
	}

	public long getMatchNumber() {
		return matchNumber;
	}

	public void setMatchNumber(long matchNumber) {
		this.matchNumber = matchNumber;
	}

}
