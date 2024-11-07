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

import static org.jvirtanen.util.Applications.*;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.nassau.soupbintcp.SoupBinTCP;
import com.paritytrading.parity.net.poe.POE;
import com.paritytrading.parity.util.Instruments;
import com.paritytrading.parity.util.OrderIDGenerator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Stream;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jvirtanen.config.Configs;

// Determine whether needed
// to setBridge in EnterCommand objects
import com.paritytrading.parity.client.EnterCommand;
import com.paritytrading.parity.client.CtsBridge;

/*
 *	Comments that start with "CTS " refer to code changes for CTS integration
 */


class TerminalClient implements Closeable {

    static final Command[] COMMANDS = new Command[] {
        new EnterCommand(POE.BUY),
        new EnterCommand(POE.SELL),
        new CancelCommand(),
        new OrdersCommand(),
        new TradesCommand(),
        new ErrorsCommand(),
        new HelpCommand(),
        new ExitCommand(),
    };
    

    // link to the CtsBridge and use for setters
    static CtsBridge ctsBridge; 

    static final String[] COMMAND_NAMES = Stream.of(COMMANDS)
            .map(Command::getName)
            .toArray(String[]::new);
    static final Locale LOCALE = Locale.US;
    static final long NANOS_PER_MILLI = 1_000_000;
    private final Events events;
    private final OrderEntry orderEntry;
    private final Instruments instruments;
    private final OrderIDGenerator orderIdGenerator;
    private boolean closed;

    private TerminalClient(Events events, OrderEntry orderEntry,
    		Instruments instruments) {
        this.events      = events;
        this.orderEntry  = orderEntry;
        this.instruments = instruments;
        this.orderIdGenerator = new OrderIDGenerator();
    }

    /*
     * open is the first point where instruments, events and client are available
     * to create new CtsBridge
     */
    static TerminalClient open(InetSocketAddress address, String username,
            String password, Instruments instruments) throws IOException {
        Events events = new Events();
        TerminalClient terminalClient;	// temp for result of new TerminalClient()
        EnterCommand buy, sell;
        OrderEntry orderEntry = OrderEntry.open(address, events);

        SoupBinTCP.LoginRequest loginRequest = new SoupBinTCP.LoginRequest();

        ASCII.putLeft(loginRequest.username, username);
        ASCII.putLeft(loginRequest.password, password);
        ASCII.putRight(loginRequest.requestedSession, "");
        ASCII.putLongRight(loginRequest.requestedSequenceNumber, 0);

        orderEntry.getTransport().login(loginRequest);
        
        terminalClient = new TerminalClient(events, orderEntry, instruments);
        
        /*
         * First place in execution where client, events, instruments, and Command classes
         * for buy and sell (subclassed to EnterCommand) are all available.
         * 
         * Captured the new TerminalClient from this static routine for CtsBridge.
         * 
         * Extract the buy and sell side EnterCommand objects and set their ref in ctsBridge
         */      
         ctsBridge = new CtsBridge(terminalClient, events, instruments);
         
         // tell the buy and sell EnterCommand instances about CtsBridging
         buy = (EnterCommand) COMMANDS[0];
         buy.setBridge(ctsBridge);
         
         sell = (EnterCommand) COMMANDS[1];
         sell.setBridge(ctsBridge); 
         
         ctsBridge.setBuySide(buy);
         ctsBridge.setSellSide(sell);
         // and start the CtsBridge thread
         ctsBridge.start();
         
         // and return the constructed terminalClient
         return terminalClient;
    }

    OrderEntry getOrderEntry() {
        return orderEntry;
    }

    Instruments getInstruments() {
        return instruments;
    }

    OrderIDGenerator getOrderIdGenerator() {
        return orderIdGenerator;
    }

    Events getEvents() {
        return events;
    }
    
    /*
     * CTS getters for Buy, Sell EnterCommand instances
     * 
     * EnterCommand.bridgeExecute() allows order entry from
     * CtsBridge, returning String orderId
     * 
     * Note that POE protocol includes messages from server. These are
     * in Events and Event, and call to CtsBridge.
     * 
     * This integration package uses Order Executed and soon will
     * use Order Canceled
     * 		Order Entered (order entered by System)
     * 		Order Added (order put into correct Order Book
     * 		* Order Canceled (order canceled for future CTS EiCancelTender
     * 		* Order Executed (one message per order, for resting order(s) and incoming
     * 				order numbers and preparing MarketCreateTransactionPayload)
     */
    public TerminalClient getClient()	{
    	return this;
    }
    
    // Return the instance of EnterCommand that sends enter buy
    public static EnterCommand getBuy()	{
    	EnterCommand saveEnter;
    	Command saveCommand = COMMANDS[0];

    	saveEnter = (EnterCommand) saveCommand;   	
    	if (saveEnter == null)	{
    		System.out.println("TerminalClient: getBuy: saveGet is null");
    	}	else	{
    		System.out.println("TerminalClient: getBuy: saveGet is not null");	
    	}

    	System.out.println(COMMANDS[0].getDescription()); 	
    	System.out.println("TerminalClient: getBuy: side is " + saveEnter.getSide());
    	return saveEnter;
    }
    
    
    // Return the instance of EnterCommand that sends enter sell
    public static EnterCommand getSell()	{
    	EnterCommand saveEnter;
    	Command saveCommand = COMMANDS[1];

    	saveEnter = (EnterCommand) saveCommand;   	
    	if (saveEnter == null)	{
    		System.out.println("TerminalClient: getBuy: saveEnter is null");
    	}	else	{
    		System.out.println("TerminalClient: getBuy: saveEnter is not null");	
    	}

    	System.out.println(COMMANDS[0].getDescription()); 	
    	System.out.println("TerminalClient: getSell: side is " + saveEnter.getSide() );
    	return saveEnter;
    }
    
    /*
    // Return the instance of EnterCommand that performs Sell
    public static Command getSell()	{
    //	System.out.println(COMMANDS[1].getDescription());
    	return COMMANDS[1];
    }
    */
    
    void run() throws IOException {
        LineReader reader = LineReaderBuilder.builder()
            .completer(new StringsCompleter(COMMAND_NAMES))
            .build();

        printf("Type 'help' for help.\n");

        while (!closed) {
            String line = reader.readLine("> ");
            if (line == null)
                break;

            Scanner scanner = scan(line);

            if (!scanner.hasNext())
                continue;

            Command command = findCommand(scanner.next());
            if (command == null) {
                printf("error: Unknown command\n");
                continue;
            }

            try {
                command.execute(this, scanner);
            } catch (IllegalArgumentException e) {
                printf("Usage: %s\n", command.getUsage());
            } catch (ClosedChannelException e) {
                printf("error: Connection closed\n");
            }
        }

        close();
    }

    @Override
    public void close() {
        orderEntry.close();

        closed = true;
    }

    static Command findCommand(String name) {
        for (Command command : COMMANDS) {
            if (name.equals(command.getName()))
                return command;
        }

        return null;
    }

    static void printf(String format, Object... args) {
        System.out.printf(LOCALE, format, args);
    }

	private Scanner scan(String text) {
        Scanner scanner = new Scanner(text);
        scanner.useLocale(LOCALE);

        return scanner;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1)
            usage("parity-client <configuration-file>");
        
        try {
            main(config(args[0]));
        } catch (EndOfFileException | UserInterruptException e) {
            // Ignore.
        } catch (ConfigException | FileNotFoundException e) {
            error(e);
        }
    }

    private static void main(Config config) throws IOException {
        InetAddress orderEntryAddress  = Configs.getInetAddress(config, "order-entry.address");
        int         orderEntryPort     = Configs.getPort(config, "order-entry.port");
        String      orderEntryUsername = config.getString("order-entry.username");
        String      orderEntryPassword = config.getString("order-entry.password");

        Instruments instruments = Instruments.fromConfig(config, "instruments");
        
        // CTS log4j2 setup in working directory
        System.setProperty("log4j2.configurationFile","./log4j2.xml");

        // and open then run
        TerminalClient.open(new InetSocketAddress(orderEntryAddress, orderEntryPort),
                orderEntryUsername, orderEntryPassword, instruments).run();
    }

}
