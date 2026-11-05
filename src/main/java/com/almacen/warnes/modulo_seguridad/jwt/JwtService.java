package com.almacen.warnes.modulo_seguridad.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24 horas por defecto
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generarToken(Integer usuarioId, String usuario, Integer rolId) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("usuarioId", usuarioId);
        extraClaims.put("rolId", rolId);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(usuario)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerUsuario(String token) {
        return extraerClaims(token).getSubject();
    }

    public Integer extraerUsuarioId(String token) {
        Object val = extraerClaims(token).get("usuarioId");
        return val != null ? ((Number) val).intValue() : null;
    }

    public Integer extraerRolId(String token) {
        Object val = extraerClaims(token).get("rolId");
        return val != null ? ((Number) val).intValue() : null;
    }

    public boolean esTokenValido(String token, String usuarioEsperado) {
        final String usuario = extraerUsuario(token);
        return (usuario.equals(usuarioEsperado) && !estaTokenExpirado(token));
    }

    private boolean estaTokenExpirado(String token) {
        return extraerClaims(token).getExpiration().before(new Date());
    }
}

