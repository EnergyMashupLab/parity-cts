/*
 * Copyright 2014 Parity authors
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

import static com.paritytrading.parity.client.TerminalClient.*;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.parity.net.poe.POE;
import com.paritytrading.parity.util.Instrument;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.io.UnsupportedEncodingException;

class EnterCommand implements Command {

    private final POE.EnterOrder message;
    
    // place in buy/sell Commands	
    private CtsBridge ctsBridge; 
    
    // WTC hookExecute is called only when hookExecuteFlag is true
	Boolean hookExecuteFlag = false; // if true executes hook first time per side

    /*
     * WTC Constructor creates the POE (Parity Order Entry protocol) message
     * with Side from the constructor parameter
     */
    EnterCommand(byte side) {
        this.message = new POE.EnterOrder();
        this.message.side = side;
    }

    /*
     * WTC Execute executes this command and parses the command line, sending
     * the result to execute(Client, long, long, long) below.
     */
    
    /*
     * Execute with a Scanner to finish command line parsing, then
     * call private execute to process
     */

    @Override
    public void execute(TerminalClient client, Scanner arguments) throws IOException {
        try {
            double quantity   = arguments.nextDouble();
            long   instrument = ASCII.packLong(arguments.next());
            double price      = arguments.nextDouble();

            if (arguments.hasNext())
                throw new IllegalArgumentException();

            Instrument config = client.getInstruments().get(instrument);
            if (config == null)
                throw new IllegalArgumentException();

            execute(client, Math.round(quantity * config.getSizeFactor()),
            			instrument, Math.round(price * config.getPriceFactor()));
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException();
        }
    }
    
    /*
     * WTC Chained from execute after parsing command line
     */
    private void execute(TerminalClient client, long quantity, long instrument,
    		long price) throws IOException {
    	
    	String orderId = client.getOrderIdGenerator().next();

        /*
         * WTC insert field values into the message
         * THIS MAY NOT BE THREAD SAFE
         */
        ASCII.putLeft(message.orderId, orderId);
        message.quantity   = quantity;
        message.instrument = instrument;
        message.price      = price;
        
        /*
         * WTC This is the actual message send
         */

        client.getOrderEntry().send(message);

        printf("\nOrder ID\n----------------\n%s\n\n", orderId);
        
        // WTC hook - one level call based on flag
        if (hookExecuteFlag)	{
        	hookExecuteFlag = false;
        	System.err.println("before hookExecute call quantity " + quantity + 
        			" instrument long " + instrument + " price " + price);
        	hookExecute(client, quantity, instrument, price);
        }
    }

    /*
     * WTC hookExecute is called from execute(client, long, long, long) for 
     * hooking into that function. This version creates a specific order with
     * the same side. 
     * 
     * Set hookExecuteFlag to true to mirror first order
     */
    void hookExecute(TerminalClient client, long quantity, long instrument,
    		long price) throws IOException	{
    	// create a tender quantity 19 instrument AAPL price 123
      	System.err.println("inside hookExecute quantity " + quantity +
      			" instrument long " + instrument + " price " + price);
    	execute(client, 19, instrument, 123*100);
    }
    /*
     * Setter for CTS integration - method to set ctsBridge attribute
     */
    public void setBridge(CtsBridge bridge)	{
    	Boolean nullBridge;
    	nullBridge = (bridge == null);
    	this.ctsBridge = bridge;
    }

    /*
     * WTC For calls from CtsBridge to equivalent of EnterCommand private
     * execute. This version returns String form of orderId to allow mapping
     * to and from Parity orderIds.
     */
    public String bridgeExecute(TerminalClient client, long quantity, long instrument,
    		long price) throws IOException {
    	
    	String orderId = client.getOrderIdGenerator().next();

        /*
         * WTC insert field values into the message
         * THIS MAY NOT BE THREADSAFE
         */
        ASCII.putLeft(message.orderId, orderId);
        message.quantity   = quantity;
        message.instrument = instrument;
        message.price      = price;
        
        /*
         * WTC Actual message send
         */
        client.getOrderEntry().send(message);

        /*
         *	WTC Return he orderId just sent as a String
         */
        return new String(message.orderId);
    }
    
    public EnterCommand getSideCommand()	{
    	return this;
    }
    
    public String getSide()	{
    	byte	sideByte[];
     	String	sideString = null;
    	
    	sideByte = new byte[1]; sideByte[0] = this.message.side;
    			
    	try	{
    		sideString = new String(sideByte, "UTF-8");
    	} catch	(UnsupportedEncodingException e)	{
    		System.err.println("Unsupported EncodingException in getSide");
    	}
    	
    	return sideString;
    }

    @Override
    public String getName() {
        return message.side == POE.BUY ? "buy" : "sell";
    }

    @Override
    public String getDescription() {
        return "Enter a " + getName() + " order";
    }

    @Override
    public String getUsage() {
        return getName() + " <quantity> <instrument> <price>";
    }

}
