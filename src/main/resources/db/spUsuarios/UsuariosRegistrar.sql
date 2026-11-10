-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     CREAR UN NUEVO USUARIO INTERNO Y DEVOLVER SU ID
-- NOTA:            El parámetro p_password_hash ya debe venir cifrado por Spring Boot
-- ============================================================================================

CREATE OR REPLACE PROCEDURE sp_usuarios_insertar(
    p_usuario VARCHAR,
	p_sucursal_id INT, 
    p_nombres VARCHAR,
    p_apellidos VARCHAR,
    p_rol_id INT,      
    p_password_hash VARCHAR,
    p_usuario_creacion INT,
    INOUT p_nuevo_usuario_id INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM Usuarios WHERE usuario = p_usuario) THEN
        RAISE EXCEPTION 'El nombre de usuario % ya está registrado.', p_usuario;
    END IF;

    INSERT INTO Usuarios (
        usuario, sucursal_id, nombres, apellidos, rol_id, password, 
        estado, marca_baja, fecha_creacion, usuario_creacion
    )
    VALUES (
        p_usuario, p_sucursal_id, p_nombres, p_apellidos, p_rol_id, p_password_hash, 
        1, false, CURRENT_TIMESTAMP, p_usuario_creacion
    )
    RETURNING usuario_id INTO p_nuevo_usuario_id;
END;
$$;
