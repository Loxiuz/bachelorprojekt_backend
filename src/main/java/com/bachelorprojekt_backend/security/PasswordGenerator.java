package com.bachelorprojekt_backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/* for testing */
@Slf4j
public class PasswordGenerator {
    static void main(String[] args) {

        HashMap<String, String> users = new HashMap<>();
        users.put("user", "user");

        PasswordGenerator generator = new PasswordGenerator();
        int i = 2;
        for (Map.Entry<String, String> userEntry : users.entrySet()) {
            log.info(generator.printPasswordWithUsername(userEntry.getKey(), userEntry.getValue(), i++));
        }
    }

    String printPasswordWithUsername(String username, String password, int i) {
        // Helper output for SQL seed creation; not used by the authentication runtime.
        return "(" + (i) + ", '" + new BCryptPasswordEncoder().encode(password) + "', '" + username + "', UUID_TO_BIN('62d7d583-9809-4a1a-a803-a44e3156595b'), 2),";
    }
}
