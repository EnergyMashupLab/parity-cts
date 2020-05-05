package com.paritytrading.parity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@ComponentScan("com.paritytrading.parity")
@SpringBootApplication
public class BridgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(BridgeApplication.class, args);
		
		TerminalClient.main(String[] args);
	}

}
