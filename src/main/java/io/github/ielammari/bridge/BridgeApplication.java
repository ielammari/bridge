package io.github.ielammari.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BridgeApplication {

	private final static Logger log = LoggerFactory.getLogger(BridgeApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BridgeApplication.class, args);
		log.info("Welcome to Bridge");
	}

}
