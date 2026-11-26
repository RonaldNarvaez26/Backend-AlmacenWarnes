package com.almacen.warnes.modulo_analitica.service;

import com.almacen.warnes.modulo_analitica.dto.CrearMovimientoDTO;
import com.almacen.warnes.modulo_analitica.dto.ModificarMovimientoDTO;
import com.almacen.warnes.modulo_analitica.dto.MovimientoListadoDTO;
import com.almacen.warnes.modulo_analitica.repository.MovimientoRepository;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;

    @Transactional
    public Integer crearMovimiento(CrearMovimientoDTO dto, Usuario usuarioAutenticado) {
        Integer sucursalIdEfectiva;

        // Regla obligatoria por rol:
        if (usuarioAutenticado.getRolId() == 3) {
            // Propietario: debe seleccionar explícitamente la sucursal
            if (dto.getSucursalId() == null || dto.getSucursalId() <= 0) {
                throw new IllegalArgumentException("El propietario debe seleccionar una sucursal válida para el movimiento.");
            }
            sucursalIdEfectiva = dto.getSucursalId();
        } else {
            // Cajero (1) o Supervisor (2): toma automáticamente la sucursal asignada a su usuario
            if (usuarioAutenticado.getSucursalId() == null || usuarioAutenticado.getSucursalId() <= 0) {
                throw new IllegalStateException("El usuario no tiene una sucursal asignada en el sistema.");
            }
            sucursalIdEfectiva = usuarioAutenticado.getSucursalId();
        }

        log.info("Creando movimiento: sucursal={}, tipo={}, monto={}, usuarioId={}",
                sucursalIdEfectiva, dto.getTipoMovimiento(), dto.getMonto(), usuarioAutenticado.getUsuarioId());

        Integer nuevoId = movimientoRepository.insertarMovimiento(
                sucursalIdEfectiva,
                dto.getMonto(),
                dto.getTipoMovimiento(),
                dto.getMetodoMovimiento(),
                dto.getDetalle(),
                usuarioAutenticado.getUsuarioId()
        );
        return nuevoId;
    }

    @Transactional
    public Map<String, Object> registrarYConsolidarSiAplica(CrearMovimientoDTO dto, Usuario usuarioAutenticado) {
        Integer nuevoId = crearMovimiento(dto, usuarioAutenticado);
        Integer reporteId = null;

        boolean esSupervisorOProp = usuarioAutenticado.getRolId() == 2 || usuarioAutenticado.getRolId() == 3;
        if (esSupervisorOProp && Boolean.TRUE.equals(dto.getConsolidarDirecto())) {
            Integer sucursalIdEfectiva = usuarioAutenticado.getRolId() == 3
                    ? dto.getSucursalId()
                    : usuarioAutenticado.getSucursalId();
            reporteId = movimientoRepository.consolidarMovimientos(
                    sucursalIdEfectiva,
                    List.of(nuevoId),
                    usuarioAutenticado.getUsuarioId()
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("movimientoId", nuevoId);
        if (reporteId != null) {
            result.put("reporteId", reporteId);
        }
        return result;
    }

    @Transactional
    public List<Integer> consolidarMovimientos(List<Integer> movimientoIds, Usuario usuarioAutenticado) {
        if (usuarioAutenticado == null || (usuarioAutenticado.getRolId() != 2 && usuarioAutenticado.getRolId() != 3)) {
            throw new SecurityException("Solo un Supervisor o Propietario puede consolidar movimientos directamente.");
        }
        if (movimientoIds == null || movimientoIds.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un movimiento pendiente para consolidar.");
        }

        Integer sucursalFiltro = usuarioAutenticado.getRolId() == 2 ? usuarioAutenticado.getSucursalId() : null;
        List<MovimientoListadoDTO> pendientes = movimientoRepository.listarMovimientos(
                sucursalFiltro, null, true, null, null, null, null, "PENDIENTES"
        );

        Set<Integer> idsSolicitados = Set.copyOf(movimientoIds);
        Map<Integer, List<Integer>> porSucursal = pendientes.stream()
                .filter(m -> idsSolicitados.contains(m.getMovimientoId()))
                .collect(Collectors.groupingBy(
                        MovimientoListadoDTO::getSucursalId,
                        LinkedHashMap::new,
                        Collectors.mapping(MovimientoListadoDTO::getMovimientoId, Collectors.toList())
                ));

        if (porSucursal.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron movimientos pendientes válidos en su sucursal para consolidar.");
        }

        List<Integer> reportesGenerados = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : porSucursal.entrySet()) {
            log.info("Consolidando {} movimiento(s) en sucursal {} por usuario {}",
                    entry.getValue().size(), entry.getKey(), usuarioAutenticado.getUsuarioId());
            Integer repId = movimientoRepository.consolidarMovimientos(
                    entry.getKey(),
                    entry.getValue(),
                    usuarioAutenticado.getUsuarioId()
            );
            if (repId != null) {
                reportesGenerados.add(repId);
            }
        }
        return reportesGenerados;
    }

    @Transactional
    public void modificarMovimiento(Integer movimientoId, ModificarMovimientoDTO dto, Usuario usuarioAutenticado) {
        log.info("Modificando movimiento ID: {} por usuarioId: {}", movimientoId, usuarioAutenticado.getUsuarioId());
        movimientoRepository.modificarMovimiento(
                movimientoId,
                dto.getMonto(),
                dto.getTipoMovimiento(),
                dto.getMetodoMovimiento(),
                dto.getDetalle(),
                usuarioAutenticado.getUsuarioId()
        );
    }

    @Transactional
    public void eliminarMovimiento(Integer movimientoId, Usuario usuarioAutenticado) {
        log.info("Eliminando movimiento ID: {} por usuarioId: {}", movimientoId, usuarioAutenticado.getUsuarioId());
        movimientoRepository.eliminarMovimiento(movimientoId, usuarioAutenticado.getUsuarioId());
    }

    public List<MovimientoListadoDTO> listarMovimientos(
            Integer sucursalIdParam,
            Integer reporteId,
            Boolean soloPendientes,
            Integer tipoMovimiento,
            Integer usuarioIdParam,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            Usuario usuarioAutenticado
    ) {
        Integer sucursalEfectiva = sucursalIdParam;
        Integer usuarioEfectivo = usuarioIdParam;

        if (usuarioAutenticado != null) {
            // Supervisor (2): forzar su sucursal
            if (usuarioAutenticado.getRolId() == 2) {
                sucursalEfectiva = usuarioAutenticado.getSucursalId();
            }
            // Cajero (1): forzar su sucursal y su usuario si no es consulta general de turno
            else if (usuarioAutenticado.getRolId() == 1) {
                sucursalEfectiva = usuarioAutenticado.getSucursalId();
                if (usuarioEfectivo == null) {
                    usuarioEfectivo = usuarioAutenticado.getUsuarioId();
                }
            }
        }

        return movimientoRepository.listarMovimientos(
                sucursalEfectiva,
                reporteId,
                soloPendientes,
                tipoMovimiento,
                usuarioEfectivo,
                fechaDesde,
                fechaHasta,
                estado
        );
    }
}
