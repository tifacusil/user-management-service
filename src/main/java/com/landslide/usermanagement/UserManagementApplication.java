package com.landslide.usermanagement;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.landslide.usermanagement.mapper")
public class UserManagementApplication {

    public static void main(String[] args) {
        // Load .env file and inject into system properties before Spring starts
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(e -> {
            if (System.getProperty(e.getKey()) == null) {
                System.setProperty(e.getKey(), e.getValue());
            }
        });

        SpringApplication.run(UserManagementApplication.class, args);
    }
}
