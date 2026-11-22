package com.almacen.warnes.modulo_seguridad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SesionPurgeJob {

    private final SesionService sesionService;

    /**
     * Purga física programada de sesiones cerradas mayores a 3 meses.
     * Se ejecuta todos los domingos a las 03:00 AM hora de Bolivia (America/La_Paz).
     */
    @Scheduled(cron = "0 0 3 * * SUN", zone = "America/La_Paz")
    public void ejecutarPurgaSemanalSesiones() {
        log.info("[SesionPurgeJob] Iniciando tarea programada de purga de sesiones antiguas...");
        try {
            Integer registrosEliminados = sesionService.purgarSesionesAntiguas(3);
            log.info("[SesionPurgeJob] Tarea completada exitosamente. Registros eliminados: {}", registrosEliminados);
        } catch (Exception e) {
            log.error("[SesionPurgeJob] Error durante la purga programada de sesiones: {}", e.getMessage(), e);
        }
    }
}
