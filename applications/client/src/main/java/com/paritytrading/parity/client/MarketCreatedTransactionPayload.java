package com.paritytrading.parity.client;

/*
 * Sent by LME to Market in response to a MarketCreateTransactionPayload
 */

public class MarketCreatedTransactionPayload {
	protected Boolean success = false;
	protected String info = "MarketCreatedTransactionPayload";
	
	MarketCreatedTransactionPayload()	{
		success = true;
	}
	
	@Override
	public String toString()	{
		return (info + " success is " + success.toString());
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
}