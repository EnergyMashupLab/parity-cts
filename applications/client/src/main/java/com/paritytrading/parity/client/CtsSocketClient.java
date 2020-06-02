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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*/

/*
 * 	This socket client is for communication to the LME for MarketCreateTransactionPayload
 * 
 * 	The payloads are expected to be already serialized in JSON. LME opens CtsSocketServer on
 * 	LME_PORT: CtsSocketClient.startConnection(("127.0.0.1", LME_PORT)
 * 
 *  CtsBridge when an orderExecuted is invoked from the POE protocol engine in parity-client;
 *  this will generate two or more MarketCreateTransactionPayloads (one for each party engaged
 *  in the market trade) and send them one at a time into the CTS system via the LME.
 *  
 *  TODO these messages could be bundled, to avoid additional network traffic
 *	
 *	MarketCreateTransaction payloads go to LmeRestController where
 *	they are used to generate EiCreateTransaction with new TransactionId, using the
 * 	tender informmation passed by the LME to CtsBridge
 * 
 * Global values in CtsBridge and in CTS

public final int LME_PORT = 39401;		// Socket Client in CtsBridge sends MarketCreateTransaction
										//		to ServerSocket in LME reads CreateTransaction
public final int MARKET_PORT = 39402;	// Socket Server in Market reads CreateTender 
 */



/*
 * Start by new CtsSocketClient.startConnection(("127.0.0.1",
 *	port matching server)
 *	
 *	Insert into LmeRestController to receive Tender information
 *	and generate EiCreateTransaction with new TransactionId, using the
 * Tenderid passed from CtsBridge
 */

/*
	public final int LME_PORT = 39401;		// for Socket Server in LME takes CreateTransaction
 */


public class CtsSocketClient	extends Thread {

//	private static final Logger logger = LogManager.getLogger(
//			CtsSocketClient.class);
	
	final ObjectMapper mapper = new ObjectMapper();

	private Socket clientSocket;
	private PrintWriter out;
	private static InputStreamReader inStream;
	private BufferedReader in;
	
	public static final int MARKET_PORT = 39402;
	public static final int LME_PORT = 39401;		// Socket Client in CtsBridge sends MarketCreateTransaction
	
	private static int port = LME_PORT;	// CreateTransaction from Market to LME
	private static String ip = "127.0.0.1";
	
//	private static String driverLine;	// input line to drive to Market - json encoding
//	private static String s;
//	private static int ITERATIONS = 27;
	
	CtsBridge bridge;	// to access bridge.createTransactionQ
	
	public CtsSocketClient()	{
    	System.err.println("CtsSocketClient: constructor no args" +
    			port + " Thread " + Thread.currentThread().getName());
	}

	@Override
	public void run() {
		MarketCreateTransactionPayload createTransaction;
//		EiTender tender;
//		MarketCreateTransactionPayload toJson;
		String jsonString = null;	// for JSON string
 		System.err.println("CtsSocketClient.run() port: " + port +
 				" " + Thread.currentThread().getName());
		
		  try {
				clientSocket = new Socket(ip, port);
				out = new PrintWriter(clientSocket.getOutputStream(), true);
				
				System.err.println("CtsSocketClient.run out is " + out.toString());
				
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		  } catch (IOException e) {
//				logger.debug("SocketClient start IOException: " + e.getMessage());
		  }
		  
		  while(true) {
//			  logger.info("SocketClient while loop head");
			  try {
				  	// take the first MarketCreateTransactionPayload from the queue, serialize, and send
	              	System.err.println("CtsSocketClient.run before bridge.createTransactionQ.take " + Thread.currentThread().getName());
	              	createTransaction = bridge.createTransactionQ.take();
	              	System.err.println("CtsSocketClient.run after bridge.createTransactionQ.take " + Thread.currentThread().getName());
				
	              	jsonString = mapper.writeValueAsString(createTransaction);
	              	if (jsonString == null)	{
	              		System.err.println("CtsSocketClient.run jsonString null");
	              	}	else	{
	              		System.err.println("CtsSocketClient.run jsonString to Lme " + jsonString);
	              	}
	              	
	              	out.println(jsonString);	
	              	
	              	System.err.println("CtsSocketClient.run Json string written to Lme " + jsonString);
//	              	logger.info("SocketClient after println of json " + jsonString);			
			} catch (InterruptedException e) {
					System.err.println("createTransactionQ.take interrupted");
					e.printStackTrace();
			} catch (JsonProcessingException e) {
					System.err.println("JsonProcessingException: Input MarketCreateTenderPayload " + e);
					e.printStackTrace();
			}
		  }  
	}

	public String sendMessage(String msg) {	// not used TODO delete
		  try {
				out.println(msg);
				System.err.println("Client sendMessage: " + msg);
				return in.readLine();
		  } catch (Exception e) {
//				logger.debug("SocketClient sendMessage: " + e.getMessage());

				return null;
		  }
	}

	public void stopConnection() {	// not used TODO
		  try {
			in.close();
			out.close();
			clientSocket.close();
	  } catch (IOException e) {
//			logger.debug("SocketClient stop IOException: " + e.getMessage());
	  }
	}
	
    
    public CtsSocketClient(int port, CtsBridge bridge)	{
    	System.err.println("CtsSocketClient: constructor 2 parameters bridge and Port: " +
    			port + " " + Thread.currentThread().getName());
    	this.bridge = bridge;
    	this.port = port;
    	if (bridge == null)	{
    		System.err.println("CtsSocketClient: constructor:this.bridge is null");
    	}
    }
}
