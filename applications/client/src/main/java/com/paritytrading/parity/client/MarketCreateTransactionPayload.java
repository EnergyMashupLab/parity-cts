package com.paritytrading.parity.client;

// TODO copied from RestTestbedTypes manual synchronization

public class MarketCreateTransactionPayload {
	String parityOrderId = null;
	long quantity = 0;	// quantity transacted. At least two Transactions created for a match
	long price = 0;		//price may not be that tendered
	long matchNumber = 0;	// parity matchNumber for this match
	long ctsTenderId = 0;	// set before sending -- Map maintained by CtsBridge	
	private String info = "MarketCreateTransactionPayload";
	private SideType side;
	
	MarketCreateTransactionPayload(String parityOrderId, long quantity, long price, long matchNumber, SideType side)	{
		this.parityOrderId = parityOrderId;
		this.quantity = quantity;
		this.price = price;
		this.matchNumber = matchNumber;
		this.side = side;
		// Side is implicit in parityOrderId with a Map lookup
		// TODO consider whether to do the reverse map and get side here
	}
}
