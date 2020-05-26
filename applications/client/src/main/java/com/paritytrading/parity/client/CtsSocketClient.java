package com.paritytrading.parity.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

/*
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*/

/*
 * 	This socket client is for communication to/from the LME for MarketCreateTransactionPayload
 * 
 * 	The payloads are expected to be already serialized in JSON. LME opens CtsSocketServer on
 * 	port 39401: CtsSocketClient.startConnection(("127.0.0.1", LME_PORT)
 * 
 *  CtsBridge when an orderExecuted is invoked from the POE protocol engine in parity-client;
 *  this will generate two or more MarketCreateTransactionPayloads (one for each party engaged
 *  in the market trade) and send them one at a time into the CTS system via the LME.
 *  
 *  TODO these messages could be bundled, to avoid additional network traffic
 *	
 *	Insert into LmeRestController to receive information
 *	and generate EiCreateTransaction with new TransactionId, using the
 * 	Tenderid passed from CtsBridge
 * 
 * Global values in CtsBridge and in CTS

public final int LME_PORT = 39401;		// Socket Client in CtsBridge sends MarketCreateTransaction
										//		to ServerSocket in LME reads CreateTransaction
public final int MARKET_PORT = 39402;	// Socket Server in Market reads CreateTender 
 */



//	TODO run in separate thread
// 	TODO TEMP use external client initially. This one to contact LME with Transactions


public class CtsSocketClient {

//	private static final Logger logger = LogManager.getLogger(//CtsSocketClient.class);

	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;

	public void startConnection(String ip, int port) {
		  try {
				clientSocket = new Socket(ip, port);
				out = new PrintWriter(clientSocket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		  } catch (IOException e) {
//					logger.debug("SocketClient start IOException: " + e.getMessage());
			  	System.out.println("SocketClient start IOException: " + e.getMessage());
		  }
	}

	public String sendMessage(String msg) {
		  try {
				out.println(msg);
				System.err.println("Client sendMessage: " + msg);
				return in.readLine();
		  } catch (Exception e) {
//					logger.debug("SocketClient sendMessage: " + e.getMessage());
			  	System.out.println("SocketClient start IOException: " + e.getMessage());
				return null;
		  }
	}

	public void stopConnection() {
		  try {
			in.close();
			out.close();
			clientSocket.close();
	  } catch (IOException e) {
//				logger.debug("SocketClient stop IOException: " + e.getMessage());
		  	System.out.println("SocketClient start IOException: " + e.getMessage());
	  }
	}
}
