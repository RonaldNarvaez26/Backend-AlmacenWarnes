package com.almacen.warnes.modulo_analitica.libro_mayor.service;

import com.almacen.warnes.modulo_analitica.libro_mayor.dto.FiltrosOpcionesDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.dto.LibroMayorItemDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.dto.LibroMayorResumenDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.dto.MovimientoExtraVentaRequestDTO;
import com.almacen.warnes.modulo_analitica.libro_mayor.repository.LibroMayorRepository;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibroMayorService {

    private final LibroMayorRepository libroMayorRepository;

    private static final int ROL_PROPIETARIO = 3;

    /**
     * Resuelve la sucursal autorizada según el rol del usuario (RBAC).
     */
    private Integer resolverSucursalAutorizada(Integer sucursalIdParam, Usuario usuario) {
        if (usuario.getRolId() != null && usuario.getRolId() == ROL_PROPIETARIO) {
            return sucursalIdParam != null && sucursalIdParam > 0 ? sucursalIdParam : null;
        }
        return usuario.getSucursalId();
    }

    /**
     * Valida y normaliza el rango de fechas.
     */
    private LocalDate[] normalizarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDate inicio = fechaInicio != null ? fechaInicio : LocalDate.now();
        LocalDate fin = fechaFin != null ? fechaFin : LocalDate.now();

        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
        return new LocalDate[]{inicio, fin};
    }

    /**
     * Consulta el listado detallado del Libro Mayor con filtros avanzados.
     */
    public List<LibroMayorItemDTO> consultarLibroMayor(
            Integer sucursalIdParam,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            LocalTime horaInicio,
            LocalTime horaFin,
            Integer cajeroId,
            Integer productoId,
            Integer tipoMovimiento,
            Integer metodoMovimiento,
            Boolean incluirBajas,
            Usuario usuario
    ) {
        Integer sucursalId = resolverSucursalAutorizada(sucursalIdParam, usuario);
        LocalDate[] fechas = normalizarFechas(fechaInicio, fechaFin);

        log.info("Consultando Libro Mayor: sucursal={}, rango=[{}, {}], horas=[{}-{}], cajero={}, producto={}, tipo={}, metodo={}, bajas={}",
                sucursalId, fechas[0], fechas[1], horaInicio, horaFin, cajeroId, productoId, tipoMovimiento, metodoMovimiento, incluirBajas);

        return libroMayorRepository.consultarLibroMayor(
                sucursalId,
                fechas[0],
                fechas[1],
                horaInicio,
                horaFin,
                cajeroId,
                productoId,
                tipoMovimiento,
                metodoMovimiento,
                incluirBajas
        );
    }

    /**
     * Consulta el resumen y balance del periodo del Libro Mayor con filtros avanzados.
     */
    public LibroMayorResumenDTO consultarResumen(
            Integer sucursalIdParam,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            LocalTime horaInicio,
            LocalTime horaFin,
            Integer cajeroId,
            Integer productoId,
            Usuario usuario
    ) {
        Integer sucursalId = resolverSucursalAutorizada(sucursalIdParam, usuario);
        LocalDate[] fechas = normalizarFechas(fechaInicio, fechaFin);

        log.info("Consultando Resumen Libro Mayor: sucursal={}, rango=[{}, {}], horas=[{}-{}], cajero={}, producto={}",
                sucursalId, fechas[0], fechas[1], horaInicio, horaFin, cajeroId, productoId);

        return libroMayorRepository.consultarResumen(
                sucursalId,
                fechas[0],
                fechas[1],
                horaInicio,
                horaFin,
                cajeroId,
                productoId
        );
    }

    /**
     * Obtiene el JSON detallado de una venta para emitir su Recibo PDF.
     */
    public String obtenerReciboVenta(Integer ventaId, Usuario usuario) {
        log.info("Generando datos de recibo para venta #{}, usuario solicitante: {}", ventaId, usuario.getUsuarioId());
        String reciboJson = libroMayorRepository.obtenerReciboVentaJson(ventaId);
        if (reciboJson == null || reciboJson.isBlank() || "null".equals(reciboJson.trim())) {
            throw new IllegalArgumentException("Venta no encontrada con ID: " + ventaId);
        }
        return reciboJson;
    }

    /**
     * Obtiene listas de opciones para los selectores de filtros (cajeros y productos).
     */
    public FiltrosOpcionesDTO obtenerFiltrosOpciones(Integer sucursalIdParam, Usuario usuario) {
        Integer sucursalId = resolverSucursalAutorizada(sucursalIdParam, usuario);
        return libroMayorRepository.obtenerFiltrosOpciones(sucursalId);
    }

    /**
     * Registra un movimiento extra-venta.
     */
    public Map<String, Object> registrarMovimientoExtra(
            MovimientoExtraVentaRequestDTO dto,
            Usuario usuario
    ) {
        Integer sucursalId = resolverSucursalAutorizada(dto.getSucursalId(), usuario);
        if (sucursalId == null || sucursalId <= 0) {
            throw new IllegalArgumentException("Debe seleccionar una sucursal válida para registrar el movimiento.");
        }
        dto.setSucursalId(sucursalId);

        log.info("Registrando movimiento extra-venta: sucursal={}, tipo={}, monto={}, usuario={}",
                dto.getSucursalId(), dto.getTipoMovimiento(), dto.getMonto(), usuario.getUsuarioId());

        return libroMayorRepository.registrarMovimientoExtra(dto, usuario.getUsuarioId());
    }

    /**
     * Da de baja lógica a un movimiento o anula una venta.
     */
    public Map<String, Object> anularMovimiento(
            String origen,
            Integer id,
            String motivo,
            Usuario usuario
    ) {
        log.info("Anulando registro en Libro Mayor: origen={}, id={}, usuario={}", origen, id, usuario.getUsuarioId());
        return libroMayorRepository.anularMovimiento(origen, id, usuario.getUsuarioId(), motivo);
    }
}
