package com.netsync.backup_service.service;

import com.netsync.backup_service.security.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "admin123";

    public String authenticate(String username, String password) {
        if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
            return JwtUtil.generateToken(username);
        }
        return null; // Usuario no válido
    }
}
