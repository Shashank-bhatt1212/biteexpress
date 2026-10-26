package com.apextech.biteexpress;

import org.springframework.boot.SpringApplication;

public class TestBiteexpressApplication {

	public static void main(String[] args) {
		SpringApplication.from(BiteexpressApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
