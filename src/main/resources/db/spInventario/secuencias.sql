-- ============================================================================
-- SCRIPT UNIFICADO DE SECUENCIAS AUTOINCREMENTABLES (AlmacenWarnes)
-- Asigna DEFAULT nextval() y sincroniza el contador al MAX(id) actual
-- Evita errores de "duplicate key value violates unique constraint" con JPA
-- ============================================================================

-- 1. PRODUCTOS
CREATE SEQUENCE IF NOT EXISTS productos_producto_id_seq;
ALTER TABLE productos ALTER COLUMN producto_id SET DEFAULT nextval('productos_producto_id_seq');
ALTER SEQUENCE productos_producto_id_seq OWNED BY productos.producto_id;
SELECT setval('productos_producto_id_seq', COALESCE((SELECT MAX(producto_id) FROM productos), 1));

-- 2. LOTES
CREATE SEQUENCE IF NOT EXISTS lotes_lote_id_seq;
ALTER TABLE lotes ALTER COLUMN lote_id SET DEFAULT nextval('lotes_lote_id_seq');
ALTER SEQUENCE lotes_lote_id_seq OWNED BY lotes.lote_id;
SELECT setval('lotes_lote_id_seq', COALESCE((SELECT MAX(lote_id) FROM lotes), 1));

-- 3. INVENTARIO
CREATE SEQUENCE IF NOT EXISTS inventario_inventario_id_seq;
ALTER TABLE inventario ALTER COLUMN inventario_id SET DEFAULT nextval('inventario_inventario_id_seq');
ALTER SEQUENCE inventario_inventario_id_seq OWNED BY inventario.inventario_id;
SELECT setval('inventario_inventario_id_seq', COALESCE((SELECT MAX(inventario_id) FROM inventario), 1));

-- 4. INVENTARIO_PENDIENTES (SALA DE ESPERA)
CREATE SEQUENCE IF NOT EXISTS inventario_pendientes_inventario_pendiente_id_seq;
ALTER TABLE inventario_pendientes ALTER COLUMN inventario_pendiente_id SET DEFAULT nextval('inventario_pendientes_inventario_pendiente_id_seq');
ALTER SEQUENCE inventario_pendientes_inventario_pendiente_id_seq OWNED BY inventario_pendientes.inventario_pendiente_id;
SELECT setval('inventario_pendientes_inventario_pendiente_id_seq', COALESCE((SELECT MAX(inventario_pendiente_id) FROM inventario_pendientes), 1));

-- 5. PROVEEDORES
CREATE SEQUENCE IF NOT EXISTS proveedores_proveedor_id_seq;
ALTER TABLE proveedores ALTER COLUMN proveedor_id SET DEFAULT nextval('proveedores_proveedor_id_seq');
ALTER SEQUENCE proveedores_proveedor_id_seq OWNED BY proveedores.proveedor_id;
SELECT setval('proveedores_proveedor_id_seq', COALESCE((SELECT MAX(proveedor_id) FROM proveedores), 1));

-- 6. CONCEPTOS
CREATE SEQUENCE IF NOT EXISTS conceptos_concepto_id_seq;
ALTER TABLE conceptos ALTER COLUMN concepto_id SET DEFAULT nextval('conceptos_concepto_id_seq');
ALTER SEQUENCE conceptos_concepto_id_seq OWNED BY conceptos.concepto_id;
SELECT setval('conceptos_concepto_id_seq', COALESCE((SELECT MAX(concepto_id) FROM conceptos), 1));

-- 7. USUARIOS
CREATE SEQUENCE IF NOT EXISTS usuarios_usuario_id_seq;
ALTER TABLE usuarios ALTER COLUMN usuario_id SET DEFAULT nextval('usuarios_usuario_id_seq');
ALTER SEQUENCE usuarios_usuario_id_seq OWNED BY usuarios.usuario_id;
SELECT setval('usuarios_usuario_id_seq', COALESCE((SELECT MAX(usuario_id) FROM usuarios), 1));

-- 8. CLIENTES
CREATE SEQUENCE IF NOT EXISTS clientes_cliente_id_seq;
ALTER TABLE clientes ALTER COLUMN cliente_id SET DEFAULT nextval('clientes_cliente_id_seq');
ALTER SEQUENCE clientes_cliente_id_seq OWNED BY clientes.cliente_id;
SELECT setval('clientes_cliente_id_seq', COALESCE((SELECT MAX(cliente_id) FROM clientes), 1));

-- 9. VENTAS Y DETALLES
CREATE SEQUENCE IF NOT EXISTS ventas_venta_id_seq;
ALTER TABLE ventas ALTER COLUMN venta_id SET DEFAULT nextval('ventas_venta_id_seq');
ALTER SEQUENCE ventas_venta_id_seq OWNED BY ventas.venta_id;
SELECT setval('ventas_venta_id_seq', COALESCE((SELECT MAX(venta_id) FROM ventas), 1));

CREATE SEQUENCE IF NOT EXISTS detalles_ventas_detalle_venta_id_seq;
ALTER TABLE detalles_ventas ALTER COLUMN detalle_venta_id SET DEFAULT nextval('detalles_ventas_detalle_venta_id_seq');
ALTER SEQUENCE detalles_ventas_detalle_venta_id_seq OWNED BY detalles_ventas.detalle_venta_id;
SELECT setval('detalles_ventas_detalle_venta_id_seq', COALESCE((SELECT MAX(detalle_venta_id) FROM detalles_ventas), 1));

-- 10. REPORTES Y MOVIMIENTOS FINANCIEROS
CREATE SEQUENCE IF NOT EXISTS reportes_reporte_id_seq;
ALTER TABLE reportes ALTER COLUMN reporte_id SET DEFAULT nextval('reportes_reporte_id_seq');
ALTER SEQUENCE reportes_reporte_id_seq OWNED BY reportes.reporte_id;
SELECT setval('reportes_reporte_id_seq', COALESCE((SELECT MAX(reporte_id) FROM reportes), 1));

CREATE SEQUENCE IF NOT EXISTS entradas_entrada_id_seq;
ALTER TABLE entradas ALTER COLUMN entrada_id SET DEFAULT nextval('entradas_entrada_id_seq');
ALTER SEQUENCE entradas_entrada_id_seq OWNED BY entradas.entrada_id;
SELECT setval('entradas_entrada_id_seq', COALESCE((SELECT MAX(entrada_id) FROM entradas), 1));

CREATE SEQUENCE IF NOT EXISTS salidas_salida_id_seq;
ALTER TABLE salidas ALTER COLUMN salida_id SET DEFAULT nextval('salidas_salida_id_seq');
ALTER SEQUENCE salidas_salida_id_seq OWNED BY salidas.salida_id;
SELECT setval('salidas_salida_id_seq', COALESCE((SELECT MAX(salida_id) FROM salidas), 1));

CREATE SEQUENCE IF NOT EXISTS movimientos_movimiento_id_seq;
ALTER TABLE movimientos ALTER COLUMN movimiento_id SET DEFAULT nextval('movimientos_movimiento_id_seq');
ALTER SEQUENCE movimientos_movimiento_id_seq OWNED BY movimientos.movimiento_id;
SELECT setval('movimientos_movimiento_id_seq', COALESCE((SELECT MAX(movimiento_id) FROM movimientos), 1));

