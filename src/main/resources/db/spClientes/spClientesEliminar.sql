-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     BAJA LÓGICA DE UN CLIENTE (marca_baja = true)
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_clientes_eliminar(
    p_cliente_id INT,
    p_usuario_modificacion INT DEFAULT 1
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Clientes WHERE cliente_id = p_cliente_id AND marca_baja = false) THEN
        RAISE EXCEPTION 'El cliente con ID % no existe o ya ha sido dado de baja.', p_cliente_id;
    END IF;

    UPDATE Clientes
    SET marca_baja = true,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE cliente_id = p_cliente_id;
END;
$$;
