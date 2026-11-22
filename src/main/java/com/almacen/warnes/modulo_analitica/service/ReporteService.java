package com.almacen.warnes.modulo_analitica.service;

import com.almacen.warnes.modulo_analitica.dto.*;
import com.almacen.warnes.modulo_analitica.repository.ReporteRepository;
import com.almacen.warnes.modulo_seguridad.dto.SesionDTO;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.repository.SesionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final SesionRepository sesionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Devuelve el resumen del turno activo para el cajero autenticado.
     */
    public Optional<ArqueoResumenTurnoDTO> obtenerResumenTurno(Integer usuarioId) {
        return reporteRepository.obtenerResumenTurno(usuarioId);
    }

    /**
     * Registra el arqueo de caja del turno, consolida movimientos en Opción B y cierra la sesión atómicamente.
     */
    @Transactional
    public Integer registrarArqueo(RegistrarArqueoDTO dto, Usuario usuarioAutenticado) {
        Integer usuarioId = usuarioAutenticado.getUsuarioId();
        Integer sucursalId = usuarioAutenticado.getSucursalId();

        if (sucursalId == null || sucursalId <= 0) {
            throw new IllegalStateException("El cajero no tiene una sucursal asignada válida.");
        }

        // 1. Determinar el sesionId
        Integer sesionId = dto.getSesionId();
        if (sesionId == null) {
            Optional<SesionDTO> sesionActiva = sesionRepository.obtenerSesionActiva(usuarioId);
            if (sesionActiva.isEmpty()) {
                throw new IllegalStateException("No existe una sesión o turno activo abierto para el usuario " + usuarioId);
            }
            sesionId = sesionActiva.get().getSesionId();
        }

        log.info("Iniciando registro de arqueo para sesionId: {}, usuarioId: {}, sucursalId: {}",
                sesionId, usuarioId, sucursalId);

        // 2. Serializar entradas a JSON con el formato requerido por el SP: [{"tipo_entrada": x, "monto": y, "descripcion": z}]
        List<Map<String, Object>> entradasList = new ArrayList<>();
        if (dto.getEntradas() != null) {
            for (PartidaArqueoDTO e : dto.getEntradas()) {
                Map<String, Object> map = new HashMap<>();
                map.put("tipo_entrada", e.getTipo());
                map.put("monto", e.getMonto());
                map.put("descripcion", e.getDescripcion());
                entradasList.add(map);
            }
        }

        // 3. Serializar salidas a JSON con el formato requerido por el SP: [{"tipo_salida": x, "monto": y, "descripcion": z}]
        List<Map<String, Object>> salidasList = new ArrayList<>();
        if (dto.getSalidas() != null) {
            for (PartidaArqueoDTO s : dto.getSalidas()) {
                Map<String, Object> map = new HashMap<>();
                map.put("tipo_salida", s.getTipo());
                map.put("monto", s.getMonto());
                map.put("descripcion", s.getDescripcion());
                salidasList.add(map);
            }
        }

        String entradasJson = toJson(entradasList);
        String salidasJson = toJson(salidasList);
        String snapshotJson = dto.getDatosSnapshot() != null ? toJson(dto.getDatosSnapshot()) : "{}";

        Integer nuevoReporteId = reporteRepository.registrarArqueo(
                sesionId,
                usuarioId,
                sucursalId,
                dto.getMontoBase(),
                snapshotJson,
                entradasJson,
                salidasJson,
                dto.getPeriodo() != null ? dto.getPeriodo() : "Turno"
        );

        log.info("Arqueo registrado exitosamente -> Reporte ID: {} para el usuarioId: {}", nuevoReporteId, usuarioId);
        return nuevoReporteId;
    }

    /**
     * Lista reportes con filtros y control de acceso por rol.
     */
    public List<ReporteListadoDTO> listarReportes(
            Integer sucursalIdParam,
            String periodo,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer usuarioIdParam,
            Usuario usuarioAutenticado
    ) {
        Integer sucursalEfectiva = sucursalIdParam;
        Integer usuarioEfectivo = usuarioIdParam;

        if (usuarioAutenticado != null) {
            if (usuarioAutenticado.getRolId() == 2) {
                // Supervisor: restringido a su sucursal
                sucursalEfectiva = usuarioAutenticado.getSucursalId();
            } else if (usuarioAutenticado.getRolId() == 1) {
                // Cajero: solo puede ver sus propios reportes de su sucursal
                sucursalEfectiva = usuarioAutenticado.getSucursalId();
                usuarioEfectivo = usuarioAutenticado.getUsuarioId();
            }
        }

        return reporteRepository.listarReportes(
                sucursalEfectiva,
                periodo,
                fechaDesde,
                fechaHasta,
                usuarioEfectivo
        );
    }

    /**
     * Obtiene el detalle de un reporte y valida visibilidad por rol.
     */
    public Optional<ReporteDetalleDTO> obtenerDetalleReporte(Integer reporteId, Usuario usuarioAutenticado) {
        Optional<ReporteDetalleDTO> opt = reporteRepository.obtenerDetalleReporte(reporteId);
        if (opt.isEmpty()) return Optional.empty();

        ReporteDetalleDTO detalle = opt.get();
        if (usuarioAutenticado != null) {
            if (usuarioAutenticado.getRolId() == 2 && !Objects.equals(usuarioAutenticado.getSucursalId(), detalle.getSucursalId())) {
                log.warn("Supervisor intentó consultar reporte de otra sucursal: {}", reporteId);
                return Optional.empty();
            }
        }

        return opt;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error serializando objeto a JSON: {}", e.getMessage());
            return "[]";
        }
    }
}
