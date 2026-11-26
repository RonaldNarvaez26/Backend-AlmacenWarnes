package com.almacen.warnes.modulo_seguridad.service;

import com.almacen.warnes.modulo_seguridad.dto.*;
import com.almacen.warnes.modulo_seguridad.jwt.BlockedUsersRegistry;
import com.almacen.warnes.modulo_seguridad.jwt.JwtService;
import com.almacen.warnes.modulo_seguridad.mapper.UsuarioMapper;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final BlockedUsersRegistry blockedUsersRegistry;
    private final com.almacen.warnes.modulo_seguridad.repository.SesionRepository sesionRepository;

    @Value("${app.setup.secret-key:WARNES-SETUP-2026}")
    private String setupSecretKey;

    // =========================================================================
    // 1. GESTION DE USUARIOS (CRUD Y PROCEDURES)
    // =========================================================================

    /**
     * Lista usuarios invocando la función PostgreSQL fn_usuarios_listar
     */
    public List<UsuarioListadoDTO> listarConFiltros(Boolean mostrarBajas, Integer rolId, Integer sucursalId,
            String textoBusqueda) {
        String sql = "SELECT * FROM fn_usuarios_listar(CAST(? AS BOOLEAN), CAST(? AS INTEGER), CAST(? AS INTEGER), CAST(? AS VARCHAR))";
        return jdbcTemplate.query(sql, (rs, rowNum) -> UsuarioListadoDTO.builder()
                .usuarioId(rs.getInt("usuario_id"))
                .sucursalId(rs.getObject("sucursal_id") != null ? rs.getInt("sucursal_id") : null)
                .usuario(rs.getString("usuario"))
                .nombres(rs.getString("nombres"))
                .apellidos(rs.getString("apellidos"))
                .rolId(rs.getInt("rol_id"))
                .descripcionRol(rs.getString("descripcion_rol"))
                .estado(rs.getObject("estado") != null ? rs.getInt("estado") : 1)
                .fechaCreacion(rs.getTimestamp("fecha_creacion") != null
                        ? rs.getTimestamp("fecha_creacion").toLocalDateTime()
                        : null)
                .build(),
                mostrarBajas, rolId, sucursalId, textoBusqueda);
    }

    public List<UsuarioDTO> listarUsuariosActivos() {
        return usuarioRepository.findByMarcaBajaFalse().stream()
                .map(usuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    public UsuarioDTO obtenerPorId(Integer id) {
        return usuarioRepository.findById(id)
                .filter(u -> !u.getMarcaBaja())
                .map(usuarioMapper::toDTO)
                .orElse(null);
    }

    /**
     * Registra un nuevo usuario llamando al procedimiento almacenado
     * sp_usuarios_insertar
     * Cifra la contraseña con BCrypt antes de enviarla
     */
    @Transactional
    public Integer registrarUsuario(UsuarioRegistroDTO dto) {
        if (usuarioRepository.existsByUsuarioAndMarcaBajaFalse(dto.getUsuario())) {
            throw new IllegalArgumentException("El nombre de usuario '" + dto.getUsuario() + "' ya está registrado.");
        }

        String passwordHash = passwordEncoder.encode(dto.getPassword());

        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("CALL sp_usuarios_insertar(?, ?, ?, ?, ?, ?, ?, ?)")) {
                cs.setString(1, dto.getUsuario());
                if (dto.getSucursalId() != null) {
                    cs.setInt(2, dto.getSucursalId());
                } else {
                    cs.setNull(2, Types.INTEGER);
                }
                cs.setString(3, dto.getNombres());
                cs.setString(4, dto.getApellidos());
                cs.setInt(5, dto.getRolId());
                cs.setString(6, passwordHash);
                cs.setInt(7, dto.getUsuarioCreacion());
                cs.registerOutParameter(8, Types.INTEGER);
                cs.execute();
                return cs.getInt(8);
            }
        });
    }

    /**
     * Modifica los datos del usuario invocando sp_usuarios_modificar
     */
    @Transactional
    public void modificarUsuario(Integer usuarioId, UsuarioModificarDTO dto) {
        jdbcTemplate.update("CALL sp_usuarios_modificar(?, ?, ?, ?, ?, ?)",
                usuarioId,
                dto.getSucursalId(),
                dto.getNombres(),
                dto.getApellidos(),
                dto.getRolId(),
                dto.getUsuarioModificacion());
    }

    /**
     * Realiza borrado lógico invocando sp_usuarios_eliminar
     */
    @Transactional
    public void eliminarUsuario(Integer usuarioId, Integer usuarioModificacion) {
        if (usuarioModificacion != null && usuarioId.equals(usuarioModificacion)) {
            throw new IllegalArgumentException("No puedes dar de baja tu propia cuenta en sesión.");
        }
        jdbcTemplate.update("CALL sp_usuarios_eliminar(?, ?)", usuarioId, usuarioModificacion);
        blockedUsersRegistry.markBlocked(usuarioId);
    }

    /**
     * Bloquea (3) o activa/desbloquea (1) una cuenta de usuario invocando sp_usuarios_cambiar_estado.
     * Si se bloquea, cierra cualquier sesión activa abierta del usuario y actualiza la caché en memoria.
     */
    @Transactional
    public void cambiarEstadoUsuario(Integer usuarioId, Integer nuevoEstado, Integer operadorId) {
        if (nuevoEstado == null || (nuevoEstado != 1 && nuevoEstado != 3)) {
            throw new IllegalArgumentException("Estado no válido. Solo se permite Activo (1) o Bloqueado (3).");
        }
        if (operadorId == null) {
            throw new IllegalArgumentException("Se requiere identificar al administrador que realiza la operación.");
        }
        if (usuarioId.equals(operadorId)) {
            throw new IllegalArgumentException("No puedes bloquear ni modificar el estado de tu propia cuenta en sesión.");
        }

        jdbcTemplate.update("CALL sp_usuarios_cambiar_estado(?, ?, ?)", usuarioId, nuevoEstado, operadorId);

        if (nuevoEstado == 3) {
            blockedUsersRegistry.markBlocked(usuarioId);
            try {
                Optional<SesionDTO> sesionActiva = sesionRepository.obtenerSesionActiva(usuarioId);
                if (sesionActiva.isPresent()) {
                    sesionRepository.cerrarSesion(sesionActiva.get().getSesionId(), usuarioId);
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(UsuarioService.class)
                        .warn("No se pudo cerrar la sesión activa tras bloquear usuarioId {}: {}", usuarioId, e.getMessage());
            }
        } else {
            blockedUsersRegistry.markActive(usuarioId);
        }
    }

    // =========================================================================
    // 2. AUTENTICACION Y RECUPERACION DE CONTRASEÑA
    // =========================================================================

    /**
     * Realiza el login validando con BCrypt y generando JWT
     * Retorna el token y el usuario_id necesario para consultas posteriores
     */
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByUsuarioAndMarcaBajaFalse(dto.getUsuario())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        if (usuario.getEstado() != null && usuario.getEstado() == 3) {
            throw new LockedException("Tu cuenta se encuentra bloqueada. Contacta con un propietario o administrador del sistema.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        String token = jwtService.generarToken(usuario.getUsuarioId(), usuario.getUsuario(), usuario.getRolId());

        // Invocación a sp_sesiones_abrir: abre nueva sesión o retoma la sesión activa previa sin duplicar
        Integer sesionId = null;
        LocalDateTime horaSesionAbierta = null;
        try {
            sesionId = sesionRepository.abrirSesion(usuario.getUsuarioId());
            horaSesionAbierta = sesionRepository.obtenerSesionActiva(usuario.getUsuarioId())
                    .map(SesionDTO::getHoraSesionAbierta)
                    .orElse(LocalDateTime.now());
        } catch (Exception e) {
            // Loguear error pero no truncar el login si ocurre una eventualidad en sesiones
            org.slf4j.LoggerFactory.getLogger(UsuarioService.class)
                    .error("Error al registrar sesión para usuarioId {}: {}", usuario.getUsuarioId(), e.getMessage());
        }

        return LoginResponseDTO.builder()
                .token(token)
                .usuarioId(usuario.getUsuarioId())
                .sucursalId(usuario.getSucursalId())
                .usuario(usuario.getUsuario())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .rolId(usuario.getRolId())
                .estado(usuario.getEstado())
                .sesionId(sesionId)
                .horaSesionAbierta(horaSesionAbierta)
                .mensaje("Inicio de sesión exitoso")
                .build();
    }

    /**
     * Solicita la recuperación de contraseña invocando el procedimiento almacenado
     * sp_usuarios_solicitar_recuperacion
     * Cambia el estado a 2 ('Solicita Recuperación')
     */
    @Transactional
    public void solicitarRecuperacion(String usuario) {
        Usuario u = usuarioRepository.findByUsuarioAndMarcaBajaFalse(usuario)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún usuario activo con el nombre: " + usuario));

        if (u.getEstado() != null && u.getEstado() == 3) {
            throw new IllegalStateException("Tu cuenta se encuentra bloqueada. Contacta con un propietario o administrador del sistema.");
        }

        jdbcTemplate.update("CALL sp_usuarios_solicitar_recuperacion(?)", usuario);
    }

    /**
     * Verifica si el sistema requiere configuración inicial (no existe ningún Propietario activo en BD).
     */
    public Map<String, Object> obtenerEstadoInicial() {
        long totalPropietarios = usuarioRepository.countByRolIdAndMarcaBajaFalse(3);
        return Map.of(
                "requiereConfiguracionInicial", totalPropietarios == 0,
                "requiereClaveInstalacion", true
        );
    }

    /**
     * Registra al primer Propietario del sistema cuando la tabla Usuarios no tiene ningún Propietario.
     * Exige la clave secreta de instalación configurada en app.setup.secret-key y se auto-desactiva tras el primer registro.
     */
    @Transactional
    public synchronized Integer registrarPrimerAdmin(PrimerAdminRegistroDTO dto) {
        if (usuarioRepository.countByRolIdAndMarcaBajaFalse(3) > 0) {
            throw new IllegalStateException("El sistema ya cuenta con un Propietario configurado. El registro inicial ha sido deshabilitado.");
        }

        String claveConfigurada = setupSecretKey != null ? setupSecretKey.trim() : "";
        String claveRecibida = dto.getClaveSecreta() != null ? dto.getClaveSecreta().trim() : "";

        if (claveConfigurada.isEmpty() || !MessageDigest.isEqual(
                claveConfigurada.getBytes(StandardCharsets.UTF_8),
                claveRecibida.getBytes(StandardCharsets.UTF_8))) {
            throw new BadCredentialsException("La clave secreta de instalación es incorrecta.");
        }

        String username = dto.getUsuario().trim();
        if (usuarioRepository.existsByUsuarioAndMarcaBajaFalse(username)) {
            throw new IllegalArgumentException("El nombre de usuario '" + username + "' ya está registrado.");
        }

        UsuarioRegistroDTO registro = UsuarioRegistroDTO.builder()
                .usuario(username)
                .sucursalId(0)
                .nombres(dto.getNombres().trim())
                .apellidos(dto.getApellidos().trim())
                .rolId(3)
                .password(dto.getPassword())
                .usuarioCreacion(1)
                .build();

        return registrarUsuario(registro);
    }

    /**
     * Permite al propietario restablecer la contraseña a un usuario (después de su
     * solicitud o directamente)
     * Cifra con BCrypt y restablece el estado a 1 ('Activo')
     */
    @Transactional
    public void recuperarContrasena(RecuperarContrasenaDTO dto) {
        Usuario usuario = usuarioRepository.findByUsuarioAndMarcaBajaFalse(dto.getUsuario())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún usuario activo con el nombre: " + dto.getUsuario()));

        usuario.setPassword(passwordEncoder.encode(dto.getNuevaPassword()));
        usuario.setEstado(1); // Restablecer a 1 ('Activo')
        usuario.setFechaModificacion(LocalDateTime.now());
        if (dto.getUsuarioModificacion() != null) {
            usuario.setUsuarioModificacion(dto.getUsuarioModificacion());
        }
        usuarioRepository.save(usuario);
        blockedUsersRegistry.markActive(usuario.getUsuarioId());
    }

    /**
     * Permite al usuario logueado cambiar su contraseña verificando la anterior
     */
    @Transactional
    public void cambiarContrasena(CambiarContrasenaDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .filter(u -> !u.getMarcaBaja())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPassword())) {
            throw new BadCredentialsException("La contraseña actual no coincide");
        }

        usuario.setPassword(passwordEncoder.encode(dto.getNuevaPassword()));
        usuario.setEstado(1); // Asegurar que quede en 1 ('Activo')
        usuario.setFechaModificacion(LocalDateTime.now());
        usuario.setUsuarioModificacion(dto.getUsuarioId());
        usuarioRepository.save(usuario);
        blockedUsersRegistry.markActive(usuario.getUsuarioId());
    }
}