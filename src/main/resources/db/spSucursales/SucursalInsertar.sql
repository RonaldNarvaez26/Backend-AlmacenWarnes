-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     CREAR UNA NUEVA SUCURSAL
-- ============================================================================================
DROP PROCEDURE IF EXISTS sp_sucursales_insertar;
DROP PROCEDURE IF EXISTS sp_sucursales_insertar(VARCHAR, VARCHAR, VARCHAR, VARCHAR, INT, INT);
DROP PROCEDURE IF EXISTS sp_sucursales_insertar(VARCHAR, VARCHAR, VARCHAR, VARCHAR, INT);

CREATE OR REPLACE PROCEDURE sp_sucursales_insertar(
    IN p_codigo_interno VARCHAR,
    IN p_ciudad VARCHAR,
    IN p_direccion VARCHAR,
    IN p_telefono VARCHAR,
    IN p_usuario_creacion INT,
    OUT p_sucursal_id INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM Sucursales WHERE codigo_interno = p_codigo_interno AND marca_baja = false) THEN
        RAISE EXCEPTION 'El código de sucursal % ya existe.', p_codigo_interno;
    END IF;

    INSERT INTO Sucursales (
        codigo_interno, ciudad, direccion, telefono, 
        marca_baja, fecha_creacion, usuario_creacion
    )
    VALUES (
        p_codigo_interno, p_ciudad, p_direccion, p_telefono, 
        false, CURRENT_TIMESTAMP, p_usuario_creacion
    )
    RETURNING sucursales.sucursal_id INTO p_sucursal_id;
END;
$$;