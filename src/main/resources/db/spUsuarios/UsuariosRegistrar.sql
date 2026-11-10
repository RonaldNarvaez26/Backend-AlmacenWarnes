-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez / Kenneth Lopez
-- DESCRIPCION:     CREAR UN NUEVO USUARIO INTERNO Y DEVOLVER SU ID
-- NOTA:            El parámetro p_password_hash ya debe venir cifrado por Spring Boot
-- ============================================================================================

DROP PROCEDURE IF EXISTS sp_usuarios_insertar;
DROP PROCEDURE IF EXISTS sp_usuarios_insertar(VARCHAR, INT, VARCHAR, VARCHAR, INT, VARCHAR, INT);

CREATE OR REPLACE PROCEDURE sp_usuarios_insertar(
    IN p_usuario VARCHAR,
    IN p_sucursal_id INT, 
    IN p_nombres VARCHAR,
    IN p_apellidos VARCHAR,
    IN p_rol_id INT,      
    IN p_password_hash VARCHAR,
    IN p_usuario_creacion INT,
    OUT p_usuario_id INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuarios WHERE usuario = p_usuario) THEN
        RAISE EXCEPTION 'El nombre de usuario % ya está registrado.', p_usuario;
    END IF;

    INSERT INTO usuarios (
        usuario, sucursal_id, nombres, apellidos, rol_id, password, 
        estado, marca_baja, fecha_creacion, usuario_creacion
    )
    VALUES (
        p_usuario, COALESCE(p_sucursal_id, 0), p_nombres, p_apellidos, p_rol_id, p_password_hash, 
        1, FALSE, CURRENT_TIMESTAMP, p_usuario_creacion
    )
    RETURNING usuarios.usuario_id INTO p_usuario_id;
END;
$$;
