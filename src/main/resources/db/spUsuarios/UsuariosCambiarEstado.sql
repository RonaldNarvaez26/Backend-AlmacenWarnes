-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez / Kenneth Lopez
-- DESCRIPCION:     BLOQUEAR (estado = 3) O DESBLOQUEAR/ACTIVAR (estado = 1) UN USUARIO
-- ============================================================================================

DROP PROCEDURE IF EXISTS sp_usuarios_cambiar_estado(INT, INT, INT);

CREATE OR REPLACE PROCEDURE sp_usuarios_cambiar_estado(
    p_usuario_id INT,
    p_nuevo_estado INT,
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_rol_id INT;
    v_marca_baja BOOLEAN;
    v_otros_propietarios_activos INT;
BEGIN
    IF p_nuevo_estado NOT IN (1, 3) THEN
        RAISE EXCEPTION 'Estado no válido (%). Solo se permite Activo (1) o Bloqueado (3).', p_nuevo_estado;
    END IF;

    IF p_usuario_id = p_usuario_modificacion THEN
        RAISE EXCEPTION 'No puede bloquear ni cambiar el estado de su propia cuenta en sesión.';
    END IF;

    SELECT rol_id, marca_baja
      INTO v_rol_id, v_marca_baja
      FROM usuarios
     WHERE usuario_id = p_usuario_id;

    IF NOT FOUND OR v_marca_baja = TRUE THEN
        RAISE EXCEPTION 'Usuario no encontrado o ya dado de baja (ID: %).', p_usuario_id;
    END IF;

    IF p_nuevo_estado = 3 AND v_rol_id = 3 THEN
        SELECT COUNT(*)
          INTO v_otros_propietarios_activos
          FROM usuarios
         WHERE rol_id = 3
           AND marca_baja = FALSE
           AND estado = 1
           AND usuario_id <> p_usuario_id;

        IF v_otros_propietarios_activos = 0 THEN
            RAISE EXCEPTION 'No se puede bloquear al único propietario activo del sistema.';
        END IF;
    END IF;

    UPDATE usuarios
       SET estado = p_nuevo_estado,
           fecha_modificacion = CURRENT_TIMESTAMP,
           usuario_modificacion = p_usuario_modificacion
     WHERE usuario_id = p_usuario_id
       AND marca_baja = FALSE;
END;
$$;
