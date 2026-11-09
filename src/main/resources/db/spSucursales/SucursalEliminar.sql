-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     ELIMINAR LOGICAMENTE UNA SUCURSAL
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_sucursales_eliminar(
    p_sucursal_id INT,
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE Sucursales
    SET 
        marca_baja = true,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE sucursal_id = p_sucursal_id
      AND marca_baja = false;
END;
$$;