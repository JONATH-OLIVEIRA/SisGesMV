package br.com.sisgesmv.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

// Importações do JJWT (Json Web Token)
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long expiration;
    private final JwtParser parser;

    public JwtTokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {

        // Validação da chave
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("JWT secret não pode estar vazio");
        }

        // Decodifica a chave Base64 e garante o tamanho mínimo para HS384
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 48) { // HS384 requer 384 bits (48 bytes)
            throw new IllegalArgumentException("Chave secreta muito curta para HS384. Mínimo de 48 bytes (384 bits) requerido.");
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration > 0 ? expiration : 86400000L; // 24h padrão

        this.parser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
    }

    public String gerarToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    public boolean validarToken(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("ERRO na validação: " + e.getMessage());
            return false;
        }
    }

    public Claims extrairClaims(String token) {
        try {
            return parser.parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Token inválido ou expirado", e);
        }
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public String extrairRole(String token) {
        return extrairClaims(token).get("role", String.class);
    }
}