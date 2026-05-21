package com.koins.loanbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KoinsLoanBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KoinsLoanBackendApplication.class, args);
    }
}
