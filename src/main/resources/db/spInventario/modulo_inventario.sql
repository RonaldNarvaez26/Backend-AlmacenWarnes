-- ============================================================================
-- SCRIPT DE MIGRACION Y PARCHE: MODULO DE INVENTARIO (AlmacenWarnes)
-- Basado en warnes_db_create.sql (Redgate Data Modeler)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. PARCHES DDL (ALTER TABLE, COLUMNAS FALTANTES Y AUTOINCREMENTO)
-- ----------------------------------------------------------------------------

-- 1.1 Columnas complementarias en inventario_pendientes para soportar lotes al aprobar
ALTER TABLE inventario_pendientes ADD COLUMN IF NOT EXISTS proveedor_id INT NULL REFERENCES proveedores(proveedor_id);
ALTER TABLE inventario_pendientes ADD COLUMN IF NOT EXISTS descripcion_lote VARCHAR(100) NULL;
ALTER TABLE inventario_pendientes ADD COLUMN IF NOT EXISTS precio_lote DECIMAL(8,2) NULL;
ALTER TABLE inventario_pendientes ADD COLUMN IF NOT EXISTS fecha_vencimiento DATE NULL;

-- 1.2 Permitir codigo de barras vacio o nulo si el producto no cuenta con codigo fisico inicial
ALTER TABLE productos ALTER COLUMN codigo_barras DROP NOT NULL;

-- 1.3 Secuencias autoincrementales vinculadas (para soportar RETURNING en SPs y JPA GenerationType.IDENTITY)
CREATE SEQUENCE IF NOT EXISTS productos_producto_id_seq;
ALTER TABLE productos ALTER COLUMN producto_id SET DEFAULT nextval('productos_producto_id_seq');
ALTER SEQUENCE productos_producto_id_seq OWNED BY productos.producto_id;
SELECT setval('productos_producto_id_seq', COALESCE((SELECT MAX(producto_id) FROM productos), 1));

CREATE SEQUENCE IF NOT EXISTS lotes_lote_id_seq;
ALTER TABLE lotes ALTER COLUMN lote_id SET DEFAULT nextval('lotes_lote_id_seq');
ALTER SEQUENCE lotes_lote_id_seq OWNED BY lotes.lote_id;
SELECT setval('lotes_lote_id_seq', COALESCE((SELECT MAX(lote_id) FROM lotes), 1));

CREATE SEQUENCE IF NOT EXISTS inventario_inventario_id_seq;
ALTER TABLE inventario ALTER COLUMN inventario_id SET DEFAULT nextval('inventario_inventario_id_seq');
ALTER SEQUENCE inventario_inventario_id_seq OWNED BY inventario.inventario_id;
SELECT setval('inventario_inventario_id_seq', COALESCE((SELECT MAX(inventario_id) FROM inventario), 1));

CREATE SEQUENCE IF NOT EXISTS inventario_pendientes_inventario_pendiente_id_seq;
ALTER TABLE inventario_pendientes ALTER COLUMN inventario_pendiente_id SET DEFAULT nextval('inventario_pendientes_inventario_pendiente_id_seq');
ALTER SEQUENCE inventario_pendientes_inventario_pendiente_id_seq OWNED BY inventario_pendientes.inventario_pendiente_id;
SELECT setval('inventario_pendientes_inventario_pendiente_id_seq', COALESCE((SELECT MAX(inventario_pendiente_id) FROM inventario_pendientes), 1));

CREATE SEQUENCE IF NOT EXISTS proveedores_proveedor_id_seq;
ALTER TABLE proveedores ALTER COLUMN proveedor_id SET DEFAULT nextval('proveedores_proveedor_id_seq');
ALTER SEQUENCE proveedores_proveedor_id_seq OWNED BY proveedores.proveedor_id;
SELECT setval('proveedores_proveedor_id_seq', COALESCE((SELECT MAX(proveedor_id) FROM proveedores), 1));

CREATE SEQUENCE IF NOT EXISTS conceptos_concepto_id_seq;
ALTER TABLE conceptos ALTER COLUMN concepto_id SET DEFAULT nextval('conceptos_concepto_id_seq');
ALTER SEQUENCE conceptos_concepto_id_seq OWNED BY conceptos.concepto_id;
SELECT setval('conceptos_concepto_id_seq', COALESCE((SELECT MAX(concepto_id) FROM conceptos), 1));

-- ----------------------------------------------------------------------------
-- 2. FUNCIONES DE LECTURA (4 FUNCTIONS)
-- ----------------------------------------------------------------------------

-- 2.1 Generar codigo interno correlativo por prefijo
CREATE OR REPLACE FUNCTION spProductosGenerarCodigoInterno(p_prefijo VARCHAR)
RETURNS VARCHAR AS $$
DECLARE
    v_prefijo_limpio VARCHAR(10);
    v_ultimo_numero INT;
    v_nuevo_codigo VARCHAR(10);
BEGIN
    v_prefijo_limpio := UPPER(TRIM(COALESCE(p_prefijo, 'PRO')));
    
    SELECT COALESCE(MAX(
        CAST(SUBSTRING(codigo_interno FROM LENGTH(v_prefijo_limpio) + 1) AS INTEGER)
    ), 0)
    INTO v_ultimo_numero
    FROM productos
    WHERE codigo_interno LIKE v_prefijo_limpio || '%'
      AND SUBSTRING(codigo_interno FROM LENGTH(v_prefijo_limpio) + 1) ~ '^[0-9]+$';

    v_nuevo_codigo := v_prefijo_limpio || LPAD((v_ultimo_numero + 1)::TEXT, 3, '0');
    RETURN v_nuevo_codigo;
END;
$$ LANGUAGE plpgsql;

-- 2.2 Resumen cuantitativo / KPIs
CREATE OR REPLACE FUNCTION spInventarioKpiResumen()
RETURNS TABLE (
    total_productos_catalogo BIGINT,
    unidades_stock_activo BIGINT,
    lotes_sala_espera BIGINT,
    alertas_vencimiento BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        (SELECT COUNT(DISTINCT p.producto_id)
         FROM productos p
         WHERE p.marca_baja = FALSE AND p.estado = 1)::BIGINT AS total_productos_catalogo,

        (SELECT COALESCE(SUM(i.stock), 0)
         FROM inventario i
         WHERE i.marca_baja = FALSE AND i.sucursal_id = 1)::BIGINT AS unidades_stock_activo,

        (SELECT COUNT(ip.inventario_pendiente_id)
         FROM inventario_pendientes ip
         WHERE ip.marca_baja = FALSE AND ip.estado = 1 AND ip.sucursal_id = 1)::BIGINT AS lotes_sala_espera,

        (SELECT COUNT(i.inventario_id)
         FROM inventario i
         INNER JOIN lotes l ON i.lote_id = l.lote_id
         WHERE i.marca_baja = FALSE
           AND l.marca_baja = FALSE
           AND i.sucursal_id = 1
           AND i.stock > 0
           AND l.fecha_vencimiento IS NOT NULL
           AND l.fecha_vencimiento <= (CURRENT_DATE + INTERVAL '7 days'))::BIGINT AS alertas_vencimiento;
END;
$$ LANGUAGE plpgsql;

-- 2.3 Listado detallado de lotes en inventario (con #variable_conflict use_column)
CREATE OR REPLACE FUNCTION spInventarioLotesListar()
RETURNS TABLE (
    inventario_id INT,
    stock INT,
    en_descuento BOOLEAN,
    precio_descuento DECIMAL(8,2),
    producto_id INT,
    codigo_interno VARCHAR,
    codigo_barras VARCHAR,
    nombre_producto VARCHAR,
    precio_venta DECIMAL(8,2),
    estado_producto INT,
    lote_id INT,
    descripcion_lote VARCHAR,
    costo_unitario DECIMAL(8,2),
    fecha_vencimiento DATE,
    nombre_proveedor VARCHAR,
    dias_para_vencer INT
) AS $$
#variable_conflict use_column
BEGIN
    RETURN QUERY
    SELECT
        i.inventario_id,
        i.stock,
        i.en_descuento,
        i.precio_descuento,
        p.producto_id,
        p.codigo_interno,
        p.codigo_barras,
        p.nombre_producto,
        p.precio_venta,
        p.estado AS estado_producto,
        l.lote_id,
        l.descripcion AS descripcion_lote,
        l.costo_unitario,
        l.fecha_vencimiento,
        COALESCE(pr.nombre_empresa, 'Sin Proveedor') AS nombre_proveedor,
        CASE
            WHEN l.fecha_vencimiento IS NOT NULL THEN (l.fecha_vencimiento - CURRENT_DATE)::INT
            ELSE NULL
        END AS dias_para_vencer
    FROM inventario i
    INNER JOIN productos p ON i.producto_id = p.producto_id
    INNER JOIN lotes l ON i.lote_id = l.lote_id
    LEFT JOIN proveedores pr ON l.proveedor_id = pr.proveedor_id
    WHERE i.marca_baja = FALSE
      AND i.sucursal_id = 1
      AND p.marca_baja = FALSE
      AND l.marca_baja = FALSE
    ORDER BY l.fecha_vencimiento ASC NULLS LAST, i.inventario_id DESC;
END;
$$ LANGUAGE plpgsql;

-- 2.4 Catalogo consolidado de productos con stock acumulado (con #variable_conflict use_column)
CREATE OR REPLACE FUNCTION spProductosListarCatalogo()
RETURNS TABLE (
    producto_id INT,
    codigo_interno VARCHAR,
    codigo_barras VARCHAR,
    nombre_producto VARCHAR,
    categoria_id INT,
    precio_compra DECIMAL(8,2),
    precio_venta DECIMAL(8,2),
    estado INT,
    stock_total_acumulado BIGINT
) AS $$
#variable_conflict use_column
BEGIN
    RETURN QUERY
    SELECT
        p.producto_id,
        p.codigo_interno,
        p.codigo_barras,
        p.nombre_producto,
        p.categoria_id,
        p.precio_compra,
        p.precio_venta,
        p.estado,
        COALESCE(SUM(i.stock), 0)::BIGINT AS stock_total_acumulado
    FROM productos p
    LEFT JOIN inventario i ON p.producto_id = i.producto_id AND i.marca_baja = FALSE AND i.sucursal_id = 1
    WHERE p.marca_baja = FALSE
    GROUP BY p.producto_id, p.codigo_interno, p.codigo_barras, p.nombre_producto, p.categoria_id, p.precio_compra, p.precio_venta, p.estado
    ORDER BY p.nombre_producto ASC;
END;
$$ LANGUAGE plpgsql;

-- ----------------------------------------------------------------------------
-- 3. PROCEDIMIENTOS ALMACENADOS DE ESCRITURA (2 PROCEDURES)
-- ----------------------------------------------------------------------------

-- 3.1 Aprobar ingreso de Sala de Espera (WAR-18)
CREATE OR REPLACE PROCEDURE spInventarioPendientesAprobar(
    p_inventario_pendiente_id INT,
    p_usuario_revision INT
)
LANGUAGE plpgsql AS $$
DECLARE
    v_reg RECORD;
    v_producto_id INT;
    v_lote_id INT;
BEGIN
    SELECT * INTO v_reg
    FROM inventario_pendientes
    WHERE inventario_pendiente_id = p_inventario_pendiente_id
      AND marca_baja = FALSE
      AND estado = 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El registro pendiente con ID % no existe o ya fue procesado', p_inventario_pendiente_id;
    END IF;

    -- Validar colision de codigo de barras en otro producto activo
    IF v_reg.codigo_barras IS NOT NULL AND TRIM(v_reg.codigo_barras) <> '' THEN
        IF EXISTS (
            SELECT 1 FROM productos
            WHERE codigo_barras = TRIM(v_reg.codigo_barras)
              AND marca_baja = FALSE
              AND (v_reg.producto_id IS NULL OR producto_id <> v_reg.producto_id)
        ) THEN
            RAISE EXCEPTION 'El codigo de barras % ya se encuentra registrado en otro producto', v_reg.codigo_barras;
        END IF;
    END IF;

    -- Resolver ID de producto existente o nuevo
    v_producto_id := v_reg.producto_id;
    IF v_producto_id IS NULL AND v_reg.codigo_barras IS NOT NULL AND TRIM(v_reg.codigo_barras) <> '' THEN
        SELECT producto_id INTO v_producto_id
        FROM productos
        WHERE codigo_barras = TRIM(v_reg.codigo_barras) AND marca_baja = FALSE
        LIMIT 1;
    END IF;

    IF v_producto_id IS NULL THEN
        SELECT producto_id INTO v_producto_id
        FROM productos
        WHERE codigo_interno = v_reg.codigo_interno AND marca_baja = FALSE
        LIMIT 1;
    END IF;

    -- Insertar o actualizar producto maestro
    IF v_producto_id IS NULL THEN
        INSERT INTO productos (
            categoria_id, codigo_barras, nombre_producto, codigo_interno,
            precio_compra, precio_venta, estado, marca_baja, fecha_creacion, usuario_creacion
        ) VALUES (
            v_reg.categoria_id,
            COALESCE(TRIM(v_reg.codigo_barras), ''),
            v_reg.nombre_producto,
            v_reg.codigo_interno,
            v_reg.precio_compra,
            v_reg.precio_venta,
            1,
            FALSE,
            CURRENT_TIMESTAMP,
            v_reg.usuario_creacion
        ) RETURNING producto_id INTO v_producto_id;
    ELSE
        -- Actualizar datos maestros por si el supervisor corrigio errores del cajero (WAR-18)
        UPDATE productos
        SET nombre_producto = v_reg.nombre_producto,
            categoria_id = v_reg.categoria_id,
            codigo_barras = COALESCE(TRIM(v_reg.codigo_barras), ''),
            precio_compra = v_reg.precio_compra,
            precio_venta = v_reg.precio_venta,
            fecha_modificacion = CURRENT_TIMESTAMP,
            usuario_modificacion = p_usuario_revision
        WHERE producto_id = v_producto_id AND marca_baja = FALSE;
    END IF;

    -- Insertar lote garantizando NOT NULL en precio_lote y descripcion
    INSERT INTO lotes (
        proveedor_id, descripcion, cantidad, precio_lote, costo_unitario,
        fecha_vencimiento, marca_baja, fecha_creacion, usuario_creacion
    ) VALUES (
        v_reg.proveedor_id,
        COALESCE(v_reg.descripcion_lote, 'Lote ' || v_reg.codigo_interno),
        v_reg.stock_ingresado,
        COALESCE(v_reg.precio_lote, v_reg.precio_compra * v_reg.stock_ingresado),
        v_reg.precio_compra,
        v_reg.fecha_vencimiento,
        FALSE,
        CURRENT_TIMESTAMP,
        v_reg.usuario_creacion
    ) RETURNING lote_id INTO v_lote_id;

    -- Insertar en inventario de sucursal
    INSERT INTO inventario (
        sucursal_id, lote_id, producto_id, stock, en_descuento,
        precio_descuento, marca_baja, fecha_creacion, usuario_creacion
    ) VALUES (
        v_reg.sucursal_id, v_lote_id, v_producto_id, v_reg.stock_ingresado, FALSE,
        NULL, FALSE, CURRENT_TIMESTAMP, v_reg.usuario_creacion
    );

    -- Actualizar estado a 2 (Aprobado)
    UPDATE inventario_pendientes
    SET estado = 2,
        producto_id = v_producto_id,
        fecha_revision = CURRENT_TIMESTAMP,
        usuario_revision = p_usuario_revision
    WHERE inventario_pendiente_id = p_inventario_pendiente_id;
END;
$$;

-- 3.2 Ingreso directo por Supervisor (omite Sala de Espera)
CREATE OR REPLACE PROCEDURE spInventarioIngresoDirectoSupervisor(
    p_producto_id INT, 
    p_categoria_id INT, 
    p_nombre_producto VARCHAR(100),
    p_codigo_barras VARCHAR(100), 
    p_codigo_interno VARCHAR(10), 
    p_proveedor_id INT,
    p_descripcion_lote VARCHAR(100), 
    p_stock_ingresado INT, 
    p_precio_lote DECIMAL(8,2),
    p_precio_compra DECIMAL(8,2), 
    p_precio_venta DECIMAL(8,2), 
    p_fecha_vencimiento DATE,
    p_usuario_creacion INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nuevo_lote_id INT;
    v_sucursal_fija INT := 1;
BEGIN
    IF p_producto_id IS NULL AND p_codigo_barras IS NOT NULL AND TRIM(p_codigo_barras) <> '' THEN
        SELECT producto_id INTO p_producto_id 
        FROM productos 
        WHERE codigo_barras = TRIM(p_codigo_barras) AND marca_baja = FALSE 
        LIMIT 1;
    END IF;

    IF p_producto_id IS NULL THEN
        INSERT INTO productos 
            (categoria_id, codigo_barras, nombre_producto, codigo_interno, precio_compra, precio_venta, estado, fecha_creacion, usuario_creacion, marca_baja)
        VALUES 
            (p_categoria_id, COALESCE(TRIM(p_codigo_barras), ''), p_nombre_producto, p_codigo_interno, p_precio_compra, p_precio_venta, 1, CURRENT_TIMESTAMP, p_usuario_creacion, FALSE)
        RETURNING producto_id INTO p_producto_id;
    ELSE
        IF p_codigo_barras IS NOT NULL AND TRIM(p_codigo_barras) <> '' AND EXISTS (
            SELECT 1 FROM productos 
            WHERE codigo_barras = TRIM(p_codigo_barras) AND producto_id <> p_producto_id AND marca_baja = FALSE
        ) THEN
            RAISE EXCEPTION 'El codigo de barras ya pertenece a otro producto distinto.';
        END IF;

        UPDATE productos
        SET nombre_producto = p_nombre_producto, 
            categoria_id = p_categoria_id, 
            codigo_barras = COALESCE(TRIM(p_codigo_barras), ''),
            precio_compra = p_precio_compra, 
            precio_venta = p_precio_venta, 
            fecha_modificacion = CURRENT_TIMESTAMP, 
            usuario_modificacion = p_usuario_creacion
        WHERE producto_id = p_producto_id AND marca_baja = FALSE;
    END IF;

    INSERT INTO lotes
        (proveedor_id, descripcion, cantidad, precio_lote, costo_unitario, fecha_vencimiento, fecha_creacion, usuario_creacion, marca_baja)
    VALUES
        (p_proveedor_id, 
         COALESCE(p_descripcion_lote, 'Lote ' || p_codigo_interno), 
         p_stock_ingresado, 
         COALESCE(p_precio_lote, p_precio_compra * p_stock_ingresado), 
         p_precio_compra, 
         p_fecha_vencimiento, 
         CURRENT_TIMESTAMP, 
         p_usuario_creacion, 
         FALSE)
    RETURNING lote_id INTO v_nuevo_lote_id;

    INSERT INTO inventario
        (sucursal_id, lote_id, producto_id, stock, en_descuento, fecha_creacion, usuario_creacion, marca_baja)
    VALUES
        (v_sucursal_fija, v_nuevo_lote_id, p_producto_id, p_stock_ingresado, FALSE, CURRENT_TIMESTAMP, p_usuario_creacion, FALSE);
END;
$$;
