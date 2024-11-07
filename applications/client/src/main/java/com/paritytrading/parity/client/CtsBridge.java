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

import com.paritytrading.parity.net.poe.POE;
import com.paritytrading.parity.util.Instrument;
import com.paritytrading.parity.util.Instruments;
import java.io.IOException;

import java.util.Random;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BrokenBarrierException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 *	Comments that start with "CTS " refer to code changes for CTS integration
 */

/*
 * CTS
 *
 * CtsBridge is used to transform and send and receive messages between the LME and the 
 * enhanced Parity Terminal Client, thus entering Parity orders and detecting Parity
 * order execution.
 * 
 * CtsBridge functions include
 * 
 *		Creating the appropriate parity and CTS messages
 * 
 * 		mapping from the Parity orderExecuted to build and
 * 		send MarketCreateTransaction with the correct information.
 * 
 * A MarketCreateTender message is received from the LME and turned into a 
 * Parity Order Entry with necessary mapping of tender/order IDs
 * 
 * All prices and quantities are of the minimum increment; the minimum increment
 * MUST be consistent across this CTS  enhanced Terminal Client. First implementation
 * will use minimum increment price of one-tenth of a cent, a factor of 1000, and
 * integers for quantity.
 *
 * Design note: This class is implemented with multiple threads including those in CtsBridge,
 * CtsSocketClient, and CtsSocketServer.
 * 
 * The CtsBridge and related codes hooks the Parity Client so the user can run parity-client.jar,
 * where the parity ticker, reporter, manual entry, and trades and orders all function as in
 * the unmodified Parity release.
 */


// started in TerminalClient
class CtsBridge extends Thread {	

	private static final Logger logger = LogManager.getLogger(CtsBridge.class);

	public CtsBridge() {
	}
	
	/*
	 * Global constants in CTS and for CtsBridge
	 */
	public final static int LME_PORT = 39401;		// Socket Server in LME takes CreateTransaction
	public final static int MARKET_PORT = 39402;	// Socket Server in Market takes CreateTender 
	
	
	public CtsBridge(TerminalClient client, Events events, Instruments instruments)	{
		// store information needed to call EnterCommand.bridgeExecute() to enter orders
		this.events = events;
		CtsBridge.client = client;
		CtsBridge.instruments = instruments;
	}

	// set by TerminalClient call to this.setSide()
	static private EnterCommand buySide, sellSide; 	
	static private Instruments instruments;
	static private TerminalClient client;
	
	static final Boolean DEBUG_JSON = false;

	/*
	 * CTS
	 * CtsSocketServer receives tenders from the LME and puts on marketCreateTenderQueue
	 * CtsSocketClient takes transactions from createTransactionQueue and sends to the LME
	 * 
	 * Both are Threads for accessing their respective BlockingQueues and their sockets
	 * 
	 * CtsBridge is a Thread; all queue operations are fast so a single thread suffices
	 */
	static CtsSocketServer ctsSocketServer;	// constructed and started later
	// Queue entries are from LME via socket writes. 
    public ArrayBlockingQueue<MarketCreateTenderPayload> marketCreateTenderQueue = new ArrayBlockingQueue<>(200);
    MarketCreateTenderPayload marketCreateTender;
    String tempParityOrderId = null;
    
	// CtsSocketClient sends MarketCreateTransactionPayloads from its queue to the LME
	static CtsSocketClient ctsSocketClient;	// constructed and started later
	final private CyclicBarrier clientSocketBarrier = new CyclicBarrier(2);
    public static ArrayBlockingQueue<MarketCreateTransactionPayload> createTransactionQueue = new ArrayBlockingQueue<>(200);
    MarketCreateTransactionPayload createTransaction;
	
	/*
	 * Events can be used for detecting order entered/cancelled, updates from trades.
	 * This implementation hooks the message entry into the client and calls out to
	 * CtsBridge methods for possible processing. See Parity Order Entry Protocol definition
	 */
	Events events;	// local copy if used for evolution
	
	/*
	 * HashMap to correlate Parity Order IDs (String) and MarketCreatetenderPayload which created the order
	 */
	public static ConcurrentHashMap<String, MarketCreateTenderPayload> idToCreateTenderMap = new ConcurrentHashMap<String, MarketCreateTenderPayload>();

	/*
	 * arrays for volatile parts of an order and debug in initTenders and sendTenders
	 */
	private Instrument localInstrument;
	private static long[] quantity = new long[10];
	private static long[] price = new long[10];
	private static String[] orderIds = new String[10];
	
	// for randomized quantity and price
	final static Random rand = new Random();	
	
	/*
	 *	TODO	DEFINE map<long, String> to correlate CTS long TenderId
	 *		and Parity OrderId.
	 *
	 * 	Parity OrderId is returned from Order Entry
	 */


	@Override
	public void run()	{	// Thread for processing requests to and from LME
		MarketCreateTenderPayload mapReturnValue = null;
		long instrument = 4702127773838221344L;		//	Default - AAPL initial functionality tests
		
		logger.debug("CtsBridge.run " + Thread.currentThread().getName());

		/*  
		 *  This CtsBridge thread reads Tenders from marketCreateTenderQueue (received from LME) and inject to market
		 *  
		 *  Another thread from parity-client calls CtsBridge.orderExecuted and inserts transactions
		 *  in createTransactionQueue (to be taken off and sent to LME by CtsSocketClient)
		 *  
		 *  CtsBridge processing of CreateTenderPayload consists of a call to bridgeExecute to put
		 *  Tender on NIO queue in Parity Client -- fast and nonblocking so no need for separate thread    
		 */

		if (DEBUG_JSON) {
			logger.debug("CtsBridge run before new SocketServer: currentThread name: '" + Thread.currentThread().getName() + "'");
		}
		
		ctsSocketServer = new CtsSocketServer(MARKET_PORT, this, clientSocketBarrier); // thread enters MarketCreateTenderPayloads
		ctsSocketServer.start();
		
		// Wait for ctsSocketServer gets a client then start the client connection to the LME
		try {
			clientSocketBarrier.await();
		} catch(BrokenBarrierException e) {
			logger.debug("The client socket barrier was broken.");
		} catch(InterruptedException e) {
			logger.debug("CtsBridge thread was interrupted before socket client start.");
		}

		ctsSocketClient = new CtsSocketClient(LME_PORT, this);	// thread sends MarketCreateTransactionPayloads
	 	ctsSocketClient.start();
		
		logger.debug("Started SocketServer and SocketClient");
		
		while (true) {
			//	CtsSocketServer fills marketCreateTenderQueue	from LME
			//	Take first and send as order entxry to Parity
			
			try {
				marketCreateTender = marketCreateTenderQueue.take();	// blocking
				logger.debug("CtsBridge.run after marketCreateTenderQueue.take " + Thread.currentThread().getName());
			} catch (InterruptedException e1) {
				logger.debug("Interrupted while waiting on marketCreateTenderQueue");
				e1.printStackTrace();
			}
			
			// process the just-removed MarketCreateTenderPayload
			if (marketCreateTender == null)	{
				logger.debug("CtsBridge:run: removed MarketCreateTenderPayload is null");
				continue;
			}	else	{
				logger.debug("CtsBridge.run: " + marketCreateTender.toString());
			}
			
			//	Convert dtStart in marketCreateTender.bridgeInterval to instrument PackedLong
			
			//	TODO Conversion in BridgeInterval should also manage duration;
			//	this is the default 60 minute duration
			instrument = marketCreateTender.getBridgeInterval().toPackedLong();
			
			logger.debug("CtsBridge.run() before bridgeExecute. instrument " +
					instrument + " Instrument Name " +
					marketCreateTender.getBridgeInterval().toInstrumentName());
			
			if ( marketCreateTender.getSide() == SideType.BUY)	{	//	buySide
				 try	{
					tempParityOrderId = buySide.bridgeExecute(
						getClient(),
					 	marketCreateTender.getQuantity(), 
						instrument,
						marketCreateTender.getPrice());
					// DEBUG using AAPL packed long for now
					
					//	save Parity OrderID String and marketCreateTender in map for use in orderExecuted
					mapReturnValue = idToCreateTenderMap.put(tempParityOrderId, marketCreateTender);
					if (mapReturnValue != null)
						logger.debug("CtsBridge.run.BuySide: Entry in map " + mapReturnValue.toString() +
							" tempParityOrderId " + tempParityOrderId);
					
					// DEBUG 
//					printIdMap();
				 } catch (IOException e) {
					 logger.debug("error: CtsBridge: Connection closed");
				 }	
			} else	{	// sellSide
				try	{
					// TODO will take map output <Interval, String>
					tempParityOrderId = sellSide.bridgeExecute(
					 	getClient(),
					 	marketCreateTender.getQuantity(), 
					 	instrument,
					 	marketCreateTender.getPrice());
					
					//	save Parity OrderID String and marketCreateTender in map for use in orderExecuted
					mapReturnValue = idToCreateTenderMap.put(tempParityOrderId, marketCreateTender);
					if (mapReturnValue != null)
						logger.debug("CtsBridge.run.SellSide: Return from put in map " +
							mapReturnValue.toString());
					
					// DEBUG 
//					printIdMap();

				} catch (IOException e) {
					 logger.debug("error: CtsBridge: Connection closed");
				}
					
			// iterate
			
				}
		}
	}
	

	/*
	 * POE protocol events - the type of parameter message varies
	 * 
	 * orderId is String message.orderId for all of the POE message types
	 * 
	 * Order and other outgoing protocol message are sent by calls to EnterCommand
	 * buySide and sellSide bridgeExecute()
	 * 
	 * The POE outbound messages are
	 * 		orderAccepted
	 * 		orderCanceled
	 * 		orderRejected
	 * 		orderExecuted
	 */
	
	static void orderAccepted(POE.OrderAccepted message, String parityOrderId)	{
		// process the orderAccepted POE message
		logger.debug("CtsBridge.orderAccepted " + parityOrderId);
		
		//	TODO message back to CTS
	}
	
	static void orderCanceled(POE.OrderCanceled message, String parityOrderId)	{
		// process the orderCanceled POE message
		String reasonString = null;

		switch(message.reason)	{
		case 'R':	reasonString = "Request";
					break;
		case 'S':	reasonString = "Supervisory";
					break;
		}

		logger.info("CtsBridge.orderCanceled " + parityOrderId +
				" Canceled Quantity " + message.canceledQuantity +
				" Reason " + reasonString);

		//	TODO message back to CTS
	}

	
	static void orderRejected(POE.OrderRejected message, String parityOrderId)	{
		// process the orderRejected POE message
		byte reason;
		String reasonString = null;
		reason = message.reason;

		switch(reason)	{
		case 'I':	reasonString = "Unknown Instrument";
					break;
		case 'P':	reasonString = "Invalid Price";
					break;
		case 'Q':	reasonString = "Invalid Quantity";
					break;
		}
		logger.info("CtsBridge.orderRejected " + parityOrderId +
				" Reason " + reasonString);
		
		//	TODO message back to CTS
	}

	/*
	 *	process orderExecuted POE message
	 * 
	 *	Generate MarketCreateTransactionPayload and send to LME.
	 *	
	 *	Notes:
	 * 		Side can be determined from the OrderId using idToCreateTenderMap
	 *  	to determine side of the corresponding EiTender
	 */
	static void orderExecuted(POE.OrderExecuted message, String parityOrderId) {
		MarketCreateTenderPayload originalCreateTender;
		MarketCreateTransactionPayload marketCreateTransaction = null;		
		//	Get original MarketCreateTenderPayload which has Side
		originalCreateTender = idToCreateTenderMap.get(parityOrderId);

		logger.debug("CtsBridge.orderExecuted: " + parityOrderId +
				" " + Thread.currentThread().getName());
		
		// Only CTS tenders will have an ID map entry. Process, or print and return.
		if (originalCreateTender == null) {
			logger.debug("CtsBridge.orderExecuted: parityOrderId "  + 
					parityOrderId + " originalCreateTender is null.");
			}	else	{ 	// has a valid map entry
				logger.debug("CtsBridge.orderExecuted " +  parityOrderId +
					" original CreateTender CtsTenderId " + originalCreateTender.getCtsTenderId());
				
				marketCreateTransaction = new MarketCreateTransactionPayload(
						parityOrderId, 
						originalCreateTender.getCtsTenderId(),
						message.quantity,	// likely differs from originalCreateTender
						message.price, 		// likely differs from originalCreateTender
						message.matchNumber, 
						originalCreateTender.getSide());
				
				//	And put MarketCreateTransactionPayload on createTransactionQueue for CtsSocketServer
				//	to send to LME
				try {
					createTransactionQueue.put(marketCreateTransaction);
					
					logger.debug("CtsBridge.orderExecuted: createTransactionQueue.put size now " + createTransactionQueue.size());
				} catch (InterruptedException e) {
					logger.debug("CtsBridge.orderExecuted: createTransactionQueue.put interrupted");
					e.printStackTrace();
				}
			}
	}
	
	public Instrument getLocalInstrument() {
		return localInstrument;
	}

	public void setLocalInstrument(Instrument localInstrument) {
		this.localInstrument = localInstrument;
	}

	public static long[] getQuantity() {
		return quantity;
	}

	public static void setQuantity(long[] quantity) {
		CtsBridge.quantity = quantity;
	}

	public static long[] getPrice() {
		return price;
	}

	public static void setPrice(long[] price) {
		CtsBridge.price = price;
	}

	public static String[] getOrderIds() {
		return orderIds;
	}

	public static void setOrderIds(String[] orderIds) {
		CtsBridge.orderIds = orderIds;
	}
	
	/*
	 * 	setClient - for TerminalClient to set for buy.bridgeExecute
	 * 	setInstruments - for TerminalClient to set for iteration/lookup
	 * 	setBuySide and setSellSide - for EnterCommand instances to 
	 * 			set buySide and sellSide
	 */
	public static TerminalClient getClient() {
		return client;
	}
	
	public void setClient(TerminalClient client) {
		CtsBridge.client = client;
	}
	
	public EnterCommand getBuySide() {
		return buySide;
	}
	
	public void setBuySide(EnterCommand buyCmd) {
		CtsBridge.buySide = buyCmd;
	}
	
	public EnterCommand getSellSide() {
		return sellSide;
	}
	
	public void setSellSide(EnterCommand sellCmd) {
		CtsBridge.sellSide = sellCmd;
	}
	
	public Instruments getInstruments() {
		return instruments;
	}
	
	public void setInstruments(Instruments instruments) {
		CtsBridge.instruments = instruments;
	}

//	/*
//	 * 	Called from POST handler for MarketCreateTenderPayload
//	 */
//	static void enterOrder(SideType side, long quantity, long instrument, long price)	{
//		String parityOrderId;
//		
//		System.err.println("CtsBridge.enterOrder instrument " + instrument);
//		Thread.dumpStack();
//		
//		if ( side == SideType.BUY)	{	//	Buy
//			 try	{
//				 parityOrderId = sellSide.bridgeExecute(
//						 getClient(), quantity, instrument,price);
//			 } catch (IOException e) {
//				 System.out.println("error: CtsBridge: Connection closed");
//			 }	
//		} else	{	// Sell
//			try	{
//				parityOrderId = buySide.bridgeExecute(
//						getClient(), quantity, instrument, price);
//			} catch (IOException e) {
//				 System.out.println("error: CtsBridge: Connection closed");
//			}
//		}
//	}
//	
//	public static void printIdMap() {
//		// prints content of idToCreateTenderMap
//		Iterator it = idToCreateTenderMap.entrySet().iterator();
//		
//		System.err.println("CtsBridge.printIdMap ID MAP**********START");
//		while (it.hasNext()) {
//	        Map.Entry pair = (Map.Entry)it.next();
//	        System.out.println(pair.getKey() + " = " + pair.getValue().toString());
//	    }
//		System.err.println("CtsBridge.printIdMap ID MAP**********END");
//	}
	
}
