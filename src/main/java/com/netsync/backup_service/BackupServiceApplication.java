package com.netsync.backup_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.netsync.backup_service")
public class BackupServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackupServiceApplication.class, args);
	}

}
