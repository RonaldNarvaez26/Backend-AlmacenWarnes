package com.almacen.warnes.modulo_ventas.mapper;

import com.almacen.warnes.modulo_ventas.dto.ClienteDTO;
import com.almacen.warnes.modulo_ventas.dto.DetalleVentaDTO;
import com.almacen.warnes.modulo_ventas.dto.ProductoPOSDTO;
import com.almacen.warnes.modulo_ventas.dto.VentaResponseDTO;
import com.almacen.warnes.modulo_ventas.model.Cliente;
import com.almacen.warnes.modulo_ventas.model.DetalleVenta;
import com.almacen.warnes.modulo_ventas.model.ProductoPOS;
import com.almacen.warnes.modulo_ventas.model.Venta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper del módulo de ventas.
 * Convierte modelos ↔ DTOs y genera el HTML del recibo térmico.
 */
@Component
public class VentaMapper {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =========================================================================
    // ProductoPOS → ProductoPOSDTO
    // =========================================================================

    public ProductoPOSDTO toDTO(ProductoPOS model) {
        if (model == null) return null;
        return ProductoPOSDTO.builder()
                .inventarioId(model.getInventarioId())
                .productoId(model.getProductoId())
                .codigoBarras(model.getCodigoBarras())
                .codigoInterno(model.getCodigoInterno())
                .nombreProducto(model.getNombreProducto())
                .precioVenta(model.getPrecioVenta())
                .stockDisponible(model.getStockDisponible())
                .enDescuento(model.getEnDescuento())
                .precioDescuento(model.getPrecioDescuento())
                .build();
    }

    public List<ProductoPOSDTO> toDTOList(List<ProductoPOS> models) {
        return models.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // =========================================================================
    // DetalleVenta → DetalleVentaDTO
    // =========================================================================

    public DetalleVentaDTO toDTO(DetalleVenta model) {
        if (model == null) return null;
        BigDecimal subtotal = model.getPrecioUnitario() != null && model.getCantidad() != null
                ? model.getPrecioUnitario().multiply(model.getCantidad())
                : BigDecimal.ZERO;
        return DetalleVentaDTO.builder()
                .detalleVentaId(model.getDetalleVentaId())
                .inventarioId(model.getInventarioId())
                .productoId(model.getProductoId())
                .cantidad(model.getCantidad())
                .precioUnitario(model.getPrecioUnitario())
                .subtotal(subtotal)
                .build();
    }

    // =========================================================================
    // Venta + detalles + datos extra → VentaResponseDTO (con reciboHtml)
    // =========================================================================

    /**
     * Construye el DTO de respuesta completo, incluyendo el HTML del recibo térmico.
     *
     * @param venta        Cabecera de la venta
     * @param detalles     Líneas del carrito
     * @param cajeroNombre Nombre completo del cajero (enriquecido desde Usuarios)
     * @param sucursalNombre Nombre/ciudad de la sucursal
     * @param cliente      Datos del cliente (puede ser null para ventas anónimas)
     */
    public VentaResponseDTO toResponseDTO(Venta venta,
                                          List<DetalleVenta> detalles,
                                          String cajeroNombre,
                                          String sucursalNombre,
                                          ClienteDTO cliente) {
        List<DetalleVentaDTO> detallesDTO = detalles.stream().map(this::toDTO).collect(Collectors.toList());
        String reciboHtml = generarReciboHtml(venta, detallesDTO, cajeroNombre, sucursalNombre, cliente);

        return VentaResponseDTO.builder()
                .ventaId(venta.getVentaId())
                .sucursalId(venta.getSucursalId())
                .sucursalNombre(sucursalNombre)
                .estado(venta.getEstado())
                .precioTotal(venta.getPrecioTotal())
                .metodoVenta(venta.getMetodoVenta())
                .cobroEfectivo(venta.getCobroEfectivo())
                .cobroQr(venta.getCobroQr())
                .cobroTarjeta(venta.getCobroTarjeta())
                .montoRecibido(venta.getMontoRecibido())
                .cambio(venta.getCambio())
                .clienteId(venta.getClienteId())
                .anuladoPor(venta.getAnuladoPor())
                .fechaCreacion(venta.getFechaCreacion())
                .cajeroNombre(cajeroNombre)
                .detalles(detallesDTO)
                .reciboHtml(reciboHtml)
                .build();
    }

    // =========================================================================
    // Cliente ↔ ClienteDTO
    // =========================================================================

    public ClienteDTO toDTO(Cliente cliente) {
        if (cliente == null) return null;
        return ClienteDTO.builder()
                .clienteId(cliente.getClienteId())
                .nit(cliente.getNit())
                .nombres(cliente.getNombres())
                .apellidos(cliente.getApellidos())
                .nombreRazonSocial(cliente.getNombreRazonSocial())
                .telefono(cliente.getTelefono())
                .build();
    }

    // =========================================================================
    // GENERADOR DE RECIBO TÉRMICO HTML (80mm)
    // =========================================================================

    /**
     * Genera el HTML del recibo térmico optimizado para impresora de 80mm.
     * Incluye todos los campos de NIT/Razón Social para cuando se active la facturación oficial.
     * Vue recibe este string, lo inyecta en un div oculto (#recibo-termico) y ejecuta window.print().
     *
     * El CSS @media print en el frontend oculta toda la UI y muestra solo el recibo.
     */
    public String generarReciboHtml(Venta venta,
                                    List<DetalleVentaDTO> detalles,
                                    String cajeroNombre,
                                    String sucursalNombre,
                                    ClienteDTO cliente) {

        String fechaStr = venta.getFechaCreacion() != null
                ? venta.getFechaCreacion().format(FMT_FECHA)
                : "N/D";

        StringBuilder lineasProductos = new StringBuilder();
        for (DetalleVentaDTO d : detalles) {
            String nombreProd = d.getNombreProducto() != null && !d.getNombreProducto().isBlank()
                    ? d.getNombreProducto()
                    : "Producto #" + d.getProductoId();
            BigDecimal subtotal = d.getSubtotal() != null ? d.getSubtotal()
                    : (d.getPrecioUnitario() != null && d.getCantidad() != null
                            ? d.getPrecioUnitario().multiply(d.getCantidad())
                            : BigDecimal.ZERO);
            lineasProductos.append(
                    "<tr>" +
                    "<td style='text-align:left'>" + nombreProd + "</td>" +
                    "<td style='text-align:center'>" + d.getCantidad().stripTrailingZeros().toPlainString() + "</td>" +
                    "<td style='text-align:right'>Bs. " + String.format("%.2f", d.getPrecioUnitario()) + "</td>" +
                    "<td style='text-align:right'>Bs. " + String.format("%.2f", subtotal) + "</td>" +
                    "</tr>"
            );
        }

        // Bloque de método de pago
        StringBuilder metodoPagoHtml = new StringBuilder();
        if (venta.getCobroEfectivo() != null && venta.getCobroEfectivo().compareTo(BigDecimal.ZERO) > 0) {
            metodoPagoHtml.append("<tr><td colspan='2'>EFECTIVO:</td><td colspan='2' style='text-align:right'>Bs. ")
                    .append(String.format("%.2f", venta.getCobroEfectivo())).append("</td></tr>");
        }
        if (venta.getCobroQr() != null && venta.getCobroQr().compareTo(BigDecimal.ZERO) > 0) {
            metodoPagoHtml.append("<tr><td colspan='2'>PAGO QR:</td><td colspan='2' style='text-align:right'>Bs. ")
                    .append(String.format("%.2f", venta.getCobroQr())).append("</td></tr>");
        }
        if (venta.getCobroTarjeta() != null && venta.getCobroTarjeta().compareTo(BigDecimal.ZERO) > 0) {
            metodoPagoHtml.append("<tr><td colspan='2'>TARJETA:</td><td colspan='2' style='text-align:right'>Bs. ")
                    .append(String.format("%.2f", venta.getCobroTarjeta())).append("</td></tr>");
        }
        if (venta.getMontoRecibido() != null && venta.getMontoRecibido().compareTo(BigDecimal.ZERO) > 0) {
            metodoPagoHtml.append("<tr><td colspan='2'>RECIBIDO:</td><td colspan='2' style='text-align:right'>Bs. ")
                    .append(String.format("%.2f", venta.getMontoRecibido())).append("</td></tr>");
        }
        if (venta.getCambio() != null && venta.getCambio().compareTo(BigDecimal.ZERO) > 0) {
            metodoPagoHtml.append("<tr><td colspan='2'><strong>CAMBIO:</strong></td><td colspan='2' style='text-align:right'><strong>Bs. ")
                    .append(String.format("%.2f", venta.getCambio())).append("</strong></td></tr>");
        }

        // Bloque de cliente (NIT/Razón Social para fase de facturación)
        String clienteHtml = "";
        if (cliente != null && cliente.getNit() != null && !cliente.getNit().isBlank()) {
            String nombre = (cliente.getNombreRazonSocial() != null && !cliente.getNombreRazonSocial().isBlank())
                    ? cliente.getNombreRazonSocial()
                    : ((cliente.getNombres() != null ? cliente.getNombres() : "") + " "
                       + (cliente.getApellidos() != null ? cliente.getApellidos() : "")).trim();
            clienteHtml = "<tr><td colspan='4' style='padding-top:4px'>NIT/CI: <strong>" + cliente.getNit() + "</strong></td></tr>" +
                          "<tr><td colspan='4'>Razón Social: <strong>" + nombre + "</strong></td></tr>";
        } else {
            clienteHtml = "<tr><td colspan='4'>Cliente: Consumidor Final</td></tr>";
        }

        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'>" +
               "<style>" +
               "  body { font-family: 'Courier New', monospace; font-size: 11px; width: 72mm; margin: 0 auto; }" +
               "  .separador { border-top: 1px dashed #000; margin: 4px 0; }" +
               "  table { width: 100%; border-collapse: collapse; }" +
               "  td { padding: 2px 1px; font-size: 10px; }" +
               "  .titulo { text-align: center; font-weight: bold; font-size: 13px; }" +
               "  .subtitulo { text-align: center; font-size: 10px; }" +
               "  .total { font-weight: bold; font-size: 12px; }" +
               "  .pie { text-align: center; margin-top: 6px; font-size: 9px; }" +
               "  .futuro { text-align: center; color: #555; font-size: 9px; font-style: italic; }" +
               "</style>" +
               "</head><body>" +
               "<div class='titulo'>ALMACÉN WARNES</div>" +
               "<div class='subtitulo'>Sucursal: " + (sucursalNombre != null ? sucursalNombre : "N/D") + "</div>" +
               "<div class='subtitulo'>Cajero: " + (cajeroNombre != null ? cajeroNombre : "N/D") + "</div>" +
               "<div class='subtitulo'>Fecha: " + fechaStr + "</div>" +
               "<div class='subtitulo'>Recibo N° " + venta.getVentaId() + "</div>" +
               "<div class='separador'></div>" +
               "<table>" +
               "<tr><th style='text-align:left'>Producto</th><th>Cant</th><th style='text-align:right'>P.Unit</th><th style='text-align:right'>Subt.</th></tr>" +
               "<tr><td colspan='4'><div class='separador'></div></td></tr>" +
               lineasProductos +
               "<tr><td colspan='4'><div class='separador'></div></td></tr>" +
               "<tr><td colspan='2' class='total'>TOTAL:</td><td colspan='2' style='text-align:right' class='total'>Bs. " +
               String.format("%.2f", venta.getPrecioTotal()) + "</td></tr>" +
               metodoPagoHtml +
               "<tr><td colspan='4'><div class='separador'></div></td></tr>" +
               clienteHtml +
               "</table>" +
               "<div class='separador'></div>" +
               "<div class='pie'>** GRACIAS POR SU COMPRA **</div>" +
               "<div class='futuro'>(Fase futura: Factura Fiscal Oficial)</div>" +
               "</body></html>";
    }
}
