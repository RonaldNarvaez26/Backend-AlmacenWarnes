package com.almacen.warnes.modulo_notificaciones.service;

import com.almacen.warnes.modulo_notificaciones.dto.*;
import com.almacen.warnes.modulo_notificaciones.model.Notificacion;
import com.almacen.warnes.modulo_notificaciones.repository.NotificacionProcedureRepository;
import com.almacen.warnes.modulo_notificaciones.repository.NotificacionRepository;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionProcedureRepository notificacionProcedureRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Obtiene el listado consolidado de notificaciones activas para el usuario en sesión
     * y computa los totales por severidad.
     */
    public NotificacionResponseDTO obtenerNotificaciones(Integer sucursalIdParam, Integer usuarioIdParam, String authHeader) {
        Optional<Usuario> usuarioOpt = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader);

        Integer usuarioId = 1;
        Integer rolId = 3;
        Integer sucursalId = sucursalIdParam;

        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            usuarioId = u.getUsuarioId();
            rolId = u.getRolId() != null ? u.getRolId() : 3;

            if (rolId == 1 || rolId == 2) {
                sucursalId = (u.getSucursalId() != null) ? u.getSucursalId() : 1;
            } else if (rolId == 3) {
                sucursalId = (sucursalIdParam != null && sucursalIdParam > 0) ? sucursalIdParam : null;
            }
        }

        log.info("Listando notificaciones para usuarioId: {}, rolId: {}, sucursalId: {}", usuarioId, rolId, sucursalId);
        List<NotificacionItemDTO> notificaciones = notificacionProcedureRepository.listarNotificaciones(usuarioId, rolId, sucursalId);

        long criticas = notificaciones.stream().filter(n -> "CRITICA".equalsIgnoreCase(n.getSeveridad())).count();
        long altas = notificaciones.stream().filter(n -> "ALTA".equalsIgnoreCase(n.getSeveridad())).count();
        long advertencias = notificaciones.stream().filter(n -> "ADVERTENCIA".equalsIgnoreCase(n.getSeveridad())).count();
        long informativas = notificaciones.stream().filter(n -> "INFORMATIVA".equalsIgnoreCase(n.getSeveridad())).count();

        return NotificacionResponseDTO.builder()
                .notificaciones(notificaciones)
                .totalCriticas(criticas)
                .totalAltas(altas)
                .totalAdvertencias(advertencias)
                .totalInformativas(informativas)
                .totalGeneral(notificaciones.size())
                .build();
    }

    /**
     * Marca una notificación específica como leída en la base de datos
     */
    @Transactional
    public Map<String, Object> marcarLeido(String idNotificacionStr, Integer usuarioIdParam, String authHeader) {
        Optional<Usuario> usuarioOpt = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader);
        Integer usuarioId = usuarioOpt.map(Usuario::getUsuarioId).orElse(usuarioIdParam);

        Map<String, Object> respuesta = new HashMap<>();

        try {
            Integer idNotificacion = Integer.parseInt(idNotificacionStr);
            boolean resultado = notificacionProcedureRepository.marcarNotificacionLeida(idNotificacion, usuarioId);

            respuesta.put("exito", resultado);
            respuesta.put("idNotificacion", idNotificacion);
            respuesta.put("mensaje", resultado ? "Notificación marcada como leída exitosamente." : "No se encontró la notificación o ya estaba leída.");
            return respuesta;
        } catch (NumberFormatException e) {
            // Notificación dinámica (e.g. STOCK_1_2 o DASH_REQ_2) que no reside físicamente en la tabla Notificaciones
            log.info("Notificación con identificador dinámico {}. Marcada como atendida a nivel de sesión.", idNotificacionStr);
            respuesta.put("exito", true);
            respuesta.put("idNotificacion", idNotificacionStr);
            respuesta.put("mensaje", "Alerta dinámica atendida.");
            return respuesta;
        }
    }

    /**
     * Marca todas las notificaciones pendientes del usuario o su rol como leídas
     */
    @Transactional
    public Map<String, Object> marcarTodasLeidas(Integer usuarioIdParam, String authHeader) {
        Optional<Usuario> usuarioOpt = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader);

        Integer usuarioId = 1;
        Integer rolId = 3;

        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            usuarioId = u.getUsuarioId();
            rolId = u.getRolId() != null ? u.getRolId() : 3;
        } else if (usuarioIdParam != null) {
            usuarioId = usuarioIdParam;
        }

        int afectadas = notificacionProcedureRepository.marcarTodasNotificacionesLeidas(usuarioId, rolId);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("exito", true);
        respuesta.put("totalAfectadas", afectadas);
        respuesta.put("mensaje", "Se han marcado " + afectadas + " notificaciones como leídas.");
        return respuesta;
    }

    /**
     * Registra una nueva notificación física en la tabla Notificaciones
     */
    @Transactional
    public Notificacion crearNotificacion(CrearNotificacionRequestDTO request, Integer usuarioIdParam, String authHeader) {
        Notificacion entidad = Notificacion.builder()
                .usuarioDestino(request.getUsuarioDestino())
                .rolDestino(request.getRolDestino())
                .tipoNotificacion(request.getTipoNotificacion())
                .tituloAlerta(request.getTituloAlerta())
                .mensajeAlerta(request.getMensajeAlerta())
                .direccion(request.getDireccion())
                .leido(false)
                .fechaEmision(LocalDateTime.now())
                .build();

        return notificacionRepository.save(entidad);
    }

    /**
     * Verifica en tiempo real la disponibilidad de stock previo a una venta
     */
    public VerificacionStockVentaDTO verificarStockVenta(
            Integer sucursalIdParam,
            Integer productoId,
            BigDecimal cantidadSolicitada,
            Integer usuarioIdParam,
            String authHeader
    ) {
        Integer sucursalEfectiva = securityContextHelper.resolverSucursalFiltro(sucursalIdParam, usuarioIdParam, authHeader);
        if (sucursalEfectiva == null) {
            sucursalEfectiva = 1;
        }

        return notificacionProcedureRepository.verificarStockVenta(sucursalEfectiva, productoId, cantidadSolicitada);
    }

    /**
     * Permite a un Supervisor solicitar acceso al Dashboard
     */
    public DashboardPermisoDTO solicitarAccesoDashboard(Integer usuarioIdParam, String authHeader) {
        Optional<Usuario> usuarioOpt = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader);
        if (usuarioOpt.isEmpty()) {
            return DashboardPermisoDTO.builder()
                    .supervisorId(usuarioIdParam)
                    .tieneAcceso(false)
                    .estado("ERROR")
                    .mensaje("No se pudo identificar la sesión del usuario.")
                    .build();
        }

        Usuario usuario = usuarioOpt.get();
        if (usuario.getRolId() == null || usuario.getRolId() != 2) {
            return DashboardPermisoDTO.builder()
                    .supervisorId(usuario.getUsuarioId())
                    .tieneAcceso(usuario.getRolId() != null && usuario.getRolId() == 3)
                    .estado("DENEGADO")
                    .mensaje("Solo los usuarios con rol Supervisor pueden solicitar acceso al Dashboard.")
                    .build();
        }

        Map<String, String> resultadoSp = notificacionProcedureRepository.solicitarDashboard(usuario.getUsuarioId());
        String estadoSp = resultadoSp.get("estado");
        String mensajeSp = resultadoSp.get("mensaje");

        String estadoActual = notificacionProcedureRepository.obtenerEstadoDashboardSupervisor(usuario.getUsuarioId());
        boolean tieneAcceso = "APROBADO".equalsIgnoreCase(estadoActual);

        return DashboardPermisoDTO.builder()
                .supervisorId(usuario.getUsuarioId())
                .tieneAcceso(tieneAcceso)
                .estado(estadoActual)
                .mensaje(mensajeSp != null ? mensajeSp : estadoSp)
                .build();
    }

    /**
     * Permite al Propietario aprobar o rechazar una solicitud de acceso al Dashboard
     */
    public DashboardPermisoDTO resolverAccesoDashboard(
            Integer supervisorId,
            Boolean autorizar,
            Integer usuarioIdParam,
            String authHeader
    ) {
        Optional<Usuario> usuarioOpt = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader);
        if (usuarioOpt.isEmpty()) {
            return DashboardPermisoDTO.builder()
                    .supervisorId(supervisorId)
                    .tieneAcceso(false)
                    .estado("ERROR")
                    .mensaje("No se pudo identificar la sesión del usuario Propietario.")
                    .build();
        }

        Usuario propietario = usuarioOpt.get();
        if (propietario.getRolId() == null || propietario.getRolId() != 3) {
            return DashboardPermisoDTO.builder()
                    .supervisorId(supervisorId)
                    .tieneAcceso(false)
                    .estado("ERROR")
                    .mensaje("Acceso denegado: Solo el Propietario puede autorizar o revocar accesos al Dashboard.")
                    .build();
        }

        Map<String, String> resultadoSp = notificacionProcedureRepository.resolverDashboard(
                supervisorId,
                autorizar,
                propietario.getUsuarioId()
        );

        String estadoSp = resultadoSp.get("estado");
        String mensajeSp = resultadoSp.get("mensaje");

        String estadoActual = notificacionProcedureRepository.obtenerEstadoDashboardSupervisor(supervisorId);
        boolean tieneAcceso = "APROBADO".equalsIgnoreCase(estadoActual);

        return DashboardPermisoDTO.builder()
                .supervisorId(supervisorId)
                .tieneAcceso(tieneAcceso)
                .estado(estadoActual)
                .mensaje(mensajeSp != null ? mensajeSp : estadoSp)
                .build();
    }

    /**
     * Consulta el estado de permiso de Dashboard para un usuario o supervisor
     */
    public DashboardPermisoDTO consultarEstadoDashboard(
            Integer supervisorIdParam,
            Integer usuarioIdParam,
            String authHeader
    ) {
        Optional<Usuario> usuarioOpt = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader);

        Integer targetUserId = supervisorIdParam;
        if (targetUserId == null && usuarioOpt.isPresent()) {
            targetUserId = usuarioOpt.get().getUsuarioId();
        }

        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            // Propietario siempre tiene acceso irrestricto al Dashboard
            if (u.getRolId() != null && u.getRolId() == 3) {
                if (supervisorIdParam == null || supervisorIdParam.equals(u.getUsuarioId())) {
                    return DashboardPermisoDTO.builder()
                            .supervisorId(u.getUsuarioId())
                            .tieneAcceso(true)
                            .estado("APROBADO")
                            .mensaje("Acceso irrestricto de Propietario.")
                            .build();
                }
            } else if (u.getRolId() != null && u.getRolId() == 1) {
                // Cajero denegado
                return DashboardPermisoDTO.builder()
                        .supervisorId(u.getUsuarioId())
                        .tieneAcceso(false)
                        .estado("DENEGADO")
                        .mensaje("Los usuarios con rol Cajero no cuentan con acceso al Dashboard.")
                        .build();
            }
        }

        if (targetUserId == null) {
            return DashboardPermisoDTO.builder()
                    .supervisorId(null)
                    .tieneAcceso(false)
                    .estado("NINGUNO")
                    .mensaje("No se ha especificado el usuario supervisor.")
                    .build();
        }

        String estadoActual = notificacionProcedureRepository.obtenerEstadoDashboardSupervisor(targetUserId);
        boolean tieneAcceso = "APROBADO".equalsIgnoreCase(estadoActual);

        String mensaje;
        switch (estadoActual) {
            case "APROBADO":
                mensaje = "Acceso concedido al Dashboard.";
                break;
            case "PENDIENTE":
                mensaje = "Solicitud pendiente de aprobación por el Propietario.";
                break;
            case "RECHAZADO":
                mensaje = "La solicitud de acceso al Dashboard fue rechazada por el Propietario.";
                break;
            default:
                mensaje = "No existe ninguna solicitud de acceso registrada.";
                break;
        }

        return DashboardPermisoDTO.builder()
                .supervisorId(targetUserId)
                .tieneAcceso(tieneAcceso)
                .estado(estadoActual)
                .mensaje(mensaje)
                .build();
    }

    /**
     * Consulta la configuración actual de días de anticipación de vencimiento por categoría de producto
     */
    public List<ConfigVencimientoCategoriaDTO> listarConfiguracionVencimiento() {
        return notificacionProcedureRepository.listarConfiguracionVencimiento();
    }

    /**
     * Actualiza globalmente los días de anticipación para todas las categorías
     */
    @Transactional
    public Map<String, Object> actualizarVencimientoGlobal(int diasAnticipacion, Integer usuarioIdParam, String authHeader) {
        Optional<Usuario> usuarioOpt = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader);
        Integer usuarioId = usuarioOpt.map(Usuario::getUsuarioId).orElse(usuarioIdParam != null ? usuarioIdParam : 1);

        log.info("Actualizando vencimiento global a {} días por usuarioId: {}", diasAnticipacion, usuarioId);
        return notificacionProcedureRepository.actualizarVencimientoGlobal(diasAnticipacion, usuarioId);
    }

    /**
     * Actualiza individualmente los días de anticipación para una o múltiples categorías seleccionadas
     */
    @Transactional
    public Map<String, Object> actualizarVencimientoIndividual(
            ActualizarVencimientoIndividualRequestDTO request,
            Integer usuarioIdParam,
            String authHeader
    ) {
        Optional<Usuario> usuarioOpt = securityContextHelper.obtenerUsuarioSesion(usuarioIdParam, authHeader);
        Integer usuarioId = usuarioOpt.map(Usuario::getUsuarioId).orElse(usuarioIdParam != null ? usuarioIdParam : 1);

        Map<String, Object> respuesta = new HashMap<>();

        if (request.getCategorias() != null && !request.getCategorias().isEmpty()) {
            int actualizadas = 0;
            for (ItemCategoriaDiasDTO item : request.getCategorias()) {
                if (item.getCategoriaId() != null && item.getDiasAnticipacion() != null) {
                    notificacionProcedureRepository.actualizarVencimientoIndividual(
                            item.getCategoriaId(), item.getDiasAnticipacion(), usuarioId);
                    actualizadas++;
                }
            }
            respuesta.put("exito", true);
            respuesta.put("afectados", actualizadas);
            respuesta.put("mensaje", "Se actualizaron satisfactoriamente " + actualizadas + " categorías.");
            return respuesta;
        }

        if (request.getCategoriaId() != null && request.getDiasAnticipacion() != null) {
            Map<String, Object> res = notificacionProcedureRepository.actualizarVencimientoIndividual(
                    request.getCategoriaId(), request.getDiasAnticipacion(), usuarioId);
            respuesta.putAll(res);
            respuesta.put("exito", Boolean.TRUE.equals(res.get("resultado")));
            return respuesta;
        }

        throw new IllegalArgumentException("Debe proporcionar al menos 'categoriaId' y 'diasAnticipacion' o una lista de 'categorias'.");
    }
}
