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

import com.paritytrading.parity.util.Instruments;
import com.paritytrading.parity.util.TableHeader;
import java.util.Scanner;

// Import log4j classes.
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

class OrdersCommand implements Command {
	
	// Define a static logger variable so that it references the
    // Logger instance named "MyApp".
    private static final Logger logger = LogManager.getLogger(OrdersCommand.class);

    @Override
    public void execute(TerminalClient client, Scanner arguments) {
        if (arguments.hasNext())
            throw new IllegalArgumentException();

        Instruments instruments = client.getInstruments();

        int priceWidth = instruments.getPriceWidth();
        int sizeWidth  = instruments.getSizeWidth();

        TableHeader header = new TableHeader();

        header.add("Timestamp",       12);
        header.add("Order ID",        16);
        header.add("S",                1);
        header.add("Inst",             8);
        header.add("Quantity", sizeWidth);
        header.add("Price",   priceWidth);

        printf("\n");
        printf(header.format());

        for (Order order : Orders.collect(client.getEvents())) {
        	// DEBUG
//        	System.err.println("in OrdersCommand before print order is null? " + (order == null));
            printf("%s\n", order.format(client.getInstruments()));
        }
        printf("\n");
    }

    @Override
    public String getName() {
        return "orders";
    }

    @Override
    public String getDescription() {
        return "Display open orders";
    }

    @Override
    public String getUsage() {
        return "orders";
    }

}
