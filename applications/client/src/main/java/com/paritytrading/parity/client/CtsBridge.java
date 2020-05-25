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
* CtsBridge is used to send and receive (via Spring RestContoller interactions)
* messages both ways between the LME and the enhanced Parity Terminal Client.
* 
* Functions including two way mapping between CTS tenderId and Parity orderId are here.
* 
* An EiCreateTender message will be received from the LME and turned into a 
* Parity Order Entry with mapping of tender/order IDs
* 
* All prices and quantities are of the minimum increment; the minimum increment
* MUST be consistent across this enhanced Terminal Client. First implementation
* will use minimum increment price of one-tenth of a cent, a factor of 1000, and
* integers for quantity.
*/

/*
* Design note: This class is planned to run multiple Spring
* RestControllers for POST and PUT operations between CTS and Parity. 
* 
* Terminal client message will go to the terminal window executing the shaded
* jar, so manual entry of orders and detection of trades and of the order books,
* by allowing the Parity Client orders (buy and sell) and the separate ticker and
* (trade) reporter to function as designed.
* 
* The orders and trades
* 
* Future information retrieved from Parity System Event Visitor
* 		status of orders (which includes matches by which orders are fulfilled)
*/


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
	 * The CtsSocketServer receives tenders from the LME.
	 * 
	 * The CtsSocketClient sends transactions to the LME
	 * 
	 * Both are implemented as Threads for accessing their respective BlockingQueues
	 */
	static CtsSocketServer ctsSocketServer;	// constructed and started later
	// Queue entries are from LME via socket writes
    public ArrayBlockingQueue<MarketCreateTenderPayload> createTenderQ = new ArrayBlockingQueue<>(100);
    MarketCreateTenderPayload createTender;
    String tempParityOrderId = null;
    
	// CtsSocketClient sends MarketCreateTransactionPayloads from its queue to the LME
    static CtsSocketClient ctsSocketClient;	// constructed and started later
	
	
	/*
	 * Events may be used for detecting order entered/cancelled, updates from trades.
	 * This implementation hooks the message entry into the client and calls out to
	 * CtsBridge methods.
	 */
	Events events;	// local copy if used for evolution

	// arrays for volatile parts of an order and debug in initTenders and sendTenders
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

/*
 *		DEBUG Create and Inject 10 random tenders.
 */
	@Override
	public void run()	{	// Thread for processing requests to and from LME
		initTenders();
		sendTenders();
		
		// initial example - ends after one response

		/*
		 *  Read from createTenderQ, after paylod processing call
		 *  bridgeExecute to 
		 *  
		 *  thread each to
		 *  	read from createTenderQ (received from LME)
		 *  	write to createTransactionQ (to send to LME)
		 *  
		 *  processing of CreateTenderPayload is a call to bridgeExecute to put
		 *  Tender on NIO queue in Parity Client so need not be in separate thread    
		 */

		if (DEBUG_JSON) System.err.println("CtsBridge run before new SocketServer: currentThread name: '" +
						Thread.currentThread().getName() + "'");
		ctsSocketServer = new CtsSocketServer(MARKET_PORT, this);	// thread enters MarketCreateTenderPayloads
		ctsSocketServer.start();
		
		// TODO construct and start ctsSocketClient in its own thread - for send of MarketCreateTransactionPayloads
		
		while (true) {
			// take() (blocking) from the ArrayBlockingQueue of MarketCreateTenderPayloads
			// CtsSocketServer thread fills createTenderQ
		
			System.err.println("CtsBridge:run: CreateTenderQ size " + createTenderQ.size());
	
			
			try {
				createTender = createTenderQ.take();
			} catch (InterruptedException e1) {
				System.err.println("Interrupted while waiting on createTenderQ");
				e1.printStackTrace();
			}
			// process the removed MarketCreateTenderPayload
			if (createTender == null)	{
				System.err.println("CtsBridge:run: tender is null");
			}
			
			// replace with conditional expression for buySide/sellSie
			if ( createTender.getSide() == SideType.BUY)	{
				 try	{
					tempParityOrderId = buySide.bridgeExecute
					 	(getClient(), createTender.getQuantity(), 
						4702127773838221344L , createTender.getPrice());
				 } catch (IOException e) {
					 System.out.println("error: CtsBridge: Connection closed");
				 }	
			} else	{	// SELL
				try	{
					// TODO will take map output <Interval, String>
					tempParityOrderId = sellSide.bridgeExecute
					 	(getClient(), createTender.getQuantity(), 
						4702127773838221344L , createTender.getPrice());
				} catch (IOException e) {
					 System.out.println("error: CtsBridge: Connection closed"); 
				}
						
			// iterate
			
				}
		}
	}
	

	/*
	 * methods to process POE protocol events - note that the parameter types
	 * vary across the protocol messsages.
	 * 
	 * orderId is String message.orderId for all of the POE message types
	 * 
	 * Order and outoing protocol message are sent by calls to EnterCommand
	 * buySide and sellSide
	 */
	static void orderAccepted(POE.OrderAccepted message)	{
		// process the orderAccepted - FUTURE
	}
	
	static void orderRejected(POE.OrderRejected message)	{
		//process the orderRejected - FUTURE
	}
	
	/*
	 * The OrderExecuted POE message does not have side; correlate by orderId
	 */
	static void orderExecuted(POE.OrderExecuted message, String s) {
		// process orderExecuted POE message
		/*
		 * process orderExecuted POE message
		 * 
		 * Generate MarketCreateTransactionPayload and send to LME.
		 * Notes:
		 * 		Side is implicit and can be determined from the OrderId
		 * 		use map to determine side of the corresponding EiTender from map
		 */
		
		/*
		 * TODO use map to determine side of the corresponding EiTender from map
		 */
		MarketCreateTransactionPayload marketCreateTransaction = new MarketCreateTransactionPayload
				(s, message.quantity, message.price, message.matchNumber, SideType.BUY);
		System.err.println("CtsBridge.orderExecuted: " + marketCreateTransaction.toString());
		
		// 	TODO Send via socket marketCreateTransactionPayload to LME
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
		final long constantCtsTenderId = 91L;
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
			createTenderPayload[i] = new MarketCreateTenderPayload(tempSide, quantity[i], price[i], 91, ctsInterval, dtStart);
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
		// CTS price * 10 **2 decimal places 119 == Parity price 11900
		int i;
		long longInstrument = 4702127773838221344L; // AAPL instrument
			
		for (i= 0; i < 10; i++)	{
			//choose buy or sell side by modular arithmetic
			if ( i%2 == 0)	{
				// even number - sell
				 try	{
					 orderIds[i] = sellSide.bridgeExecute
							 (getClient(), quantity[i],
							 longInstrument,price[i]);
				 } catch (IOException e) {
					 System.out.println("error: CtsBridge: Connection closed");
				 }	
			} else	{
				// odd number - buy
				try	{
					orderIds[i] = buySide.bridgeExecute(
								client, quantity[i],
								longInstrument, price[i]);
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
	
}
