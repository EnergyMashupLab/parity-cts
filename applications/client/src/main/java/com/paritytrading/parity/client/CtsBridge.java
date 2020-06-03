package com.paritytrading.parity.client;

import com.paritytrading.parity.client.TerminalClient.*;
import com.paritytrading.parity.client.EnterCommand.*;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.parity.net.poe.POE;
import com.paritytrading.parity.util.Instrument;
import com.paritytrading.parity.util.Instruments;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import java.util.Random;

import java.time.Duration;
import java.time.Instant;
import java.time.*;
import com.fasterxml.jackson.datatype.jsr310.*;

import java.util.concurrent.atomic.AtomicLong;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//	import org.apache.logging.log4j.LogManager;
//	import org.apache.logging.log4j.Logger;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.AbstractQueue;
import java.util.AbstractCollection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

/*
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
* MUST be consistent across this enhanced Terminal Client. First implementation
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

	public CtsBridge() {
	}
	
	/*
	 * Global constants in CTS and for CtsBridge
	 */
	public final static int LME_PORT = 39401;		// Socket Server in LME takes CreateTransaction
	public final static int MARKET_PORT = 39402;	// Socket Server in Market takes CreateTender 
	
	
	public CtsBridge(TerminalClient client, Events events, Instruments instruments)	{
		// store information needed to call EnterCommand.bridgeExecute() to enter orders
		this.client = client;
		this.events = events;
		this.instruments = instruments;
	}

	// set by TerminalClient call to this.setSide()
	static private EnterCommand buySide, sellSide; 	
	static private Instruments instruments;
	static private TerminalClient client;
	
	static final Boolean DEBUG_JSON = false;

	/*
	 * CtsSocketServer receives tenders from the LME and puts on createTenderQ
	 * CtsSocketClient takes transactions from createTransactionQ and sends to the LME
	 * 
	 * Both are Threads for accessing their respective BlockingQueues and their sockets
	 * 
	 * CtsBridge is a Thread; all queue operations are fast so a single thread suffices
	 */
	static CtsSocketServer ctsSocketServer;	// constructed and started later
	// Queue entries are from LME via socket writes. 
    public ArrayBlockingQueue<MarketCreateTenderPayload> createTenderQ = new ArrayBlockingQueue<>(200);
    MarketCreateTenderPayload createTender;
    String tempParityOrderId = null;
    
	// CtsSocketClient sends MarketCreateTransactionPayloads from its queue to the LME
    static CtsSocketClient ctsSocketClient;	// constructed and started later
    public static ArrayBlockingQueue<MarketCreateTransactionPayload> createTransactionQ = new ArrayBlockingQueue<>(200);
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
	private static MarketCreateTenderPayload[] createTenderPayload = 
					new MarketCreateTenderPayload[10];
	private static String[] json = new String[10];
	private static MarketCreateTenderPayload[] deserializedTenderPayload = 
					new MarketCreateTenderPayload[10];	
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
		System.err.println("CtsBridge.run " + Thread.currentThread().getName());
		/*
		 *		DEBUG Create and Inject 10 random tenders.
		 */
//		initTenders();
//		sendTenders();

		/*  
		 *  This CtsBridge thread reads Tenders from createTenderQ (received from LME) and inject to market
		 *  
		 *  Another thread from parity-client calls CtsBridge.orderExecuted and inserts transactions
		 *  in createTransactionQ (to be taken off and sent to LME by CtsSocketClient)
		 *  
		 *  CtsBridge processing of CreateTenderPayload consists of a call to bridgeExecute to put
		 *  Tender on NIO queue in Parity Client -- fast and nonblocking so no need for separate thread    
		 */

		if (DEBUG_JSON) System.err.println("CtsBridge run before new SocketServer: currentThread name: '" +
						Thread.currentThread().getName() + "'");
		
		ctsSocketServer = new CtsSocketServer(MARKET_PORT, this);	// thread enters MarketCreateTenderPayloads
		ctsSocketServer.start();
		
		ctsSocketClient = new CtsSocketClient(LME_PORT, this);	// thread sends MarketCreateTransactionPayloads
	 	ctsSocketClient.start();
		
		System.err.println("Started SocketServer and SocketClient");
		
		while (true) {
			//	CtsSocketServer fills createTenderQ	from LME
			//	Take first and send as order entry to Parity
			
			try {
				// DEBUG before and after blocking operation
//				System.err.println("CtsBridge.run before createTenderQ.take " + Thread.currentThread().getName());
				createTender = createTenderQ.take();	// blocking
//				System.err.println("CtsBridge.run after createTenderQ.take " + Thread.currentThread().getName());
			} catch (InterruptedException e1) {
				System.err.println("Interrupted while waiting on createTenderQ");
				e1.printStackTrace();
			}
			
			// process the just-removed MarketCreateTenderPayload
			if (createTender == null)	{
				System.err.println("CtsBridge:run: removed MarketCreateTenderPayload is null");
				continue;
			}	else	{
				System.err.println("CtsBridge.run: side " + createTender.toString());
			}
			
			if ( createTender.getSide() == SideType.BUY)	{	// BUY
				 try	{
					tempParityOrderId = buySide.bridgeExecute
					 	(getClient(), createTender.getQuantity(), 
						4702127773838221344L , createTender.getPrice());
					// DEBUG using AAPL packed long for now
					
					//	save Parity OrderID String and createTender in map for use in orderExecuted
					mapReturnValue = idToCreateTenderMap.put(tempParityOrderId, createTender);
					if (mapReturnValue != null)
						System.err.println("CtsBridge.run.BuySide: Entry in map " +
							mapReturnValue.toString() + " tempParityOrderId " + tempParityOrderId);
					
					// DEBUG 
//					printIdMap();
				 } catch (IOException e) {
					 System.out.println("error: CtsBridge: Connection closed");
				 }	
			} else	{	// SELL
				try	{
					// TODO will take map output <Interval, String>
					tempParityOrderId = sellSide.bridgeExecute
					 	(getClient(), createTender.getQuantity(), 
						4702127773838221344L , createTender.getPrice());
					
					//	save Parity OrderID String and createTender in map for use in orderExecuted
					mapReturnValue = idToCreateTenderMap.put(tempParityOrderId, createTender);
					if (mapReturnValue != null)
						System.err.println("CtsBridge.run.SellSide: Return from put in map " +
							mapReturnValue.toString());
					
					// DEBUG 
//					printIdMap();

				} catch (IOException e) {
					 System.out.println("error: CtsBridge: Connection closed"); 
				}
					
			// iterate
			
				}
		}
	}
	

	/*
	 * Methods to process POE protocol events - the type of parameter message vary
	 * 
	 * orderId is String message.orderId for all of the POE message types
	 * 
	 * Order and other outgoing protocol message are sent by calls to EnterCommand
	 * buySide and sellSide bridgeExecute()
	 */
	static void orderAccepted(POE.OrderAccepted message)	{
		// process the orderAccepted - TODO
	}
	
	static void orderRejected(POE.OrderRejected message)	{
		//process the orderRejected - TODO
	}
	
	/*
	 * The OrderExecuted POE message does not have side; correlate by orderId
	 */
	/*
	 * process orderExecuted POE message
	 * 
	 * Generate MarketCreateTransactionPayload and send to LME.
	 * Notes:
	 * 		Side is implicit and can be determined from the OrderId
	 * 		use map to determine side of the corresponding EiTender from map
	 * 
	 * 	NOTE: The OrderExecuted POE message does not have side; correlate by orderId
	 * 	TODO use map to determine side of the corresponding EiTender from map
	 */
	static void orderExecuted(POE.OrderExecuted message, String parityOrderId) {
		MarketCreateTenderPayload originalCreateTender;
		MarketCreateTransactionPayload marketCreateTransaction = null;
		
		originalCreateTender = idToCreateTenderMap.get(parityOrderId);
//		System.err.println("CtsBridge.orderExecuted: " + parityOrderId + " " + Thread.currentThread().getName());
		
		// Only CTS tenders will have an ID map entry. Process, or print and return.
		if (originalCreateTender == null) {
			System.err.println("CtsBridge.orderExecuted: parityOrderId "  + 
					parityOrderId + " originalCreateTender is null.");
			}	else	{ 	// has a valid map entry
				System.err.println("CtsBridge.orderExecuted " +  parityOrderId + " " +
						" originalCreateTender " + originalCreateTender.toString());
				
				marketCreateTransaction = new MarketCreateTransactionPayload(
						parityOrderId, 
						originalCreateTender.getCtsTenderId(),
						message.quantity,	// likely differs from originalCreateTender
						message.price, 		// likely differs from originalCreateTender
						message.matchNumber, 
						originalCreateTender.getSide());
				
				//	And put MarketCreateTransactionPayload on createTransactionQ for CtsSocketServer
				//	to send to LME
				try {
					createTransactionQ.put(marketCreateTransaction);
					System.err.println("CtsBridge.orderExecuted: createTransactionQ.put size now " + createTransactionQ.size());
				} catch (InterruptedException e) {
					System.err.println("CtsBridge.orderExecuted: createTransactionQ.put interrupted");
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

	static void orderCanceled(POE.OrderCanceled message, String s) {
		// process orderCanceled - FUTURE
		// Generate MarketCanceledTenderPayload and send to LME
		// TODO
		System.err.println("In CtsBridge - message orderCanceled " + s);
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
		this.client = client;
	}
	
	public EnterCommand getBuySide() {
		return buySide;
	}
	
	public void setBuySide(EnterCommand buyCmd) {
		this.buySide = buyCmd;
	}
	
	public EnterCommand getSellSide() {
		return sellSide;
	}
	
	public void setSellSide(EnterCommand sellCmd) {
		this.sellSide = sellCmd;
	}
	
	public Instruments getInstruments() {
		return instruments;
	}
	
	public void setInstruments(Instruments instruments) {
		this.instruments = instruments;
	}
	
	/*
	 * Methods for calling CtsBridge from the network interface go here
	 * 
	 */
	
	void initTenders()	{
		int i;
		
		// CTS price * 10 **2 decimal places 119 == Parity price 11900
		// minimum increment price is 1 cent
		
		
		long longInstrument = 4702127773838221344L; // AAPL instrument
		long randQuantity = 10;	// will be random quantity from 20 to 100
		long randPrice = 60;	// will be random price in dollars from 75 to 125
		final long constantCtsTenderId = 10000L;
		Instant dtStart;	// start of interval
		CtsInterval ctsInterval;
		Instant expireTime;
		final ObjectMapper mapper = new ObjectMapper();
		
		//	First try to fix bugs with Jackson java.time
		//	mapper.findAndRegisterModules(); 
		// mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); done in application.properties
		
		SideType tempSide;


		String side;
		String buySide = new String("B");	// flag to send order to the right place
		String sellSide = new String("S");	// flag to send order to the right place
		
		dtStart = Instant.parse("2020-05-20T10:00:00.00Z");
		// and create our 60 minute interval
		ctsInterval = new CtsInterval(60, dtStart);
		expireTime = dtStart.plusSeconds(60*60*11);	// DEBUG eleven hours after start
		
		if (DEBUG_JSON) System.err.println("after expireTime set expireTime " + expireTime.toString() + "dtStart " + dtStart.toString());
		
		//	initialize array (with non-fixed fields) for random test tenders
		// 	build parallel arrays of incoming MarketCreateTenderPayloads,
		//	JSON serialization, and deserialized Payloads
		for (i = 0; i < 10; i++)	{
			randQuantity = 20 + rand.nextInt(80);
			randPrice = 75 + rand.nextInt(50);			
			quantity[i] = randQuantity;
			price[i] = randPrice * 100; // two decimal place
			
			//	ctsTenderId passed in use constant here
			//	parallel array, with JSON serialization in parallel array json[10]
			
			if ( i%2 == 0)	{
				// even number - sell
				tempSide = SideType.SELL;
			} else	{
				// odd number - buy
				tempSide = SideType.BUY;
				}
			
			// MarketCreateTenderPayload(SideType side, long quantity, long price, long ctsTenderId, CtsInterval interval, Instant expireTime)
			createTenderPayload[i] = new MarketCreateTenderPayload(tempSide,
					quantity[i],
					price[i],
					constantCtsTenderId + 1 +i,
					ctsInterval,
					dtStart.plusSeconds(60*60*11));
			try	{
				if (DEBUG_JSON) System.err.println("try block line 252 before serialization/deserialization. i = " + i);
				json[i] = mapper.writeValueAsString(createTenderPayload[i]);
				
				deserializedTenderPayload[i] = mapper.readValue(json[i], MarketCreateTenderPayload.class);
				
				if (DEBUG_JSON) System.err.println("CtsBridge: Compare original and roundtrip MarketCreateTenderPayload equals = " 
						+ createTenderPayload[i].equals(deserializedTenderPayload[i]));	// dropped expirationTime in compare
			}	catch (JsonProcessingException e)	{
				System.err.println("CtsBridge:initTenders: JsonProcessingException " + e);
				
				}	
			// and print the json
			if (DEBUG_JSON) System.err.println("***" + json[i] + "***");
			}
		// expireTime = dtStart.plusSeconds(60*60*11);
		

			// DEBUG print the headers
			if (DEBUG_JSON)System.out.println("initTenders: initialized array");
			System.out.println(" #  Quantity   Price");
			System.out.println("___ ________   ______");
	
			// DEBUG and the orders to be entered
			for (i = 0; i < 10; i++)	{
				if ( i%2 == 0)	{
					// even number - sell
					side = sellSide;
				} else	{
					// odd number - buy
					side = buySide;
					}
				System.out.println((i < 9 ? "  " : " ") + (1+i) + "   " + side + " " + quantity[i] + "       " + (price[i]/100));
			}
		
	}
	
	
	void sendTenders()	{
		MarketCreateTenderPayload mapReturnValue = null;
		// CTS price * 10 **2 decimal places 119 == Parity price 11900
		int i;
		long longInstrument = 4702127773838221344L; // AAPL instrument
			
		for (i= 0; i < 10; i++)	{
			//choose buy or sell side by modular arithmetic
			if ( i%2 == 0)	{
				// even number - sellB
				 try	{
					 orderIds[i] = sellSide.bridgeExecute
							 (getClient(), quantity[i],
							 longInstrument,price[i]);
					 
					 //	add to HashMap
					 mapReturnValue = idToCreateTenderMap.put(orderIds[i], createTenderPayload[i]);

					 if (mapReturnValue == null)	{
						 System.err.println("CtsBridge.sendTenders.SellSide: " + i + " Return from put in map NULL");
						 
					 }	else	{
						 System.err.println("CtsBridge.sendTenders.SellSide: " + i + " Return from put in map " +
									mapReturnValue.toString());
					 }
					 
					 //	DEBUG print
					 //	printIdMap();
				 } catch (IOException e) {
					 System.out.println("error: CtsBridge: Connection closed");
				 }	
			} else	{
				// odd number - buy
				try	{
					orderIds[i] = buySide.bridgeExecute(
								client, quantity[i],
								longInstrument, price[i]);
					
					 //	add to HashMap
					mapReturnValue = idToCreateTenderMap.put(orderIds[i], createTenderPayload[i]);

					if (mapReturnValue == null)	{
						 System.err.println("CtsBridge.sendTenders.BuySide: " + i + " Return from put in map NULL");
					}	else	{
						System.err.println("CtsBridge.sendTenders.BuySide: " + i + " Return from put in map " +
								mapReturnValue.toString());
					}
				 
					 //	DEBUG print
					 //	printIdMap();		
				} catch (IOException e) {
					 System.out.println("error: CtsBridge: Connection closed");
				}
			}
			System.out.println();
		}
	}
	
	/*
	 * 	Called from POST handler for MarketCreateTenderPayload
	 */
	static void enterOrder(SideType side, long quantity, long instrument, long price)	{
		String parityOrderId;
		
		if ( side == SideType.BUY)	{	//	Buy
			 try	{
				 parityOrderId = sellSide.bridgeExecute(
						 getClient(), quantity, instrument,price);
			 } catch (IOException e) {
				 System.out.println("error: CtsBridge: Connection closed");
			 }	
		} else	{	// Sell
			try	{
				parityOrderId = buySide.bridgeExecute(
						getClient(), quantity, instrument, price);
			} catch (IOException e) {
				 System.out.println("error: CtsBridge: Connection closed");
			}
		}
	}
	
	public static void printIdMap() {
		// prints content of idToCreateTenderMap
		Iterator it = idToCreateTenderMap.entrySet().iterator();
		
		System.err.println("CtsBridge.printIdMap ID MAP**********START");
		while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue().toString());
	    }
		System.err.println("CtsBridge.printIdMap ID MAP**********END");
	}
	
}
