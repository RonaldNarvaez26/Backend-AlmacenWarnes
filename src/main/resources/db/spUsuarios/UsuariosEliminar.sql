-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     ELIMINAR LOGICAMENTE UN USUARIO DEL SISTEMA
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_usuarios_eliminar(
    p_usuario_id INT,
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_rol_id INT;
    v_marca_baja BOOLEAN;
    v_otros_propietarios_activos INT;
BEGIN
    IF p_usuario_id = p_usuario_modificacion THEN
        RAISE EXCEPTION 'No puede dar de baja su propia cuenta en sesión.';
    END IF;

    SELECT rol_id, marca_baja
      INTO v_rol_id, v_marca_baja
      FROM Usuarios
     WHERE usuario_id = p_usuario_id;

    IF NOT FOUND OR v_marca_baja = TRUE THEN
        RAISE EXCEPTION 'Usuario no encontrado o ya dado de baja (ID: %).', p_usuario_id;
    END IF;

    IF v_rol_id = 3 THEN
        SELECT COUNT(*)
          INTO v_otros_propietarios_activos
          FROM Usuarios
         WHERE rol_id = 3
           AND marca_baja = FALSE
           AND estado = 1
           AND usuario_id <> p_usuario_id;

        IF v_otros_propietarios_activos = 0 THEN
            RAISE EXCEPTION 'No se puede dar de baja al único propietario activo del sistema.';
        END IF;
    END IF;

    UPDATE Usuarios
    SET 
        marca_baja = true,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE usuario_id = p_usuario_id
      AND marca_baja = false;
END;
$$;