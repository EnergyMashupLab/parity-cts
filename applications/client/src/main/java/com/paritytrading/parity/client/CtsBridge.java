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
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

class CtsBridge {	

	public CtsBridge() {
		//	initTenders();
		//	sendTenders();
	}
	
	public CtsBridge(TerminalClient client, Events events, Instruments instruments)	{
		// store information needed to call EnterCommand.bridgeExecute()
		this.client = client;
		this.events = events;
		this.instruments = instruments;
	}

	// set by TerminalClient call to this.setSide()
	// TODO was public, leaving there for now
	static private EnterCommand buySide, sellSide; 	
	static private Instruments instruments;
	static private TerminalClient client;
	/*
	 * Events may be used for detecting order entered/cancelled, updates from trades.
	 * This implementation hooks the message entry into the client and calls out to
	 * CtsBridge methods.
	 */
	Events events;	// local copy if used for evolution
	

	// arrays for volatile parts of an order via GetCommand.ctsBridgeExecute
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

/*
 *		DEBUG Create and Inject 10 random tenders.
 */
	void run()	{
		initTenders();
		sendTenders();
	}
	
	void initTenders()	{
		int i;
		
		// CTS price * 10 **2 decimal places 119 == Parity price 11900
		long longInstrument = 4702127773838221344L; // AAPL instrument
		long randQuantity = 10;	// will be random quantity from 20 to 100
		long randPrice = 60;	// will be random price in dollars from 75 to 125

		String side;
		String buySide = new String("B");
		String sellSide = new String("S");
		
		//initialize array (with non-fixed fields) for random test tenders
		for (i = 0; i < 10; i++)	{
			randQuantity = 20 + rand.nextInt(80);
			randPrice = 75 + rand.nextInt(50);
			
			quantity[i] = randQuantity;
			price[i] = randPrice * 100;
		}
		
		// DEBUG print the headers
		System.out.println("initTenders: initialized array");
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

			System.out.println(" " + (1+i) + "   " + side + " " + quantity[i] + "       " + (price[i]/100));
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

	/*
	 * methods to process POE protocol events - note that the parameter types
	 * vary. 
	 * 
	 * orderId is String message.orderId for all of the mPOE message types
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
		// process orderExecuted
		// Generate MarketCreateTransactionPayload and send to LME
		// Side is implicit in the OrderId
		MarketCreateTransactionPayload marketCreateTransaction = new MarketCreateTransactionPayload
				(s, message.quantity, message.price, message.matchNumber);
		System.err.println(marketCreateTransaction.toString());
		
		// 	POST marketCreateTransaction to LME TODO
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
	
}
