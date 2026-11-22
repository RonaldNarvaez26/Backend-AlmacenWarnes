package com.almacen.warnes.modulo_notificaciones.repository;

import com.almacen.warnes.modulo_notificaciones.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findByLeidoFalseOrderByFechaEmisionDesc();

    List<Notificacion> findByUsuarioDestinoAndLeidoFalseOrderByFechaEmisionDesc(Integer usuarioDestino);

    List<Notificacion> findByRolDestinoAndLeidoFalseOrderByFechaEmisionDesc(Integer rolDestino);

    @Query("SELECT n FROM Notificacion n WHERE n.leido = false AND " +
           "(n.usuarioDestino = :usuarioId OR (n.usuarioDestino IS NULL AND (n.rolDestino IS NULL OR n.rolDestino = :rolId))) " +
           "ORDER BY n.fechaEmision DESC")
    List<Notificacion> listarNotificacionesActivasParaUsuario(
            @Param("usuarioId") Integer usuarioId,
            @Param("rolId") Integer rolId
    );

    @Modifying
    @Query("UPDATE Notificacion n SET n.leido = true, n.fechaLeido = CURRENT_TIMESTAMP WHERE n.idNotificacion = :id")
    int marcarComoLeida(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leido = true, n.fechaLeido = CURRENT_TIMESTAMP " +
           "WHERE n.leido = false AND (n.usuarioDestino = :usuarioId OR (n.usuarioDestino IS NULL AND (n.rolDestino IS NULL OR n.rolDestino = :rolId)))")
    int marcarTodasComoLeidas(
            @Param("usuarioId") Integer usuarioId,
            @Param("rolId") Integer rolId
    );
}
