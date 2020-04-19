package com.paritytrading.parity.client;

// does this statically import all Terminalclient elements, or import only static?

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

/*
 * This class is used to send and receive (via Spring RestContoller interactions)
 * messages both ways between the LME and the enhanced Parity Terminal Client
 * 
 * An EiCreateTender message will be sent from the LME, turned into a 
 * Parity Order Entry:
 * 
 * 		Initial implementation does not return anything to LME
 * 
 * 		Future versions will use the messages sent by the Parity Engine to
 * 		generate return values, and POST back any matches in which an order
 * 		participates, for EiCreateTransaction from the LME.
 * 
 * All prices and quantities are of the minimum increment; the minimum increment
 * MUST be consistent across this enhanced Terminal Client.
 * 
 * Mapping between CTS tenders and parity orders are maintained in this class.
 */

/*
 * PLEASE NOTE that this class is planned to run multiple Spring
 * RestControllers for POST and PUT operations between CTS and Parity. 
 * 
 * Terminal client message will go to the terminal window executing the shaded
 * jar, so manual entry of orders should continue to work.
 * 
 * By design the orders and trades commands should also continue to work in
 * parallel with order entry from the LME
 * 
 * To Parity System
 * 		Buy orders
 * 		Sell orders
 * 
 * Future information retrieved from Parity System Event Visitor
 * 		status of orders (which includes matches by which orders are fulfilled)
 * 
 * Alternative;u, the ticker and/or reporter applications could be used for
 * the *from* info
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
	EnterCommand buySide, sellSide; 	
	Instruments instruments;
	TerminalClient client;
	// events may be used for detecting order entered/cancelled, updates from trade
	Events events;
	

	// arrays for volatile parts of an order via GetCommand.ctsBridgeExecute
	// Other parts include the instrument and client
	private Instrument localInstrument;
	private static long[] quantity = new long[10];
	private static long[] price = new long[10];
	private static String[] orderIds = new String[10];
	
	// for randomized quantity and price
	final static Random rand = new Random();	

	

	
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
		
		//initialize non-fixed parameter array for test tenders with random values
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
			System.out.println("  " + i + "    " + quantity[i] + "       " + (price[i]/100));
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
	 * 	setClient - for TerminalClient to set for buy.bridgeExecute
	 * 	setInstruments - for TerminalClient to set for iteration/lookup
	 * 	setBuySide and setSellSide - for EnterCommand instances to 
	 * 			set buySide and sellSide
	 */
	public TerminalClient getClient() {
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
	
}
