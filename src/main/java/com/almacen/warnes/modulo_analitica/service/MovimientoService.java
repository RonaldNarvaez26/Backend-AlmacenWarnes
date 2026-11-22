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
import java.util.List;

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

        return movimientoRepository.insertarMovimiento(
                sucursalIdEfectiva,
                dto.getMonto(),
                dto.getTipoMovimiento(),
                dto.getMetodoMovimiento(),
                dto.getDetalle(),
                usuarioAutenticado.getUsuarioId()
        );
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
                fechaHasta
        );
    }
}
