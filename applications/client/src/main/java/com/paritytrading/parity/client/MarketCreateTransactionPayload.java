package com.paritytrading.parity.client;

public class MarketCreateTransactionPayload {
	String parityOrderId = null;
	long quantity = 0;	// quantity transacted. Two or more Transactions created for a match
	long price = 0;		//price may not be that tendered
	long matchNumber = 0;	// parity matchNumber for this match
	long ctsTenderId = 0;	// set before sending from Map maintained by CtsBridge
	
	
	MarketCreateTransactionPayload(String parityOrderId, long quantity, long price, long matchNumber )	{
		this.parityOrderId = parityOrderId;
		this.quantity = quantity;
		this.price = price;
		this.matchNumber = matchNumber;
		// Side is implicit in parityOrderId with a Map lookup and
	}
}
