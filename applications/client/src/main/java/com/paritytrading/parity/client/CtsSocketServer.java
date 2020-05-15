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
import java.util.concurrent.ArrayBlockingQueue;


//	import org.apache.logging.log4j.LogManager;
//  import org.apache.logging.log4j.Logger;

/*
 * Communicates with CtsSocketClient called from LmeRestController in CTS
 *
 *	This socket server in parity-cts receives and responds to MarketCreateTransaction
 * message and replies with a MarketCreatedTransaction message.
 */

public class CtsSocketServer	{
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
    ArrayBlockingQueue<MarketCreateTenderPayload> socketServerCreateTenderQ = new ArrayBlockingQueue<>(100);

    public void start() {
    	// did have parameter port; constant for this class in Parity client
    	System.err.println("CtsSocketServer: start head; port: " + port);
    	
        try {
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            while (true)	{
                jsonReceived = in.readLine(); // sent by LME with ClientCreateTenderPayload
                System.err.println("CtsSocketServer: start: jsonReceived is '" + jsonReceived);
                payload = mapper.readValue(jsonReceived, MarketCreateTenderPayload.class);
                System.err.println("payload received object: " + payload.toString());
                // and add to the CtsBridge queue for processing
                bridge.createTenderQ.add(payload);
    		}
             
        } catch (IOException  e) {       	
            //	LOG.debug(e.getMessage());
        	//	ignore
        }
    }

    public void stop() {
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
    	System.err.println("CtsSocketServer: constructor Port: " + port);
    	
    	CtsSocketServer server = new CtsSocketServer();
    	
    	// TODO Lambda Expression for separate thread - alt implements runnable, place in thread//
    	
        server.start();
    }
    
    public CtsSocketServer(int port, CtsBridge bridge)	{
    	System.err.println("CtsSocketServer: constructor Port: " + port);
    	//	this.socketServerCreateTenderQ = bridgeQ;
    	this.bridge = bridge;
    	if (bridge == null)	{
    		System.err.println("CtsSocketServer: constructor:this.bridge is null");
    	}
    	
    	CtsSocketServer server = new CtsSocketServer();
    	
    	// TODO Lambda Expression for separate thread - alt implements runnable, place in thread//
    	
        server.start();
    }
    
}
