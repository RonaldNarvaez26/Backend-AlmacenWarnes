package com.almacen.warnes.modulo_ventas.service;

import com.almacen.warnes.modulo_ventas.dto.*;
import com.almacen.warnes.modulo_ventas.mapper.VentaMapper;
import com.almacen.warnes.modulo_ventas.model.Cliente;
import com.almacen.warnes.modulo_ventas.model.DetalleVenta;
import com.almacen.warnes.modulo_ventas.model.ProductoPOS;
import com.almacen.warnes.modulo_ventas.model.Venta;
import com.almacen.warnes.modulo_ventas.repository.VentaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio del módulo de ventas POS.
 * Implementa la lógica de negocio sobre los SPs de PostgreSQL.
 *
 * SLF4J:
 *   INFO  → operaciones normales (búsqueda, registro)
 *   WARN  → anulaciones y eventos de negocio importantes
 *   ERROR → excepciones inesperadas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteService clienteService;
    private final VentaMapper ventaMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================================
    // BUSCADOR POS
    // =========================================================================

    /**
     * Busca productos en el inventario de la sucursal dada usando fn_pos_buscar_producto.
     * El criterio puede ser código de barras (match exacto), código interno (match exacto)
     * o parte del nombre (ILIKE).
     */
    public List<ProductoPOSDTO> buscarProducto(Integer sucursalId, String criterio) {
        if (criterio == null || criterio.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El criterio de búsqueda no puede estar vacío.");
        }
        log.info("POS búsqueda → sucursal={} criterio='{}'", sucursalId, criterio.trim());

        List<ProductoPOS> resultados = ventaRepository.buscarProductoPOS(sucursalId, criterio.trim());
        log.info("POS búsqueda → {} resultado(s) encontrado(s)", resultados.size());

        return ventaMapper.toDTOList(resultados);
    }

    // =========================================================================
    // REGISTRAR VENTA
    // =========================================================================

    /**
     * Registra la venta, descuenta el stock y genera el recibo térmico.
     *
     * FLUJO:
     *   1. Serializa dto.detalles a JSON (formato esperado por sp_ventas_registrar)
     *   2. Llama sp_ventas_registrar (atómico: valida cobros, inserta venta, descuenta stock por PEPS)
     *   3. Recupera la venta y sus detalles para construir el DTO de respuesta
     *   4. Genera el HTML del recibo (VentaMapper.generarReciboHtml)
     *
     * El SP valida internamente con FOR UPDATE (bloqueo de fila) que el stock sea suficiente,
     * garantizando consistencia incluso con múltiples cajeros concurrentes.
     *
     * NOTA: Este endpoint es llamado por el frontend solo cuando los 5 segundos
     * de cuenta regresiva expiran sin que el cajero presione "Abortar".
     */
    @Transactional
    public VentaResponseDTO registrarVenta(RegistrarVentaDTO dto) {
        log.info("Registrando venta → cajero={} sucursal={} total={} método={}",
                dto.getUsuarioId(), dto.getSucursalId(), dto.getPrecioTotal(), dto.getMetodoVenta());

        // Serializar detalles a JSON con las claves snake_case exactas requeridas por sp_ventas_registrar:
        // [{"inventario_id": 1, "producto_id": 2, "cantidad": 1, "precio_unitario": 10.50}]
        String detallesJson;
        try {
            List<Map<String, Object>> detallesMapList = new ArrayList<>();
            for (DetalleVentaDTO d : dto.getDetalles()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("inventario_id", d.getInventarioId());
                map.put("producto_id", d.getProductoId());
                map.put("cantidad", d.getCantidad());
                map.put("precio_unitario", d.getPrecioUnitario());
                detallesMapList.add(map);
            }
            detallesJson = objectMapper.writeValueAsString(detallesMapList);
            log.info("Payload detalles JSON para sp_ventas_registrar: {}", detallesJson);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar detalles de venta: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al procesar los detalles de la venta.");
        }

        // Llamar al SP — puede lanzar excepción si stock insuficiente o cobros no cuadran
        Integer nuevaVentaId;
        try {
            nuevaVentaId = ventaRepository.registrarVenta(dto, detallesJson);
        } catch (Exception e) {
            log.error("Error en sp_ventas_registrar: {}", e.getMessage(), e);
            String rawMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, extraerMensajeAmigable(rawMsg));
        }

        log.info("Venta #{} registrada exitosamente → cajero={} total=Bs.{}",
                nuevaVentaId, dto.getUsuarioId(), dto.getPrecioTotal());

        // Emitir actualización de stock vía WebSocket a la sucursal en tiempo real
        List<Integer> inventarioIds = dto.getDetalles().stream()
                .map(DetalleVentaDTO::getInventarioId)
                .distinct()
                .toList();
        notificarActualizacionStock("VENTA_REGISTRADA", dto.getSucursalId(), nuevaVentaId, inventarioIds);

        // Recuperar venta y detalles para enriquecer la respuesta
        Venta venta = ventaRepository.obtenerVentaPorId(nuevaVentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Venta registrada pero no encontrada al recuperar."));
        List<DetalleVenta> detalles = ventaRepository.obtenerDetallesPorVenta(nuevaVentaId);

        // Datos del cliente (si se proporcionó)
        ClienteDTO clienteDTO = null;
        if (dto.getClienteId() != null) {
            clienteDTO = clienteService.obtenerPorId(dto.getClienteId());
        }

        // Enriquecer con nombre del cajero y sucursal (se reciben del request por simplicidad)
        // En una futura versión pueden obtenerse desde SecurityContext
        String cajeroNombre = "Cajero #" + dto.getUsuarioId();
        String sucursalNombre = "Sucursal #" + dto.getSucursalId();

        return ventaMapper.toResponseDTO(venta, detalles, cajeroNombre, sucursalNombre, clienteDTO);
    }

    // =========================================================================
    // ANULAR VENTA
    // =========================================================================

    /**
     * Anula una venta y revierte el stock a los lotes originales.
     * Solo puede ejecutarlo un Propietario o Supervisor.
     * El SP valida que la venta no esté ya anulada.
     */
    @Transactional
    public void anularVenta(AnularVentaDTO dto) {
        log.warn("ANULACIÓN de venta #{} solicitada por usuario={}",
                dto.getVentaId(), dto.getUsuarioAnulacion());

        // Obtener detalles e inventario_ids antes de anular para la notificación WebSocket
        Venta venta = ventaRepository.obtenerVentaPorId(dto.getVentaId()).orElse(null);
        List<DetalleVenta> detalles = ventaRepository.obtenerDetallesPorVenta(dto.getVentaId());
        List<Integer> inventarioIds = detalles.stream()
                .map(DetalleVenta::getInventarioId)
                .distinct()
                .toList();
        Integer sucursalId = venta != null ? venta.getSucursalId() : 1;

        try {
            ventaRepository.anularVenta(dto.getVentaId(), dto.getUsuarioAnulacion());
        } catch (Exception e) {
            log.error("Error en sp_ventas_anular venta #{}: {}", dto.getVentaId(), e.getMessage(), e);
            String rawMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, extraerMensajeAmigable(rawMsg));
        }

        log.warn("Venta #{} ANULADA — stock revertido por usuario={}",
                dto.getVentaId(), dto.getUsuarioAnulacion());

        // Emitir actualización de stock vía WebSocket a la sucursal en tiempo real
        notificarActualizacionStock("VENTA_ANULADA", sucursalId, dto.getVentaId(), inventarioIds);
    }

    // =========================================================================
    // HELPER WEBSOCKET & ERRORES
    // =========================================================================

    private void notificarActualizacionStock(String tipoEvento, Integer sucursalId, Integer ventaId, List<Integer> inventarioIds) {
        if (inventarioIds == null || inventarioIds.isEmpty()) return;
        try {
            List<ActualizacionStockDTO.ItemStockActualizadoDTO> items = ventaRepository.obtenerStockActualizado(inventarioIds);
            ActualizacionStockDTO payload = ActualizacionStockDTO.builder()
                    .tipoEvento(tipoEvento)
                    .sucursalId(sucursalId)
                    .ventaId(ventaId)
                    .items(items)
                    .build();

            String destination = "/topic/sucursal/" + sucursalId + "/stock";
            log.info("Emitiendo WebSocket a destination='{}' con {} items de stock actualizados",
                    destination, items.size());
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception ex) {
            log.error("Error al emitir actualización de stock vía WebSocket: {}", ex.getMessage(), ex);
        }
    }

    private String extraerMensajeAmigable(String raw) {
        if (raw == null) return "Error al procesar la venta.";
        if (raw.contains("Stock insuficiente")) {
            int idx = raw.indexOf("Stock insuficiente");
            int endIdx = raw.indexOf("\n", idx);
            return endIdx > idx ? raw.substring(idx, endIdx).trim() : raw.substring(idx).trim();
        }
        if (raw.contains("ERROR:")) {
            int idx = raw.indexOf("ERROR:") + 6;
            int endIdx = raw.indexOf("\n", idx);
            return endIdx > idx ? raw.substring(idx, endIdx).trim() : raw.substring(idx).trim();
        }
        return raw;
    }

    // =========================================================================
    // CONSULTAR VENTA
    // =========================================================================

    /**
     * Retorna el detalle completo de una venta con su recibo generado.
     */
    public VentaResponseDTO obtenerVenta(Integer ventaId) {
        log.info("Consultando detalle venta #{}", ventaId);

        Venta venta = ventaRepository.obtenerVentaPorId(ventaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Venta #" + ventaId + " no encontrada."));
        List<DetalleVenta> detalles = ventaRepository.obtenerDetallesPorVenta(ventaId);

        ClienteDTO clienteDTO = null;
        if (venta.getClienteId() != null) {
            clienteDTO = clienteService.obtenerPorId(venta.getClienteId());
        }

        String cajeroNombre = "Cajero #" + venta.getUsuarioCreacion();
        String sucursalNombre = "Sucursal #" + venta.getSucursalId();

        return ventaMapper.toResponseDTO(venta, detalles, cajeroNombre, sucursalNombre, clienteDTO);
    }

    // =========================================================================
    // HISTORIAL
    // =========================================================================

    /**
     * Lista ventas de una sucursal invocando el procedimiento almacenado sp_ventas_listar.
     * Soporta filtros opcionales de fecha (yyyy-MM-dd), método de venta y estado.
     */
    public List<VentaResponseDTO> listarVentas(Integer sucursalId, String fecha, Integer metodoVenta, Integer estado) {
        log.info("Historial ventas via SP → sucursal={} fecha='{}' metodo={} estado={}",
                sucursalId, fecha != null ? fecha : "todas", metodoVenta, estado);

        LocalDate localDate = null;
        if (fecha != null && !fecha.isBlank()) {
            try {
                localDate = LocalDate.parse(fecha.trim());
            } catch (Exception e) {
                log.warn("Formato de fecha inválido '{}', ignorando filtro de fecha.", fecha);
            }
        }

        return ventaRepository.listarVentasSP(sucursalId, localDate, metodoVenta, estado);
    }

    public List<VentaResponseDTO> listarVentas(Integer sucursalId, String fecha) {
        return listarVentas(sucursalId, fecha, null, null);
    }

    // =========================================================================
    // CLIENTES
    // =========================================================================

    /**
     * Busca un cliente activo por NIT/CI delegando a ClienteService. Retorna null si no existe.
     */
    public ClienteDTO buscarCliente(String nit) {
        return clienteService.buscarPorNit(nit);
    }

    /**
     * Registra o reactiva un cliente delegando a ClienteService (ejecuta sp_clientes_insertar).
     */
    @Transactional
    public ClienteDTO crearCliente(ClienteDTO dto, Integer usuarioCreacion) {
        return clienteService.crearCliente(dto, usuarioCreacion);
    }
}
