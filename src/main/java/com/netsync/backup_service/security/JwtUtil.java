package com.netsync.backup_service.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET_KEY = "TrocheEnTricicloRobada234i239QueVerguenzaTroche";
    private static final long EXPIRATION_TIME = 600000; // 10 minutos

    public static String generateToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }
}
