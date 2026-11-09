-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     MODIFICAR DATOS DE UNA SUCURSAL EXISTENTE
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_sucursales_modificar(
    p_sucursal_id INT,
    p_codigo_interno VARCHAR,
    p_ciudad VARCHAR,
    p_direccion VARCHAR,
    p_telefono VARCHAR,
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE Sucursales
    SET 
        codigo_interno = COALESCE(p_codigo_interno, codigo_interno),
        ciudad = COALESCE(p_ciudad, ciudad),
        direccion = COALESCE(p_direccion, direccion),
        telefono = COALESCE(p_telefono, telefono),
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE sucursal_id = p_sucursal_id
      AND marca_baja = false;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Sucursal no encontrada o dada de baja (ID: %)', p_sucursal_id;
    END IF;
END;
$$;