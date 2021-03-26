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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paritytrading.parity.sbe.SBEEncoderDecoder_Parity;

import baseline.*;

import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/*
 * 	This socket client communicates with the LME for MarketCreateTransactionPayloads.
 * 
 * 	The payloads are serialized in JSON. This CtsSocketClient opens localhost:LME_PORT
 * 
 *  Each orderExecuted message from the POE protocol engine generates one
 *  MarketCreateTransactionPayload on createTransactionQueue, which are sent one at a time
 *  to the LME where they are used to generate EiCreateTransactionPayloads with
 *  a New TransactionId and the original TenderId, with quantity and price changed for
 *  cleared values.
 *  
 *  TODO these messages could be bundled, to avoid additional network traffic
 */

public class CtsSocketClient	extends Thread {

	private static final Logger logger = LogManager.getLogger(CtsSocketClient.class);
	
	final ObjectMapper mapper = new ObjectMapper();

	private Socket clientSocket;
	//private PrintWriter out;
	private BufferedReader in;
	BufferedOutputStream bos;
	
	public static final int MARKET_PORT = 39402;
	public static final int LME_PORT = 39401;
	
	private static int port = LME_PORT;	// CreateTransaction from Market to LME
	private static String ip = "127.0.0.1";	
	CtsBridge bridge;	// to access bridge.createTransactionQueue
	
	private static final MarketCreateTransactionPayloadEncoder marketCreateTransactionPayloadEncoder = new MarketCreateTransactionPayloadEncoder();
	private static final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
	
	public CtsSocketClient()	{
    	// System.err.println("CtsSocketClient: constructor no args. port " +
		// 		port + " ip " + ip + " " + Thread.currentThread().getName());
		logger.debug("CtsSocketClient: constructor no args. port " +
    			port + " ip " + ip + " " + Thread.currentThread().getName());
	}
	
    public CtsSocketClient(int port, CtsBridge bridge)	{
//    	System.err.println("CtsSocketClient: constructor 2 parameters bridge port " +
//    			port + " ip " + ip + " " + Thread.currentThread().getName());
		logger.debug("CtsSocketClient: constructor 2 parameters bridge port " +
   				port + " ip " + ip + " " + Thread.currentThread().getName());
    	this.bridge = bridge;
    	CtsSocketClient.port = port;
    	if (bridge == null)	{
			// System.err.println("CtsSocketClient: constructor:this.bridge is null");
			logger.debug("CtsSocketClient: constructor:this.bridge is null");
    	}
    }
    
	@Override
	public void run() {
		MarketCreateTransactionPayload createTransaction;
		String jsonString = null;	// for JSON string
		ByteBuffer bbf = ByteBuffer.allocate(4096);
		UnsafeBuffer buffer = new UnsafeBuffer(bbf);
		
// 		System.err.println("CtsSocketClient.run() port: " + port +
// 				" ip " + ip + " " + Thread.currentThread().getName());
		logger.debug("CtsSocketClient.run() port: " + port +
				" ip " + ip + " " + Thread.currentThread().getName());
 		
 		// Sleep to improve probability of server socket being ready in CTS
 		try {
			Thread.sleep(20*1000);
		} 	catch (InterruptedException e) {
			// System.err.println("CtsSocketClient.run after sleep");
			logger.debug("CtsSocketClient.run after sleep");
			e.printStackTrace();
		}
// 		System.err.println("CtsSocketClient.run after sleep");
		logger.debug("CtsSocketClient.run after sleep");
 		
 		try {
			clientSocket = new Socket(ip, port);
			//out = new PrintWriter(clientSocket.getOutputStream(), true);
			bos = new BufferedOutputStream(clientSocket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (UnknownHostException e) {
			// System.err.println("CtsSocketClient.run UnknownHost");
			logger.debug("CtsSocketClient.run UnknownHost");
			e.printStackTrace();
		} catch (IOException e) {
			// System.err.println("CtsSocketClient.run IOException");
			logger.debug("CtsSocketClient.run IOException");
			e.printStackTrace();
		}
// 		System.err.println("CtsSocketClient.run after new Socket");
		logger.debug("CtsSocketClient.run after new Socket");
 				
		if (clientSocket == null)	{
			// System.err.println("CtsSocketClient.run clientSocket null before loop");
			logger.debug("CtsSocketClient.run clientSocket null before loop");
 		}
			  
	  while(true) {
		  try	{
//				take the first MarketCreateTransactionPayload from the queue, serialize, and send
//              System.err.println(
//              	"CtsSocketClient.run before bridge.createTransactionQueue.take size " +
//              	bridge.createTransactionQueue.size() + " " +
//              	Thread.currentThread().getName());
				logger.debug(
	             	"CtsSocketClient.run before bridge.createTransactionQueue.take size " +
	             	bridge.createTransactionQueue.size() + " " +
	             	Thread.currentThread().getName());
              	
              	createTransaction = CtsBridge.createTransactionQueue.take();
              	
//              System.err.println(
//              	"CtsSocketClient.run after bridge.createTransactionQueue.take size " +
//              	bridge.createTransactionQueue.size() + " " +
//              	Thread.currentThread().getName());
				logger.debug(
             		"CtsSocketClient.run after bridge.createTransactionQueue.take size " +
             		bridge.createTransactionQueue.size() + " " +
             		Thread.currentThread().getName());
			
              	//jsonString = mapper.writeValueAsString(createTransaction);
				
				
				int encodingLengthPlusHeader = SBEEncoderDecoder_Parity.encode(marketCreateTransactionPayloadEncoder, buffer, messageHeaderEncoder, createTransaction);
				
				bos.write(buffer.byteArray(), 0, encodingLengthPlusHeader);
				
				bos.flush();

              	if (jsonString == null)	{
					  // System.err.println("CtsSocketClient.run loop: jsonString null - continue");
					  logger.debug("CtsSocketClient.run loop: jsonString null - continue");
              		continue;
              	}	
          		if (clientSocket == null) {
              		// System.err.println(
					  // 		"CtsSocketClient.run loop: clientSocket is null - continue");
					logger.debug(
              			"CtsSocketClient.run loop: clientSocket is null - continue");
              		continue;
          		}
//				System.err.println("clientSocket non-null");
				logger.debug("clientSocket non-null");
				
				if (bos == null)	{
					// System.err.println("out is null - continue");
					logger.debug("out is null - continue");
					continue;
				}	
//				System.err.println("out is " + out.toString());
				logger.debug("out is " + bos.toString());
//          	System.err.println(
//          		"CtsSocketClient.run jsonString to send to Lme " + jsonString);
				logger.debug(
         		"CtsSocketClient.run jsonString to send to Lme " + jsonString);

          		//out.println(jsonString);
          		    		
//              System.err.println("CtsSocketClient.run Json string written to Lme " + jsonString);
				logger.debug("CtsSocketClient.run Json string written to Lme " + jsonString);
		        logger.debug("after println of json " + jsonString);		
			} catch (InterruptedException e1) {
				// System.err.println("createTransactionQueue.take interrupted");
				logger.debug("createTransactionQueue.take interrupted");
				e1.printStackTrace();
			} catch (JsonProcessingException e2) {
				// System.err.println("JsonProcessingException: Input MarketCreateTenderPayload " + e2);
				logger.debug("JsonProcessingException: Input MarketCreateTenderPayload " + e2);
				e2.printStackTrace();
			}	catch (NullPointerException e3)	{
				// System.err.println("NullPointerException: CtsSocketClient loop " + e3);
				logger.debug("NullPointerException: CtsSocketClient loop " + e3);
				e3.printStackTrace();
			}   catch (Exception e4)	{
				// System.err.println("NullPointerException: CtsSocketClient loop " + e3);
				logger.debug("NullPointerException: CtsSocketClient loop " + e4);
				e4.printStackTrace();
			}
		  }  
	}

	public void stopConnection() {	// not used TODO update for shutdown
		  try {
			//in.close();
			//out.close();
			clientSocket.close();
	  } catch (IOException e) {
		//   System.err.println("CtsSocketClient.stopConnection " + e);
		  logger.debug("CtsSocketClient.stopConnection " + e);
		  e.printStackTrace();	  
		  logger.debug("SocketClient stop IOException: " + e.getMessage());
	  }
	}
}
