package com.paritytrading.parity.sbe;

import org.agrona.concurrent.UnsafeBuffer;

import com.paritytrading.parity.client.BridgeInstant;
import com.paritytrading.parity.client.BridgeInterval;
import com.paritytrading.parity.client.CtsInterval;
import com.paritytrading.parity.client.MarketCreateTenderPayload;
import com.paritytrading.parity.client.MarketCreateTransactionPayload;

import baseline.*;

public class SBEEncoderDecoder_Parity {

	public static int encode(
            	 MarketCreateTransactionPayloadEncoder marketCreateTransactionPayloadEncoder,
            	 UnsafeBuffer directBuffer, 
            	 MessageHeaderEncoder messageHeaderEncoder, MarketCreateTransactionPayload marketcreateTransactionPayload)
    {
		com.paritytrading.parity.client.SideType  sideT = marketcreateTransactionPayload.getSide();
		
    	marketCreateTransactionPayloadEncoder.wrapAndApplyHeader(directBuffer, 0, messageHeaderEncoder)
                .quantity(marketcreateTransactionPayload.getQuantity())
                .price(marketcreateTransactionPayload.getPrice())
                .ctsTenderId(marketcreateTransactionPayload.getCtsTenderId())
                .side((sideT.name()=="BUY") ? SideType.B : SideType.S)
                .matchNumber(marketcreateTransactionPayload.getMatchNumber())
                .parityOrderId();
    	System.out.println("");
    	System.out.println("MarketCreateTransactionPayload Encoded  :-");
    	System.out.println(marketCreateTransactionPayloadEncoder.toString());
    	
        return messageHeaderEncoder.ENCODED_LENGTH + marketCreateTransactionPayloadEncoder.encodedLength();
        

    }

	public static MarketCreateTenderPayload decode( MarketCreateTenderPayloadDecoder marketCreateTenderPayloadDecoder,
			 UnsafeBuffer directBuffer,  int bufferOffset,  int actingBlockLength,
			 int actingVersion) throws Exception {
		
		final StringBuilder sb = new StringBuilder();
		marketCreateTenderPayloadDecoder.wrap(directBuffer, 0, actingBlockLength, actingVersion);
		sb.append("\nmarketCreateTenderPayload.info=").append(marketCreateTenderPayloadDecoder.info());
		sb.append("\nmarketCreateTenderPayload.quantity=").append(marketCreateTenderPayloadDecoder.quantity());
		sb.append("\nmarketCreateTenderPayload.price=").append(marketCreateTenderPayloadDecoder.price());
		sb.append("\nmarketCreateTenderPayload.ctsTenderId=").append(marketCreateTenderPayloadDecoder.ctsTenderId());
		sb.append("\nmarketCreateTenderPayload.encodedLength=")
				.append(marketCreateTenderPayloadDecoder.encodedLength());
		final BridgeInstantDecoder ep = marketCreateTenderPayloadDecoder.expireTime();
		sb.append("\nmarketCreateTenderPayload.expireTime.length=").append(ep.length());
		sb.append("\nmarketCreateTenderPayload.expireTime.varData=").append(ep.varDataMaxValue());
		final BridgeIntervalDecoder bid = marketCreateTenderPayloadDecoder.bridgeInterval();
		sb.append("\nmarketCreateTenderPayload.bridgeInterval.durationInMinutes=").append(bid.durationInMinutes());
		sb.append("\nmarketCreateTenderPayload.bridgeInterval.length=").append(bid.length());
		sb.append("\nmarketCreateTenderPayload.bridgeInterval.varData=").append(bid.varDataMaxValue());

		System.out.println("");
		System.out.println("-------------------------------------------------------------------------");
		System.out.println("MarketCreateTenderPayload Decoded :-");
		System.out.println(marketCreateTenderPayloadDecoder.toString());
		
		MarketCreateTenderPayload marketCreateTenderPayload = new MarketCreateTenderPayload();
		BridgeInstant bridgeInstant = new BridgeInstant();
		BridgeInterval bridgeInterval = new BridgeInterval();
		//CtsInterval CtsInterval = new CtsInterval();
		
		
		marketCreateTenderPayload.setQuantity(marketCreateTenderPayloadDecoder.quantity());
		marketCreateTenderPayload.setPrice(marketCreateTenderPayloadDecoder.price());
		marketCreateTenderPayload.setCtsTenderId(marketCreateTenderPayloadDecoder.ctsTenderId());
		
		if(marketCreateTenderPayloadDecoder.side() == SideType.B) {
			marketCreateTenderPayload.setSide(com.paritytrading.parity.client.SideType.BUY);
		}else {
			marketCreateTenderPayload.setSide(com.paritytrading.parity.client.SideType.SELL);
		}
		
		
		
		//bridgeInstant.setInstantString("2007-06-20T00:00:30.00Z");
		bridgeInstant.setInstantString("2021-06-20T16:00:00Z");
		
		bridgeInterval.setDurationInMinutes(bid.durationInMinutes());
		bridgeInterval.setDtStart(bridgeInstant);
		
		marketCreateTenderPayload.setExpireTime(bridgeInstant);
		marketCreateTenderPayload.setBridgeInterval(bridgeInterval);
		
		
		return marketCreateTenderPayload;
	}

}
