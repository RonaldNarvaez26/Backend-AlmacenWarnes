package com.almacen.warnes.modulo_seguridad.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro HTTP ultraligero que verifica en memoria si el usuario del JWT fue bloqueado o dado de baja.
 * Se ejecuta después de CorsFilter en SecurityConfig sin realizar consultas SQL por petición.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtBlockedUserFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final BlockedUsersRegistry blockedUsersRegistry;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/seguridad/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7).trim();
                if (!token.isEmpty()) {
                    Integer uid = jwtService.extraerUsuarioId(token);
                    if (blockedUsersRegistry.isBlocked(uid)) {
                        log.warn("Acceso rechazado en tiempo real para usuario bloqueado/inactivo ID: {} en {}", uid, path);
                        response.setStatus(423);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(
                                "{\"codigo\":\"CUENTA_BLOQUEADA\",\"error\":\"Tu cuenta ha sido bloqueada o deshabilitada. Contacta con un propietario o administrador.\"}"
                        );
                        return;
                    }
                }
            } catch (Exception ignored) {
                // Tokens inválidos o expirados continúan el flujo normal de la aplicación
            }
        }

        filterChain.doFilter(request, response);
    }
}
