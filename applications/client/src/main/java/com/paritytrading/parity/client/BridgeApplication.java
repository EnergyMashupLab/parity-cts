package com.paritytrading.parity.client;

import static org.jvirtanen.util.Applications.config;
import static org.jvirtanen.util.Applications.error;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
//	import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.typesafe.config.ConfigException;

@SpringBootApplication
public class BridgeApplication {

	public static void main(String[] args) {
		
		SpringApplication.run(BridgeApplication.class, args);
		
		System.err.println("BridgeApplication after SpringApplication.run()");
		
		// and start the Terminal Client application
		try	{
			TerminalClient.deferredMain(args);
			
		}	catch (EndOfFileException | UserInterruptException e) {
			   // Ignore.
		}	catch (ConfigException | FileNotFoundException e) {
		    error(e);
		}	catch	(IOException e) 	{
			// ignore
		}
	}
}
