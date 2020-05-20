package com.paritytrading.parity.client;

import com.paritytrading.parity.client.TerminalClient.*;
import com.paritytrading.parity.client.EnterCommand.*;

import java.net.*;
import java.io.*;
import java.lang.Runnable;
import java.lang.Thread;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.AbstractQueue;
import java.util.AbstractCollection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;


//	import org.apache.logging.log4j.LogManager;
//  import org.apache.logging.log4j.Logger;

/*
 * Communicates with CtsSocketClient in LmeRestController in CTS
 *
 *	This socket server in parity-cts receives and responds to MarketCreateTransaction
 * message and replies with a MarketCreatedTransaction message.
 */

/*
 * 	Driver for testing does the following (MarketDriverCreateTender.java):
 * 		Socket Client opens socket a localhost:39402 which is the parity-client engine,
 * 		as part of CtsBridge CtsSocketServer.
 * 
 * 		Client drives CtsBridge to insert tenders in the market by reading json serialized
 * 		MarketCreateTenderPayload from a file, writing on the socket.
 * 
 * 		CtsBridge.CtsSocketServer reads the payloads and inserts them one at a time into the
 * 		POE order entry service
 */

public class CtsSocketServer extends Thread	{
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
//	private static final Logger logger = LogManager.getLogger(CtsSocketServer.class);
    private BufferedReader in;
    
    public static final int LME_PORT = 39401;		// for Socket Server in LME takes CreateTransaction
    public static final int MARKET_PORT = 39402;	// for Socket Server in Market takes CreateTender 
    public final int port = MARKET_PORT;
    String jsonReceived = null;
    MarketCreateTenderPayload payload;
    
    final ObjectMapper mapper = new ObjectMapper();
    CtsBridge bridge;
    static CtsSocketServer sockServer;
    ArrayBlockingQueue<MarketCreateTenderPayload> socketServerCreateTenderQ; // class local name for CtsBridge Q

    @Override
    public void run() {
     	System.err.println("CtsSocketServer.run Entry port: " + port + " '" + Thread.currentThread().getName() + "'");
   	
        try {
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();
            if (clientSocket == null)	System.err.println("clientSocket null - after accept");
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            if (in == null || out == null)	System.err.println("in or out null");

            while (true)	{
            	// blocking read on BufferedReader - sent by LME with ClientCreateTenderPayload JSON 
            	jsonReceived = in.readLine();       
                
                System.err.println("CtsSocketServer: start: jsonReceived is '" + jsonReceived 
                			+ "' Thread " + Thread.currentThread().getName());
                
                if (jsonReceived == null)	break;
                
                
                
                payload = mapper.readValue(jsonReceived, MarketCreateTenderPayload.class);
                
                
                System.err.println("CtsSocketServer.run payload received object: " + payload.toString());
                
                
                // add to the CtsBridge queue for processing
                bridge.createTenderQ.put(payload);
    		}
             
        
        
        
        } catch (IOException  e) {       	
            //	LOG.debug(e.getMessage());
        	System.err.println("CtsSocketServer: IOException in readLine?");
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
        	System.err.println("CtsSocketServer stop: " + e.getMessage());
        	//	logger.info("CtsSocketServer: " + e.getMessage());
        }
    }
    
    public CtsSocketServer()	{
    }
    
    public CtsSocketServer(int port)	{
    	//System.err.println("CtsSocketServer: constructor Port: " + port);
    	
    	CtsSocketServer server = new CtsSocketServer();	
    	// TODO Lambda Expression for separate thread - alt implements runnable, place in thread//
    	
        // now a thread server.start();
    }
    
    public CtsSocketServer(int port, CtsBridge bridge)	{
    	System.err.println("CtsSocketServer: constructor Port: " + port);
    	this.socketServerCreateTenderQ = bridge.createTenderQ;
    	this.bridge = bridge;
    	if (bridge == null)	{
    		System.err.println("CtsSocketServer: constructor:this.bridge is null");
    	}
    	
    	//	This created a second server DEBUG
    	//	CtsSocketServer sockServer = new CtsSocketServer();
    	
    	// TODO Lambda Expression for separate thread - alt implements runnable, place in thread//
    	
        //	sockServer.start();
    }
    
}
