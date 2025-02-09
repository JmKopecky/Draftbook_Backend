package dev.jkopecky.draftbook_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DraftbookBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DraftbookBackendApplication.class, args);
    }

    public static String retrieveRoot() {
        return System.getProperty("user.home") + "draftbook_data/";
    }
}
