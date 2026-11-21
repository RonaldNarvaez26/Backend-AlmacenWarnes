-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     ANULA UNA VENTA Y REVIERTE EL STOCK A LOS LOTES CORRESPONDIENTES
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_ventas_anular(
    p_venta_id INT,
    p_usuario_anulacion INT -- ID del Supervisor/Propietario
)
LANGUAGE plpgsql
AS $$
DECLARE
    r_detalle RECORD;
BEGIN
    -- 1. Validar que la venta exista y no esté ya anulada
    IF EXISTS (SELECT 1 FROM Ventas WHERE venta_id = p_venta_id AND estado = 3) THEN
        RAISE EXCEPTION 'La venta % ya se encuentra anulada.', p_venta_id;
    END IF;

    -- 2. Cambiar el estado de la venta a 3 (Anulada)
    UPDATE Ventas
    SET 
        estado = 3,
        anulado_por = p_usuario_anulacion,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_anulacion
    WHERE venta_id = p_venta_id;

    -- 3. Iterar sobre los detalles de esa venta para devolver el stock
    FOR r_detalle IN 
        SELECT inventario_id, cantidad 
        FROM Detalles_Ventas 
        WHERE venta_id = p_venta_id AND marca_baja = false
    LOOP
        UPDATE Inventario
        SET 
            stock = stock + r_detalle.cantidad,
            fecha_modificacion = CURRENT_TIMESTAMP,
            usuario_modificacion = p_usuario_anulacion
        WHERE inventario_id = r_detalle.inventario_id;
    END LOOP;
END;
$$;