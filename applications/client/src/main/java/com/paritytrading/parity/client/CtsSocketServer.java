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

import java.net.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.io.*;
import java.lang.Thread;

import com.fasterxml.jackson.databind.ObjectMapper;

//import java.util.HashMap;
//import java.util.Map;
//import java.util.Iterator;
//import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 *	Comments that start with "CTS " refer to code changes for CTS integration
 */

/*
 * CTS
 * Overview:
 * 		This CtsSocketServer accepts connections on MARKET_PORT. The
 * 		CTS LME Socket Client opens the LME IP address on MARKET_PORT.
 * 		This code processes incoming MarketCreateTenderPayloads.
 * 
 * 		The LME writes JSON-serialized MarketCreateTenderPayloads which
 * 		will be read by this CtsSockerServer, deserialized, and put on
 * 		bridge.marketCreateTenderQueue for further processing in CtsBridge.
 * 
 * 		CtsBridge loops, taking the first element of marketCreateTenderQueue,
 * 		inserts the information into the POE order entry service.
 * 		That call to bridgeExecute returns the parity OrderId.	
 */

public class CtsSocketServer extends Thread	{
	
	private static final Logger logger = LogManager.getLogger(CtsSocketServer.class);
	
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
//	 private static final Logger logger = LogManager.getLogger(CtsSocketServer.class);
    private BufferedReader in;
    private CyclicBarrier clientSocketBarrier;
    
    // Socket Server in LME for CreateTransaction
    public static final int LME_PORT = 39401;
    // Socket Server in Market for CreateTender 
    public static final int MARKET_PORT = 39402;
    public static int port = MARKET_PORT;

    
    final ObjectMapper mapper = new ObjectMapper();
    CtsBridge bridge;	// to access bridge.marketCreateTenderQueue

    @Override
    public void run() {
        String jsonReceived = null;
        MarketCreateTenderPayload payload;
        
    	//	port is set in constructor or by initializer
//     		System.err.println("CtsSocketServer.run() port: " + port +
//    		" '" + Thread.currentThread().getName() + "'");
            logger.debug("CtsSocketServer.run() port: " + port +
   		    " '" + Thread.currentThread().getName() + "'");
   	
        try {
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();
            try {
                clientSocketBarrier.await();
            } catch(BrokenBarrierException e) {
                // System.err.println("clientSocketBarrier was broken.");
                logger.debug("clientSocketBarrier was broken.");
            }

            if (clientSocket == null)
                // System.err.println("CtsSocketServer: clientSocket null after accept");
                logger.debug("CtsSocketServer: clientSocket null after accept");
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(
            		new InputStreamReader(clientSocket.getInputStream()));
            if (in == null || out == null)	{
                // System.err.println("in or out null");
                logger.debug("in or out null");
            }

            while (true)	{
            	//	blocking read on BufferedReader of a JSON serialized ClientCreateTenderPayload 
                
            	// System.err.println("CtsSocketServer.run before in.readLine " + Thread.currentThread().getName());
               logger.debug("CtsSocketServer.run before in.readLine " + Thread.currentThread().getName());
            	jsonReceived = in.readLine();     
//            	System.err.println("CtsSocketServer.run: after in.readLine jsonReceived is '" + 
//            		jsonReceived  + "' Thread " + Thread.currentThread().getName());
                logger.debug("CtsSocketServer.run: after in.readLine jsonReceived is '" + 
           		    jsonReceived  + "' Thread " + Thread.currentThread().getName());
                
                if (jsonReceived == null)	break;
                payload = mapper.readValue(
                		jsonReceived, MarketCreateTenderPayload.class);
                                
//            	System.err.println("CtsSocketServer.run received and put on marketCreateTenderQueue " +
//              		payload.toString());
                logger.debug("CtsSocketServer.run received and put on marketCreateTenderQueue " +
             		payload.toString());
//                
                // Put on bridge.marketCreateTenderQueue for processing by CtsBridge
            	bridge.marketCreateTenderQueue.put(payload);
            
/*
 * CTS use println rather than adjust global log level to Debug
 */	
               System.err.println("CtsSocketServer.run after marketCreateTenderQueue.put size " + 
             			bridge.marketCreateTenderQueue.size() + " " +Thread.currentThread().getName());
                logger.debug("CtsSocketServer.run after marketCreateTenderQueue.put size " + 
             			bridge.marketCreateTenderQueue.size() + " " +Thread.currentThread().getName());
    		}            
        } catch (IOException  e) {       	
            //	LOG.debug(e.getMessage());
            // System.err.println("CtsSocketServer: IOException in readLine?");
            logger.debug("CtsSocketServer: IOException in readLine?");
        	e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void shutdown() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            // System.err.println("CtsSocketServer stop: " + e.getMessage());
            logger.debug("CtsSocketServer stop: " + e.getMessage());
        }
    }
    
    public CtsSocketServer()	{
//    	System.err.println(
//    		"CtsSocketServer: constructor no args port " + port + " " +Thread.currentThread().getName());
   	logger.debug(
   		"CtsSocketServer: constructor no args port " + port + " " +Thread.currentThread().getName());
    }
    
    public CtsSocketServer(int port)	{
//    	System.err.println(
//    		"CtsSocketServer: constructor 1 arg port: " + port + " " +
//    		Thread.currentThread().getName() );
        logger.debug(
   		    "CtsSocketServer: constructor 1 arg port: " + port + " " +
   		    Thread.currentThread().getName() );
    	CtsSocketServer.port = port;
    	
    	CtsSocketServer server = new CtsSocketServer();	
    	// TODO Lambda Expression for separate thread - current is in thread
    }
    
    public CtsSocketServer(int port, CtsBridge bridge, CyclicBarrier barrier)	{
//    	System.err.println("CtsSocketServer: constructor bridge and Port: " 
//    			+ port + " " + Thread.currentThread().getName() );
        logger.debug("CtsSocketServer: constructor bridge and Port: " 
   		    	+ port + " " + Thread.currentThread().getName() );
    	this.bridge = bridge;

      CtsSocketServer.port = port;
      this.clientSocketBarrier = barrier;

    	if (bridge == null)	{
            // System.err.println("CtsSocketServer: constructor:this.bridge is null");
            logger.debug("CtsSocketServer: constructor:this.bridge is null");
    	}
    }
    
    public CtsSocketServer(CtsBridge bridge)	{
//    	System.err.println("CtsSocketServer: constructor bridge: "  +
//        		Thread.currentThread().getName() );
        logger.debug("CtsSocketServer: constructor bridge: "  +
       		    Thread.currentThread().getName() );
    	this.bridge = bridge;
    	if (bridge == null)	{
            // System.err.println("CtsSocketServer: constructor:this.bridge is null");
            logger.debug("CtsSocketServer: constructor:this.bridge is null");
    	}
    	// will start() later with appropriate port - port is static final
    }
}
