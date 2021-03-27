package com.paritytrading.parity.sbe;

import org.agrona.concurrent.UnsafeBuffer;

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
    	
        return messageHeaderEncoder.ENCODED_LENGTH + marketCreateTransactionPayloadEncoder.encodedLength();

    }

	public static void decode( MarketCreateTenderPayloadDecoder marketCreateTenderPayloadDecoder,
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

		System.out.println(sb);
	}

}
