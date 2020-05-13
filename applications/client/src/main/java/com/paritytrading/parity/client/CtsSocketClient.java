package com.paritytrading.parity.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*/

/*
 * Start by new CtsSocketClient.startConnection(("127.0.0.1", LME_PORT)
 *	
 *	Insert into LmeRestController to receive Tender information
 *	and generate EiCreateTransaction with new TransactionId, using the
 * Tenderid passed from CtsBridge
 * 
 * Global values in CtsBridge and in CTS

public final int LME_PORT = 39401;		// Socket Client in CtsBridge to Server in LME reads CreateTransaction
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
