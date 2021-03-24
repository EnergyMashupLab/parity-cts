package com.paritytrading.parity.sbe;

import org.agrona.concurrent.UnsafeBuffer;

import baseline.*;


public class SBEEncoderDecoder_Parity {
   
   // private static final MessageHeaderDecoder MESSAGE_HEADER_DECODER = new MessageHeaderDecoder();
    //private static final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    //private static final  MarketCreatedTenderPayloadDecoder Market_Created_Tender_Payload_Decoder = new MarketCreatedTenderPayloadDecoder();
    //private static final MarketCreatedTenderPayloadEncoder Market_Created_Tender_Payload_ENCODER = new MarketCreatedTenderPayloadEncoder();
    //private static final  MarketCreateTenderPayloadDecoder Market_Create_Tender_Payload_Decoder = new MarketCreateTenderPayloadDecoder();
    //private static final MarketCreateTenderPayloadEncoder marketCreateTenderPayloadEncoder = new MarketCreateTenderPayloadEncoder();

   
    public static int encode(
            final MarketCreateTenderPayloadEncoder marketCreateTenderPayloadEncoder, final UnsafeBuffer directBuffer, final MessageHeaderEncoder messageHeaderEncoder)
    {
        marketCreateTenderPayloadEncoder.wrapAndApplyHeader(directBuffer, 0, messageHeaderEncoder)
                .quantity(99)
                .price(50)
                .ctsTenderId(111)
                .side(SideType.S);
        marketCreateTenderPayloadEncoder.expireTime()
                .length(5)
                .varDataMaxValue();
        marketCreateTenderPayloadEncoder.bridgeInterval()
                .durationInMinutes(30)
                .length(5)
                .varDataMaxValue();
        return messageHeaderEncoder.ENCODED_LENGTH + marketCreateTenderPayloadEncoder.encodedLength();

    }

    public static void decode(final MarketCreateTenderPayloadDecoder marketCreateTenderPayloadDecoder, final UnsafeBuffer directBuffer, final int bufferOffset, final int actingBlockLength, final int actingVersion) throws Exception{
        //final byte[] buffer = new byte[128];
        final StringBuilder sb = new StringBuilder();
        marketCreateTenderPayloadDecoder.wrap(directBuffer, 0, actingBlockLength, actingVersion);
        sb.append("\nmarketCreateTenderPayload.info=").append(marketCreateTenderPayloadDecoder.info());
        sb.append("\nmarketCreateTenderPayload.quantity=").append(marketCreateTenderPayloadDecoder.quantity());
        sb.append("\nmarketCreateTenderPayload.price=").append(marketCreateTenderPayloadDecoder.price());
        sb.append("\nmarketCreateTenderPayload.ctsTenderId=").append(marketCreateTenderPayloadDecoder.ctsTenderId());
        sb.append("\nmarketCreateTenderPayload.encodedLength=").append(marketCreateTenderPayloadDecoder.encodedLength());
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
