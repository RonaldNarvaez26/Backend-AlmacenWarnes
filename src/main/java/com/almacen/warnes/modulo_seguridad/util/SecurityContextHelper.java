package com.almacen.warnes.modulo_seguridad.util;

import com.almacen.warnes.modulo_seguridad.jwt.JwtService;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityContextHelper {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    /**
     * Resuelve el usuario autenticado a partir de:
     * 1. Header Authorization Bearer <token>
     * 2. Parámetro explícito usuarioId
     */
    public Optional<Usuario> obtenerUsuarioSesion(Integer usuarioIdParam, String authHeader) {
        if (usuarioIdParam != null) {
            Optional<Usuario> u = usuarioRepository.findById(usuarioIdParam);
            if (u.isPresent()) return u;
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7).trim();
                Integer uid = jwtService.extraerUsuarioId(token);
                if (uid != null) {
                    return usuarioRepository.findById(uid);
                }
            } catch (Exception e) {
                log.warn("Error al extraer usuarioId del token JWT: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    /**
     * Resuelve la sucursal permitida para consultas (GET):
     * - Cajero (rol_id = 1) o Supervisor (rol_id = 2): Ignora cualquier sucursalId externa y fuerza su propia sucursal asignada.
     * - Propietario (rol_id = 3): Respeta la sucursal enviada (null para todas, o un ID específico).
     * - Si no se detecta sesión: Respeta el sucursalIdParam.
     */
    public Integer resolverSucursalFiltro(Integer sucursalIdParam, Integer usuarioIdParam, String authHeader) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioSesion(usuarioIdParam, authHeader);
        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            Integer rol = u.getRolId();
            if (rol != null && (rol == 1 || rol == 2)) {
                Integer sucUsuario = u.getSucursalId() != null ? u.getSucursalId() : 1;
                log.info("RBAC: Usuario '{}' (Rol: {}) restringido estrictamente a sucursalId: {}",
                        u.getUsuario(), rol, sucUsuario);
                return sucUsuario;
            }
            if (rol != null && rol == 3) {
                log.info("RBAC: Propietario '{}' consultando con filtro sucursalId: {}", u.getUsuario(), sucursalIdParam);
                return sucursalIdParam;
            }
        }
        return sucursalIdParam;
    }

    /**
     * Resuelve la sucursal para operaciones de registro/mutación (POST/PUT):
     * - Cajero o Supervisor: Siempre su propia sucursal asignada.
     * - Propietario: La sucursal enviada en el formulario, o si es null, la sucursal central (1).
     */
    public Integer resolverSucursalOperacion(Integer sucursalIdParam, Integer usuarioIdParam, String authHeader) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioSesion(usuarioIdParam, authHeader);
        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            Integer rol = u.getRolId();
            if (rol != null && (rol == 1 || rol == 2)) {
                return u.getSucursalId() != null ? u.getSucursalId() : 1;
            }
            if (rol != null && rol == 3) {
                if (sucursalIdParam != null) return sucursalIdParam;
                return u.getSucursalId() != null ? u.getSucursalId() : 1;
            }
        }
        return sucursalIdParam != null ? sucursalIdParam : 1;
    }

    /**
     * Verifica si el usuario en sesión tiene permisos de escritura (Supervisor o Propietario).
     */
    public boolean tienePermisoEscritura(Integer usuarioIdParam, String authHeader) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioSesion(usuarioIdParam, authHeader);
        if (usuarioOpt.isPresent()) {
            Integer rol = usuarioOpt.get().getRolId();
            return rol != null && (rol == 2 || rol == 3);
        }
        return true; // Si no hay contexto de sesión, no bloquear a nivel de backend legado
    }
}

