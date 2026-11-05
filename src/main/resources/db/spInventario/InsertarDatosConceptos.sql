-- ============================================================================
-- SCRIPT DE INICIALIZACION: TABLA PARAMETRICA CONCEPTOS (AlmacenWarnes)
-- Mapeo 1:1 con Frontend (mapeoEstados.ts) y Backend (Spring Boot)
-- ============================================================================

INSERT INTO conceptos (concepto_id, grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion)
VALUES
    -- Grupo 1: Roles de Usuario
    (1, 1, 0, 'Roles de Usuario', false, CURRENT_TIMESTAMP, 1),
    (2, 1, 1, 'Cajero', false, CURRENT_TIMESTAMP, 1),
    (3, 1, 2, 'Supervisor', false, CURRENT_TIMESTAMP, 1),
    (4, 1, 3, 'Propietario', false, CURRENT_TIMESTAMP, 1),

    -- Grupo 2: Categoría de Productos
    (5, 2, 0, 'Categoría de Productos', false, CURRENT_TIMESTAMP, 1),
    (6, 2, 1, 'Alimentos', false, CURRENT_TIMESTAMP, 1),
    (7, 2, 2, 'Bebidas', false, CURRENT_TIMESTAMP, 1),
    (8, 2, 3, 'Juguetes', false, CURRENT_TIMESTAMP, 1),

    -- Grupo 3: Estado de los productos
    (9, 3, 0, 'Estado de los productos', false, CURRENT_TIMESTAMP, 1),
    (10, 3, 1, 'Activo', false, CURRENT_TIMESTAMP, 1),
    (11, 3, 2, 'Descontinuado', false, CURRENT_TIMESTAMP, 1),
    (12, 3, 3, 'Suspendido', false, CURRENT_TIMESTAMP, 1),

    -- Grupo 4: Métodos de Pago
    (13, 4, 0, 'Métodos de Pago', false, CURRENT_TIMESTAMP, 1),
    (14, 4, 1, 'Efectivo', false, CURRENT_TIMESTAMP, 1),
    (15, 4, 2, 'QR', false, CURRENT_TIMESTAMP, 1),
    (16, 4, 3, 'Tarjeta', false, CURRENT_TIMESTAMP, 1),
    (17, 4, 4, 'Mixto', false, CURRENT_TIMESTAMP, 1),

    -- Grupo 5: Estado de las Ventas
    (18, 5, 0, 'Estado de las Ventas', false, CURRENT_TIMESTAMP, 1),
    (19, 5, 1, 'Pendiente', false, CURRENT_TIMESTAMP, 1),
    (20, 5, 2, 'Finalizada', false, CURRENT_TIMESTAMP, 1),
    (21, 5, 3, 'Anulada', false, CURRENT_TIMESTAMP, 1),

    -- Grupo 6: Tipo de Movimientos
    (22, 6, 0, 'Tipo de Movimientos', false, CURRENT_TIMESTAMP, 1),
    (23, 6, 1, 'Entrada', false, CURRENT_TIMESTAMP, 1),
    (24, 6, 2, 'Salida', false, CURRENT_TIMESTAMP, 1),

    -- Grupo 7: Estado de Inventario Pendiente
    (25, 7, 0, 'Estado de Inventario Pendiente', false, CURRENT_TIMESTAMP, 1),
    (26, 7, 1, 'Pendiente', false, CURRENT_TIMESTAMP, 1),
    (27, 7, 2, 'Aprobado', false, CURRENT_TIMESTAMP, 1),
    (28, 7, 3, 'Rechazado', false, CURRENT_TIMESTAMP, 1),
    (29, 7, 4, 'Observado / Para Corrección', false, CURRENT_TIMESTAMP, 1)
ON CONFLICT (concepto_id) DO UPDATE
SET grupo_id = EXCLUDED.grupo_id,
    codigo = EXCLUDED.codigo,
    descripcion = EXCLUDED.descripcion,
    marca_baja = EXCLUDED.marca_baja;

-- Sincronizar secuencia de conceptos de manera segura
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relkind = 'S' AND relname = 'conceptos_concepto_id_seq') THEN
        PERFORM setval('conceptos_concepto_id_seq', COALESCE((SELECT MAX(concepto_id) FROM conceptos), 1));
    END IF;
END $$;

