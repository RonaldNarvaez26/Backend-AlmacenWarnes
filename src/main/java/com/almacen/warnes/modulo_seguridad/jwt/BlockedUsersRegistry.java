package com.almacen.warnes.modulo_seguridad.jwt;

import com.almacen.warnes.modulo_seguridad.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caché en memoria (O(1)) de usuarios bloqueados (estado = 3) o dados de baja (marca_baja = true).
 * Evita consultar la base de datos en cada petición HTTP entrante.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockedUsersRegistry {

    private final UsuarioRepository usuarioRepository;
    private final Set<Integer> blockedUserIds = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        refreshFromDatabase();
    }

    public void refreshFromDatabase() {
        blockedUserIds.clear();
        blockedUserIds.addAll(usuarioRepository.findIdsBloqueadosOBaja());
        log.info("BlockedUsersRegistry inicializado con {} usuario(s) bloqueado(s)/inactivo(s).", blockedUserIds.size());
    }

    public boolean isBlocked(Integer usuarioId) {
        return usuarioId != null && blockedUserIds.contains(usuarioId);
    }

    public void markBlocked(Integer usuarioId) {
        if (usuarioId != null) {
            blockedUserIds.add(usuarioId);
        }
    }

    public void markActive(Integer usuarioId) {
        if (usuarioId != null) {
            blockedUserIds.remove(usuarioId);
        }
    }
}
