-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     REGISTRAR UN MOVIMIENTO DE CAJA (ENTRADA O SALIDA) PENDIENTE DE ARQUEO
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_movimientos_insertar(
    p_sucursal_id INT,
    p_monto DECIMAL(8,2),
    p_tipo_movimiento INT,      -- Grupo 6: 1=Entrada, 2=Salida
    p_metodo_movimiento INT,    -- Grupo 4: 1=Efectivo, 2=QR, 3=Tarjeta, 4=Mixto, 5=Otro
    p_detalle VARCHAR(40),
    p_usuario_creacion INT,
    INOUT p_nuevo_movimiento_id INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_sucursal_id IS NULL OR p_sucursal_id <= 0 THEN
        RAISE EXCEPTION 'Debe especificar una sucursal válida para registrar el movimiento.';
    END IF;

    IF p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto del movimiento debe ser mayor a 0.';
    END IF;

    IF p_tipo_movimiento NOT IN (1, 2) THEN
        RAISE EXCEPTION 'Tipo de movimiento inválido (%). Use 1 (Entrada) o 2 (Salida).', p_tipo_movimiento;
    END IF;

    INSERT INTO Movimientos (
        sucursal_id, reporte_id, monto, tipo_movimiento, metodo_movimiento,
        detalle, marca_baja, fecha_creacion, usuario_creacion
    )
    VALUES (
        p_sucursal_id, NULL, p_monto, p_tipo_movimiento, p_metodo_movimiento,
        TRIM(p_detalle), false, CURRENT_TIMESTAMP, p_usuario_creacion
    )
    RETURNING movimiento_id INTO p_nuevo_movimiento_id;
END;
$$;