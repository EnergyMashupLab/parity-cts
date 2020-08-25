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

import com.paritytrading.parity.net.poe.POE;
import com.paritytrading.parity.net.poe.POEClientListener;
import java.util.ArrayList;
import java.util.List;

// Import log4j classes.
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/*
 *	WTC
 *	Changed to allow hooks for CtsBridge to emit EiCreateTransaction
 *	and EiCanceledTender and error information
 *		orderAccepted
 *		orderRejected
 *		orderExecuted
 *		orderCanceled
 *
 *	Events is where events are created based on incoming messages
 *	from Parity system.
 *
 *	From Event.java - in common though there are casting issues
 *      public final long   timestamp;
 *      public final String orderId;
 */
class Events implements POEClientListener {
	
	// Define a static logger variable so that it references the
    // Logger instance named "MyApp".
    private static final Logger logger = LogManager.getLogger(Events.class);

	private final List<Event> events;

	Events() {
		events = new ArrayList<>();
	}

	synchronized void accept(EventVisitor visitor) {
		for (Event event : events)
			event.accept(visitor);
	}

	@Override
	public void orderAccepted(POE.OrderAccepted message) {
		String parityOrderId = new String(message.orderId);

//		System.out.println("Events.orderAccepted " + parityOrderId);
		CtsBridge.orderAccepted(message, parityOrderId);
		add(new Event.OrderAccepted(message));
	}

	@Override
	public void orderRejected(POE.OrderRejected message) {
		String parityOrderId = new String(message.orderId);

//		System.out.println("Events.orderRejected " + parityOrderId);
		CtsBridge.orderRejected(message, parityOrderId);
		add(new Event.OrderRejected(message));
	}

	/*
	 * POE.OrderExecuted.java includes attributes we need:
	 * 		orderId - maps to CtsTenderId in CtsBridge 
	 * 		quantity 
	 * 		price 
	 * 		matchNumber
	 * Not used by CTS
	 * 		timestamp (on message)
	 *		liquidityFlag
	 */
	@Override
	public void orderExecuted(POE.OrderExecuted message) {
		String parityOrderId = new String(message.orderId);
		
		logger.debug("Events.orderExecuted " + Thread.currentThread().getName() +
			" parityOrderId " + parityOrderId);

		// Call to CtsBridge for processing
		CtsBridge.orderExecuted(message, parityOrderId);
		
		add(new Event.OrderExecuted(message));
	}

	@Override
	public void orderCanceled(POE.OrderCanceled message) {
		String parityOrderId = new String(message.orderId);

		logger.debug("Events.orderCanceled " + parityOrderId +
				" CanceledQuantity " + message.canceledQuantity +
				 " Reason " + (char)message.reason);

		// Call to CtsBridge for processing
		CtsBridge.orderCanceled(message, parityOrderId);

		add(new Event.OrderCanceled(message));
	}

	private synchronized void add(Event event) {
		/*
		 * DEBUG 
		 * 		String orderId;
		 * 		long timestamp;
		 * 		orderId = event.orderId;
		 * 		timestamp =
		 * 		event.timestamp;
		 */

		// System.out.println("events.add(event)");
		events.add(event);
	}

}
