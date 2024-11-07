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

/*
 *	Comments that start with "CTS " refer to code changes for CTS integration
 */


class TradesCommand implements Command {

	/*
	 * CTS execute command including command line scan then pull
	 * matches (client.getEvents) and print.
	 * 
	 * Possible that the TerminalClient tracks events - look for
	 * PMD listener
	 */
	
	/*
	 * CTS check Reporter for simpler Trade reporting
	 */
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

        /*
         * CTS why are collect and printf in italic?
         */
        printf("\n");
        printf(header.format());

        /*
         * CTS Trade listener seems to put events into Trades,
         * might be able to simply read those
         */
        for (Trade trade : Trades.collect(client.getEvents()))
            printf("%s\n", trade.format(client.getInstruments()));
        printf("\n");
    }

    @Override
    public String getName() {
        return "trades";
    }

    @Override
    public String getDescription() {
        return "Display occurred trades";
    }

    @Override
    public String getUsage() {
        return "trades";
    }

}
