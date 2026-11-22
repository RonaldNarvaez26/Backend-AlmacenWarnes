package com.almacen.warnes.modulo_seguridad.service;

import com.almacen.warnes.modulo_seguridad.dto.SesionDTO;
import com.almacen.warnes.modulo_seguridad.dto.SesionListadoDTO;
import com.almacen.warnes.modulo_seguridad.model.Usuario;
import com.almacen.warnes.modulo_seguridad.repository.SesionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SesionService {

    private final SesionRepository sesionRepository;

    @Transactional
    public Integer abrirSesion(Integer usuarioId) {
        log.info("Abriendo o retomando sesión para usuarioId: {}", usuarioId);
        Integer sesionId = sesionRepository.abrirSesion(usuarioId);
        log.info("Sesión activa confirmada -> ID: {} para usuarioId: {}", sesionId, usuarioId);
        return sesionId;
    }

    @Transactional
    public Integer cerrarSesionDirecta(Integer sesionId, Integer usuarioId) {
        log.info("Cerrando sesión directa ID: {} para usuarioId: {}", sesionId, usuarioId);
        Integer duracionMinutos = sesionRepository.cerrarSesion(sesionId, usuarioId);
        log.info("Sesión ID: {} cerrada con éxito. Duración: {} minutos", sesionId, duracionMinutos);
        return duracionMinutos;
    }

    public Optional<SesionDTO> obtenerSesionActiva(Integer usuarioId) {
        return sesionRepository.obtenerSesionActiva(usuarioId);
    }

    public List<SesionListadoDTO> listarSesionesAuditoria(
            Integer usuarioIdFiltro,
            Integer sucursalIdFiltro,
            Boolean soloActivas,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String textoBusqueda,
            Usuario usuarioAutenticado
    ) {
        Integer sucursalEfectiva = sucursalIdFiltro;
        // Si el usuario es Supervisor (rol 2), solo puede ver las sesiones de su sucursal
        if (usuarioAutenticado != null && usuarioAutenticado.getRolId() == 2) {
            sucursalEfectiva = usuarioAutenticado.getSucursalId();
        }

        return sesionRepository.listarSesiones(
                usuarioIdFiltro,
                sucursalEfectiva,
                soloActivas,
                fechaDesde,
                fechaHasta,
                textoBusqueda
        );
    }

    @Transactional
    public Integer purgarSesionesAntiguas(Integer mesesRetencion) {
        log.info("Iniciando purga física de sesiones cerradas mayores a {} meses", mesesRetencion);
        Integer eliminadas = sesionRepository.purgarSesionesAntiguas(mesesRetencion);
        log.info("Purga de sesiones finalizada -> {} registros eliminados", eliminadas);
        return eliminadas;
    }
}
