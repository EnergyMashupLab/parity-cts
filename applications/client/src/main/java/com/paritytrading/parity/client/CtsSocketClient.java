package com.paritytrading.parity.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.UnknownHostException;
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
 * 	This socket client communicates with the LME for MarketCreateTransactionPayloads.
 * 
 * 	The payloads are serialized in JSON. This CtsSocketClient opens localhost:LME_PORT
 * 
 *  Each orderExecuted message from the POE protocol engine generates one
 *  MarketCreateTransactionPayload on createTransactionQ, which are sent one at a time
 *  to the LME where they are used to generate EiCreateTransactionPayloads with
 *  	New TransactionId
 *  	Included EiTender with original TenderId and cleared quantity and price
 *  
 *  TODO these messages could be bundled, to avoid additional network traffic
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
	public static final int LME_PORT = 39401;
	
	private static int port = LME_PORT;	// CreateTransaction from Market to LME
	private static String ip = "127.0.0.1";	
	private static String hostName = "localhost";
	CtsBridge bridge;	// to access bridge.createTransactionQ
	
	public CtsSocketClient()	{
    	System.err.println("CtsSocketClient: constructor no args. port " +
    			port + " ip " + ip + " " + Thread.currentThread().getName());
	}
	
    public CtsSocketClient(int port, CtsBridge bridge)	{
//    	System.err.println("CtsSocketClient: constructor 2 parameters bridge port " +
//    			port + " ip " + ip + " " + Thread.currentThread().getName());
    	this.bridge = bridge;
    	this.port = port;
    	if (bridge == null)	{
    		System.err.println("CtsSocketClient: constructor:this.bridge is null");
    	}
    }
    

	@Override
	public void run() {
		MarketCreateTransactionPayload createTransaction;
		String jsonString = null;	// for JSON string
		boolean tryingToCreateSocket = true;
		long retryCount = 0;
		
 		System.err.println("CtsSocketClient.run() port: " + port +
 				" ip " + ip + " " + Thread.currentThread().getName());
 		
 		// Sleep to improve probability of server socket being ready in CTS
 		try {
			Thread.sleep(20*1000);
		} 	catch (InterruptedException e) {
			System.err.println("CtsSocketClient.run after sleep");
			e.printStackTrace();
		}
// 		System.err.println("CtsSocketClient.run after sleep");
 		
 		try {
			clientSocket = new Socket(ip, port);
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("CtsSocketClient.run UnknownHost");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("CtsSocketClient.run IOException");
			e.printStackTrace();
		}
 		
// 		System.err.println("CtsSocketClient.run after new Socket");
 				
		if (clientSocket == null)	{
			System.err.println("CtsSocketClient.run clientSocket null before loop");
 		}
	
		  
	  while(true) {
		  try {
			  	// take the first MarketCreateTransactionPayload from the queue, serialize, and send
//              	System.err.println(
//              			"CtsSocketClient.run before bridge.createTransactionQ.take size "
//              			+ bridge.createTransactionQ.size() + " " +
//              			Thread.currentThread().getName());
              	
              	createTransaction = bridge.createTransactionQ.take();
              	
//              	System.err.println(
//              			"CtsSocketClient.run after bridge.createTransactionQ.take size " +
//              			bridge.createTransactionQ.size() + " " +
//              			Thread.currentThread().getName());
			
              	jsonString = mapper.writeValueAsString(createTransaction);

              	if (jsonString == null)	{
              		System.err.println("CtsSocketClient.run loop: jsonString null - continue");
              		continue;
              	}	

          		if (clientSocket == null) {
              		System.err.println(
              				"CtsSocketClient.run loop: clientSocket is null - continue");
              		continue;
          		}
          		
//				System.err.println("clientSocket non-null");
				
				if (out == null)	{
					System.err.println("out is null - continue");
					continue;
				}
					
//				System.err.println("out is " + out.toString());
//          		System.err.println(
//          				"CtsSocketClient.run jsonString to send to Lme " + jsonString);

          		out.println(jsonString);
          		
          		
              	System.err.println("CtsSocketClient.run Json string written to Lme " + jsonString);
//		              	logger.info("SocketClient after println of json " + jsonString);		
              	
			} catch (InterruptedException e1) {
				System.err.println("createTransactionQ.take interrupted");
				e1.printStackTrace();
			} catch (JsonProcessingException e2) {
				System.err.println("JsonProcessingException: Input MarketCreateTenderPayload " + e2);
				e2.printStackTrace();
			}	catch (NullPointerException e3)	{
				System.err.println("NullPointerException: CtsSocketClient loop " + e3);
				e3.printStackTrace();
			}
		  }  
		  
	}



	public void stopConnection() {	// not used TODO update for shutdown
		  try {
			in.close();
			out.close();
			clientSocket.close();
	  } catch (IOException e) {
		  System.err.println("CtsSocketClient.stopConnection " + e);
		  e.printStackTrace();	  
//		  logger.debug("SocketClient stop IOException: " + e.getMessage());
	  }
	}
}
