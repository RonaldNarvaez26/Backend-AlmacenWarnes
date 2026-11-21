package com.almacen.warnes.modulo_ventas.service;

import com.almacen.warnes.modulo_ventas.dto.ClienteDTO;
import com.almacen.warnes.modulo_ventas.mapper.VentaMapper;
import com.almacen.warnes.modulo_ventas.model.Cliente;
import com.almacen.warnes.modulo_ventas.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para la gestión de Clientes (ABM y búsqueda para facturación).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final VentaMapper ventaMapper;

    /**
     * Busca un cliente activo por su NIT/CI. Retorna null si no existe.
     */
    public ClienteDTO buscarPorNit(String nit) {
        if (nit == null || nit.trim().isEmpty()) {
            return null;
        }
        log.info("Buscando cliente por NIT='{}'", nit);
        Optional<Cliente> cliente = clienteRepository.buscarClientePorNit(nit.trim());
        return cliente.map(ventaMapper::toDTO).orElse(null);
    }

    /**
     * Obtiene un cliente por su ID. Retorna null si no existe.
     */
    public ClienteDTO obtenerPorId(Integer clienteId) {
        log.info("Obteniendo cliente por ID={}", clienteId);
        return clienteRepository.obtenerClientePorId(clienteId)
                .map(ventaMapper::toDTO)
                .orElse(null);
    }

    /**
     * Lista clientes con filtros de búsqueda y estado.
     */
    public List<ClienteDTO> listarClientes(String busqueda, String estado) {
        String est = (estado != null && !estado.isBlank()) ? estado.toUpperCase() : "ACTIVOS";
        log.info("Listando clientes busqueda='{}', estado='{}'", busqueda, est);
        return clienteRepository.listarClientes(busqueda, est);
    }

    /**
     * Crea un cliente o reactiva/actualiza uno existente mediante el SP sp_clientes_insertar.
     */
    @Transactional
    public ClienteDTO crearCliente(ClienteDTO dto, Integer usuarioCreacion) {
        log.info("Creando o registrando cliente NIT='{}', RazonSocial='{}'", dto.getNit(), dto.getNombreRazonSocial());
        Integer clienteId = clienteRepository.crearCliente(dto, usuarioCreacion);
        return obtenerPorId(clienteId);
    }

    /**
     * Actualiza un cliente existente mediante el SP sp_clientes_modificar.
     */
    @Transactional
    public ClienteDTO modificarCliente(Integer clienteId, ClienteDTO dto, Integer usuarioModificacion) {
        log.info("Modificando cliente ID={}, NIT='{}'", clienteId, dto.getNit());
        clienteRepository.modificarCliente(clienteId, dto, usuarioModificacion);
        return obtenerPorId(clienteId);
    }

    /**
     * Da de baja lógica a un cliente mediante el SP sp_clientes_eliminar.
     */
    @Transactional
    public void eliminarCliente(Integer clienteId, Integer usuarioModificacion) {
        log.info("Dando de baja lógica a cliente ID={}", clienteId);
        clienteRepository.eliminarCliente(clienteId, usuarioModificacion);
    }
}
