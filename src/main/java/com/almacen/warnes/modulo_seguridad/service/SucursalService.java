package com.almacen.warnes.modulo_seguridad.service;

import com.almacen.warnes.modulo_seguridad.dto.CrearSucursalDTO;
import com.almacen.warnes.modulo_seguridad.dto.ModificarSucursalDTO;
import com.almacen.warnes.modulo_seguridad.dto.SucursalDTO;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.repository.SucursalProcedureRepository;
import com.almacen.warnes.modulo_seguridad.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SucursalService {

    private final SucursalProcedureRepository sucursalProcedureRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Lista sucursales con filtros dinámicos invocando fn_sucursales_listar
     */
    public List<SucursalDTO> listarSucursales(Boolean mostrarBajas, String textoBusqueda) {
        log.info("Consultando sucursales -> mostrarBajas: {}, busqueda: '{}'", mostrarBajas, textoBusqueda);
        return sucursalProcedureRepository.listarSucursales(mostrarBajas, textoBusqueda);
    }

    /**
     * Registra una nueva sucursal física.
     * Solo permitido para el rol de Propietario (rol_id = 3).
     */
    @Transactional
    public Integer crearSucursal(CrearSucursalDTO dto, Integer usuarioIdParam, String authHeader) {
        Usuario usuario = validarPermisoPropietario(usuarioIdParam, authHeader, "crear sucursales");
        Integer usuarioId = usuario != null ? usuario.getUsuarioId() : (usuarioIdParam != null ? usuarioIdParam : 1);

        log.info("Propietario '{}' registrando nueva sucursal con código '{}'",
                usuario != null ? usuario.getUsuario() : usuarioId, dto.getCodigoInterno());

        return sucursalProcedureRepository.insertarSucursal(dto, usuarioId);
    }

    /**
     * Modifica los datos de una sucursal existente.
     * Solo permitido para el rol de Propietario (rol_id = 3).
     */
    @Transactional
    public void modificarSucursal(Integer sucursalId, ModificarSucursalDTO dto, Integer usuarioIdParam, String authHeader) {
        Usuario usuario = validarPermisoPropietario(usuarioIdParam, authHeader, "modificar sucursales");
        Integer usuarioId = usuario != null ? usuario.getUsuarioId() : (usuarioIdParam != null ? usuarioIdParam : 1);

        log.info("Propietario '{}' modificando sucursal ID: {}",
                usuario != null ? usuario.getUsuario() : usuarioId, sucursalId);

        sucursalProcedureRepository.modificarSucursal(sucursalId, dto, usuarioId);
    }

    /**
     * Da de baja lógica a una sucursal física.
     * Solo permitido para el rol de Propietario (rol_id = 3).
     */
    @Transactional
    public void eliminarSucursal(Integer sucursalId, Integer usuarioIdParam, String authHeader) {
        Usuario usuario = validarPermisoPropietario(usuarioIdParam, authHeader, "dar de baja sucursales");
        Integer usuarioId = usuario != null ? usuario.getUsuarioId() : (usuarioIdParam != null ? usuarioIdParam : 1);

        log.info("Propietario '{}' dando de baja lógica a sucursal ID: {}",
                usuario != null ? usuario.getUsuario() : usuarioId, sucursalId);

        sucursalProcedureRepository.eliminarSucursal(sucursalId, usuarioId);
    }

    /**
     * Valida que el usuario en sesión cuente con el rol de Propietario (rol_id = 3).
     */
    private Usuario validarPermisoPropietario(Integer usuarioIdParam, String authHeader, String accion) {
        Usuario usuario = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader).orElse(null);

        if (usuario != null) {
            Integer rolId = usuario.getRolId();
            if (rolId == null || rolId != 3) {
                log.warn("RBAC Denegado: Usuario '{}' (Rol: {}) intentó {} sin privilegios de Propietario",
                        usuario.getUsuario(), rolId, accion);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acceso denegado: Solo el Propietario / Administrador general puede " + accion + ".");
            }
        }
        return usuario;
    }
}

