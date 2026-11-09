-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     CREAR UNA NUEVA SUCURSAL
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_sucursales_insertar(
    p_codigo_interno VARCHAR,
    p_ciudad VARCHAR,
    p_direccion VARCHAR,
    p_telefono VARCHAR,
    p_usuario_creacion INT,
    INOUT p_nueva_sucursal_id INT DEFAULT NULL
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
    RETURNING sucursal_id INTO p_nueva_sucursal_id;
END;
$$;