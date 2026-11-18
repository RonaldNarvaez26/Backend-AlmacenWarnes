-- ============================================================================================
-- AUTOR:           Kenneth Lopez / Ronald Narvaez
-- MODULO:          Notificaciones y Alertas Internas en Tiempo Real
-- DESCRIPCION:     Procedimientos para marcar notificaciones individuales o masivas como leídas
--                  en la tabla física oficial Notificaciones.
-- ============================================================================================

DROP PROCEDURE IF EXISTS sp_notificaciones_marcar_leido(INT, INT, BOOLEAN);
DROP PROCEDURE IF EXISTS sp_notificaciones_marcar_leido(INT, INT);
DROP PROCEDURE IF EXISTS sp_notificaciones_marcar_leido;

CREATE OR REPLACE PROCEDURE sp_notificaciones_marcar_leido(
    IN p_id_notificacion INT,
    IN p_usuario_id INT,
    OUT p_resultado BOOLEAN
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE notificaciones
    SET leido = TRUE,
        fecha_leido = CURRENT_TIMESTAMP
    WHERE id_notificacion = p_id_notificacion;

    p_resultado := FOUND;
END;
$$;

DROP PROCEDURE IF EXISTS sp_notificaciones_marcar_todas_leidas(INT, INT, INT);
DROP PROCEDURE IF EXISTS sp_notificaciones_marcar_todas_leidas(INT, INT);
DROP PROCEDURE IF EXISTS sp_notificaciones_marcar_todas_leidas;

CREATE OR REPLACE PROCEDURE sp_notificaciones_marcar_todas_leidas(
    IN p_usuario_id INT,
    IN p_rol_id INT,
    OUT p_afectados INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE notificaciones
    SET leido = TRUE,
        fecha_leido = CURRENT_TIMESTAMP
    WHERE leido = FALSE
      AND (
          usuario_destino = p_usuario_id
          OR (usuario_destino IS NULL AND (rol_destino IS NULL OR rol_destino = p_rol_id))
      );

    GET DIAGNOSTICS p_afectados = ROW_COUNT;
END;
$$;
