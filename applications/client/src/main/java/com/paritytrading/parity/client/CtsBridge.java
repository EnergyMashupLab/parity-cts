package com.paritytrading.parity.client;

// does this statically import all Terminalclient elements, or import only static?

import static com.paritytrading.parity.client.TerminalClient.*;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.parity.net.poe.POE;
import com.paritytrading.parity.util.Instrument;
import com.paritytrading.parity.util.Instruments;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.NoSuchElementException;
import java.util.Scanner;

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

	
	/*
	 * 	setClient - for TerminalClient to set client to use for
	 * 				 buy.bridgeExecute
	 * 	setInstruments - for TerminalClient to set CtsBridgeinstruments
	 * 				to use for iteration/lookup
	 * 	setBuySide and setSellSide - for EnterCommand instances to set buySide
	 * 				and sellSide
	 */
	
	public CtsBridge() {
		// Work in progress
		// AAPL is long 4702127773838221344 price price * 10 **2 decimal places 119 -> 11900
		initTenders();
		sendTenders();
	}
	
	private EnterCommand buySide, sellSide;
	private Instrument localInstrument;
	private Instruments instruments;
	private long longInstrument = 4702127773838221344L; // AAPL instrument
	private TerminalClient client;
	private long[] quantity = new long[10];
	private long[] price = new long[10];
	private String[] orderIds = new String[10];
	
	void initTenders()	{
		int i;
		
		if (buySide == null)	{
			System.err.println("buySide is null");
		}	else	{
			System.err.println("buySide is NOT null");
		}
		if (sellSide == null)	{
			System.err.println("sellSide is null");
		}	else	{
			System.err.println("sellSide is NOT null");
		}
		
		//initialize variable part of tender test information
		
		for (i = 0; i < 10; i++)	{
			quantity[i] = 10 + i;
			price[i] = (30 + 5 * i) * 100;	// price 2 decimal digits in config
		}
	}
	
	void sendTenders()	{
		int i;
		
		for (i= 0; i < 10; i++)	{
			//choose buy or sell side by modular arithmetic
			if ( i%2 == 0)	{
				// even number - sell
				 try	{
					 orderIds[i] = 
							 sellSide
							 .
							 bridgeExecute
							 (client, 
							 quantity[i],
							 longInstrument,
							 price[i]);
				 } catch (IOException e) {
					 System.err.println("error: Connection closed");
				 }
				    		
			} else	{
				// odd number - buy
				try	{
					orderIds[i] = buySide.bridgeExecute(client, quantity[i], longInstrument,price[i]);
				} catch (IOException e) {
					 System.err.println("error: Connection closed");
				}
			}
			System.err.println("CtsBridge: Returned OrderId '" + orderIds[i]);
		}
	}

	public TerminalClient getClient() {
		return client;
	}
	public void setClient(TerminalClient client) {
		this.client = client;
	}
	public Command getBuySide() {
		return buySide;
	}
	public void setBuySide(EnterCommand buyCmd) {
		this.buySide = buyCmd;
	}
	public Command getSellSide() {
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
