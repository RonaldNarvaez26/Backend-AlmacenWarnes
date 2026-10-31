-- ============================================================================
-- SCRIPT DE DATOS DE PRUEBA (SEED DATA) - ALMACEN WARNES
-- Adaptado 100% a warnes_db_create.sql + Parche de Inventario_Pendientes
-- ============================================================================

-- 1. ESQUEMA ESTRICTO ORIGINAL (SIN ALTER TABLE DDL)

-- 2. TABLA: CONCEPTOS (Catalogos parametricos del sistema)
-- Grupos: 1=Roles, 2=Categorias Productos, 3=Estados Producto, 4=Metodos Pago, 5=Estados Venta, 6=Tipos Movimiento, 7=Estados Sala Espera
INSERT INTO conceptos (concepto_id, grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion) VALUES
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

-- 3. TABLA: SUCURSALES (Sucursal unica fija = 1)
INSERT INTO sucursales (sucursal_id, codigo_interno, ciudad, direccion, telefono, marca_baja, fecha_creacion, usuario_creacion) VALUES
(1, 'SUC-001', 'La Paz', 'Calle Warnes #450, Zona Central', '22445566', FALSE, CURRENT_TIMESTAMP, 1)
ON CONFLICT (sucursal_id) DO NOTHING;

-- 4. TABLA: USUARIOS (3 roles principales para pruebas)
INSERT INTO usuarios (usuario_id, usuario, nombres, apellidos, rol_id, password, marca_baja, fecha_creacion, usuario_creacion) VALUES
(1, 'kenneth.admin', 'Kenneth Gabriel', 'Lopez Castillo', 3, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', FALSE, CURRENT_TIMESTAMP, 1),
(2, 'carlos.sup',    'Carlos Eduardo',  'Mendoza Quispe', 2, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', FALSE, CURRENT_TIMESTAMP, 1),
(3, 'ana.cajera',    'Ana Maria',       'Flores Condori', 1, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', FALSE, CURRENT_TIMESTAMP, 1),
(4, 'luis.cajero',   'Luis Fernando',   'Mamani Vargas',  1, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', FALSE, CURRENT_TIMESTAMP, 1)
ON CONFLICT (usuario_id) DO NOTHING;

-- 5. TABLA: SESIONES
INSERT INTO sesiones (sesion_id, usuario_id, hora_sesion_abierta, hora_sesion_cerrada, duracion_sesion, marca_baja) VALUES
(1, 3, CURRENT_TIMESTAMP - INTERVAL '9 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour', 480, FALSE),
(2, 2, CURRENT_TIMESTAMP - INTERVAL '5 hours', CURRENT_TIMESTAMP - INTERVAL '30 minutes', 270, FALSE)
ON CONFLICT (sesion_id) DO NOTHING;

-- 6. TABLA: CLIENTES
INSERT INTO clientes (cliente_id, nit, nombres, apellidos, nombre_razon_social, telefono, marca_baja, fecha_creacion, usuario_creacion) VALUES
(1, '0',          'Sin Nombre', 'Sin Apellido', 'Control Tributario / Sin Nombre', '00000000', FALSE, CURRENT_TIMESTAMP, 1),
(2, '4892103015', 'Juan',       'Perez',        'Perez SRL',                       '70123456', FALSE, CURRENT_TIMESTAMP, 3),
(3, '8374651011', 'Maria',      'Gomez',        'Gomez Consultores',               '71543210', FALSE, CURRENT_TIMESTAMP, 3),
(4, '1020304050', 'Roberto',    'Torrez',       'Constructora Torrez',             '72098765', FALSE, CURRENT_TIMESTAMP, 2)
ON CONFLICT (cliente_id) DO NOTHING;

-- 7. TABLA: PROVEEDORES (numero_contacto max 15 chars, nombre_empresa max 50 chars)
INSERT INTO proveedores (proveedor_id, nombre_empresa, nombre_contacto, numero_contacto, anotaciones, marca_baja, fecha_creacion, usuario_creacion) VALUES
(1, 'PIL Andina S.A.',           'Roberto Gomez',  '70112233', 'Entregas martes y viernes 08:00 AM', FALSE, CURRENT_TIMESTAMP, 1),
(2, 'Industrias Venado S.A.',    'Lucia Vargas',   '71556677', 'Distribuidor mayorista abarrotes y salsas', FALSE, CURRENT_TIMESTAMP, 1),
(3, 'EMBOL S.A. (Coca-Cola)',    'Jorge Camacho',  '72001122', 'Preventa los lunes, entrega miercoles', FALSE, CURRENT_TIMESTAMP, 1),
(4, 'Monopol Ltda.',             'Fernando Rojas', '73009988', 'Linea pinturas, latex y anticorrosivos', FALSE, CURRENT_TIMESTAMP, 1),
(5, 'Unilever Andina Bolivia',   'Carla Medina',   '76543210', 'Detergentes OMO, jabones y limpieza', FALSE, CURRENT_TIMESTAMP, 1)
ON CONFLICT (proveedor_id) DO NOTHING;

-- 8. TABLA: PRODUCTOS (codigo_interno max 10 chars)
-- Estados: 1=Activo, 2=Descontinuado, 3=Suspendido
INSERT INTO productos (producto_id, categoria_id, codigo_barras, nombre_producto, codigo_interno, precio_compra, precio_venta, estado, marca_baja, fecha_creacion, usuario_creacion) VALUES
(1, 1, '777123456001', 'Leche Entera PIL Bolsa 946ml',        'LEC001', 5.20,   6.00,   1, FALSE, CURRENT_TIMESTAMP, 1),
(2, 1, '777123456002', 'Leche Deslactosada PIL 946ml',        'LEC002', 5.80,   7.00,   1, FALSE, CURRENT_TIMESTAMP, 1),
(3, 1, '777123456003', 'Yogurt Bebible Frutilla PIL 1L',      'YOG001', 11.00,  14.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(4, 1, '777123456004', 'Mantequilla PIL con Sal 200g',        'MAN001', 12.50,  16.00,  3, FALSE, CURRENT_TIMESTAMP, 1), -- Suspendido
(5, 2, '777222333001', 'Gaseosa Coca-Cola Retornable 2L',     'GAS001', 7.50,   10.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(6, 2, '777222333002', 'Agua Vital Sin Gas 2L',               'AGU001', 4.50,   7.00,   1, FALSE, CURRENT_TIMESTAMP, 1),
(7, 2, '777222333003', 'Jugo Del Valle Durazno 1L',           'JUG001', 8.00,   11.00,  2, FALSE, CURRENT_TIMESTAMP, 1), -- Descontinuado
(8, 3, '777333444001', 'Cafe Instantaneo Venado Lata 200g',   'CAF001', 22.00,  28.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(9, 3, '777333444002', 'Aceite Fino Girasol 1L',              'ACE001', 11.50,  14.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(10, 4, '777444555001', 'Pintura Latex Monopol Blanca 3.5L',  'PIN001', 85.00,  110.00, 1, FALSE, CURRENT_TIMESTAMP, 1),
(11, 5, '777555666001', 'Detergente OMO Multiaccion 2kg',     'DET001', 29.00,  36.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(12, 5, '777555666002', 'Lavandina Sapolio Clasica 1L',       'LAV001', 6.00,   8.50,   1, FALSE, CURRENT_TIMESTAMP, 1)
ON CONFLICT (producto_id) DO NOTHING;

-- 9. TABLA: LOTES (Incluye lotes por vencer, frescos, con bonificacion y sin vencimiento)
INSERT INTO lotes (lote_id, proveedor_id, descripcion, cantidad, precio_lote, costo_unitario, fecha_vencimiento, marca_baja, fecha_creacion, usuario_creacion) VALUES
(1,  1,    'Lote Leche PIL (Alerta vence en 2 dias)',     20, 104.00, 5.20,  CURRENT_DATE + INTERVAL '2 days',   FALSE, CURRENT_TIMESTAMP, 2),
(2,  1,    'Lote Leche PIL Fresco Quincena',              40, 208.00, 5.20,  CURRENT_DATE + INTERVAL '35 days',  FALSE, CURRENT_TIMESTAMP, 2),
(3,  1,    'Lote Deslactosada c/Regalia (10+1)',          11, 58.00,  5.80,  CURRENT_DATE + INTERVAL '5 days',   FALSE, CURRENT_TIMESTAMP, 2), -- Alerta vence en 5 dias
(4,  1,    'Lote Yogurt Frutilla PIL',                    15, 165.00, 11.00, CURRENT_DATE + INTERVAL '4 days',   FALSE, CURRENT_TIMESTAMP, 2), -- Alerta vence en 4 dias
(5,  1,    'Lote Mantequilla Observada',                  8,  100.00, 12.50, CURRENT_DATE + INTERVAL '25 days',  FALSE, CURRENT_TIMESTAMP, 2),
(6,  3,    'Lote Coca-Cola 2L Cajas',                     36, 270.00, 7.50,  CURRENT_DATE + INTERVAL '120 days', FALSE, CURRENT_TIMESTAMP, 2),
(7,  3,    'Lote Agua Vital Paquetes',                    24, 108.00, 4.50,  CURRENT_DATE + INTERVAL '180 days', FALSE, CURRENT_TIMESTAMP, 2),
(8,  2,    'Lote Cafe Venado Lata',                       12, 264.00, 22.00, CURRENT_DATE + INTERVAL '365 days', FALSE, CURRENT_TIMESTAMP, 2),
(9,  2,    'Lote Aceite Fino Cajas',                      30, 345.00, 11.50, CURRENT_DATE + INTERVAL '200 days', FALSE, CURRENT_TIMESTAMP, 2),
(10, NULL, 'Compra Directa Feria 16 de Julio (Pintura)',  6,  510.00, 85.00, NULL,                               FALSE, CURRENT_TIMESTAMP, 1), -- Sin proveedor y sin fecha vencimiento
(11, 5,    'Lote Detergente OMO Bolsas 2kg',              15, 435.00, 29.00, NULL,                               FALSE, CURRENT_TIMESTAMP, 2),
(12, 5,    'Lote Lavandina Sapolio 1L',                   20, 120.00, 6.00,  CURRENT_DATE + INTERVAL '90 days',  FALSE, CURRENT_TIMESTAMP, 2)
ON CONFLICT (lote_id) DO NOTHING;

-- 10. TABLA: INVENTARIO (Stock activo en sucursal_id = 1)
INSERT INTO inventario (inventario_id, sucursal_id, lote_id, producto_id, stock, en_descuento, precio_descuento, marca_baja, fecha_creacion, usuario_creacion) VALUES
(1,  1, 1,  1,  8,  TRUE,  5.00, FALSE, CURRENT_TIMESTAMP, 2), -- Leche por vencer en liquidacion
(2,  1, 2,  1,  40, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 2), -- Leche fresca precio normal
(3,  1, 3,  2,  11, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 2), -- Deslactosada por vencer (5 dias)
(4,  1, 4,  3,  9,  TRUE,  11.50,FALSE, CURRENT_TIMESTAMP, 2), -- Yogurt por vencer (4 dias) en oferta
(5,  1, 5,  4,  8,  FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 2), -- Mantequilla (Producto suspendido)
(6,  1, 6,  5,  32, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 2), -- Coca-Cola
(7,  1, 7,  6,  20, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 2), -- Agua Vital
(8,  1, 8,  8,  10, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 2), -- Cafe Venado
(9,  1, 9,  9,  25, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 2), -- Aceite Fino
(10, 1, 10, 10, 5,  FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 1), -- Pintura Latex
(11, 1, 11, 11, 14, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 2), -- Detergente OMO
(12, 1, 12, 12, 18, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 2)  -- Lavandina
ON CONFLICT (inventario_id) DO NOTHING;

-- 11. TABLA: INVENTARIO_PENDIENTES (Sala de Espera para probar aprobacion/edicion del Supervisor)
-- Estados: 1=Pendiente, 2=Aprobado, 3=Rechazado
INSERT INTO inventario_pendientes (
    inventario_pendiente_id, sucursal_id, producto_id, nombre_producto, codigo_barras, codigo_interno, 
    categoria_id, precio_compra, precio_venta, stock_ingresado, estado, 
    marca_baja, fecha_creacion, usuario_creacion, fecha_revision, usuario_revision
) VALUES
-- Caso 1: Producto existente (Leche PIL) ingresado por Cajera Ana esperando aprobacion
(1, 1, 1, 'Leche Entera PIL Bolsa 946ml', '777123456001', 'LEC001', 1, 5.20, 6.00, 30, 1, 
 FALSE, CURRENT_TIMESTAMP - INTERVAL '2 hours', 3, NULL, NULL),

-- Caso 2: Producto NUEVO con error tipografico del Cajero ("Gelatina Venado Frambueza") para probar edicion WAR-18 antes de aprobar
(2, 1, NULL, 'Gelatina Venado Frambueza 150g', '777333444099', 'GEL001', 3, 3.50, 5.00, 24, 1, 
 FALSE, CURRENT_TIMESTAMP - INTERVAL '1 hour', 3, NULL, NULL),

-- Caso 3: Producto NUEVO de Ferreteria con codigo de barras valido
(3, 1, NULL, 'Brocha Cerda Natural 2 Pulgadas', '777444555099', 'BRO001', 4, 8.00, 12.00, 12, 1, 
 FALSE, CURRENT_TIMESTAMP - INTERVAL '30 minutes', 4, NULL, NULL),

-- Caso 4: Historico ya Aprobado (Estado = 2)
(4, 1, 5, 'Gaseosa Coca-Cola Retornable 2L', '777222333001', 'GAS001', 2, 7.50, 10.00, 36, 2, 
 FALSE, CURRENT_TIMESTAMP - INTERVAL '1 day', 3, CURRENT_TIMESTAMP - INTERVAL '20 hours', 2),

-- Caso 5: Historico Rechazado por estar duplicado (Estado = 3)
(5, 1, 6, 'Agua Vital Sin Gas 2L', '777222333002', 'AGU001', 2, 4.50, 7.00, 999, 3, 
 FALSE, CURRENT_TIMESTAMP - INTERVAL '2 days', 4, CURRENT_TIMESTAMP - INTERVAL '1 day', 2)
ON CONFLICT (inventario_pendiente_id) DO NOTHING;

-- 12. TABLA: VENTAS y DETALLES_VENTAS (Para poblar modulo de ventas y analitica)
INSERT INTO ventas (
    venta_id, sucursal_id, estado, precio_total, metodo_venta, 
    cobro_qr, cobro_efectivo, cobro_tarjeta, monto_recibido, cambio, 
    cliente_id, anulado_por, marca_baja, fecha_creacion, usuario_creacion
) VALUES
(1, 1, 2, 32.00, 1, 0.00,  32.00, 0.00, 50.00, 18.00, 2, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '4 hours', 3),
(2, 1, 2, 56.00, 2, 56.00, 0.00,  0.00, 56.00, 0.00,  3, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '3 hours', 3),
(3, 1, 2, 146.00,4, 100.00,46.00, 0.00, 50.00, 4.00,  4, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '1 hour',  3)
ON CONFLICT (venta_id) DO NOTHING;

INSERT INTO detalles_ventas (detalle_venta_id, producto_id, venta_id, inventario_id, precio_unitario, cantidad, marca_baja) VALUES
(1, 1,  1, 2,  6.00,   2.00, FALSE), -- 2 Leches PIL = 12.00
(2, 5,  1, 6,  10.00,  2.00, FALSE), -- 2 Coca-Colas = 20.00 (Total Venta 1 = 32.00)
(3, 8,  2, 8,  28.00,  1.00, FALSE), -- 1 Cafe Venado = 28.00
(4, 9,  2, 9,  14.00,  2.00, FALSE), -- 2 Aceites Fino = 28.00 (Total Venta 2 = 56.00)
(5, 10, 3, 10, 110.00, 1.00, FALSE), -- 1 Pintura Latex = 110.00
(6, 11, 3, 11, 36.00,  1.00, FALSE)  -- 1 Detergente OMO = 36.00 (Total Venta 3 = 146.00)
ON CONFLICT (detalle_venta_id) DO NOTHING;

-- 13. TABLAS FINANCIERAS: REPORTES, ENTRADAS, SALIDAS Y MOVIMIENTOS (descripcion/detalle max 40 chars)
INSERT INTO reportes (
    reporte_id, sucursal_id, periodo, fecha_inicio, fecha_fin, 
    monto_base, monto_entradas, monto_salidas, utilidad, datos_snapshot, 
    marca_baja, fecha_creacion, usuario_creacion
) VALUES
(1, 1, 'OCTUBRE-2026', CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE, 
 500.00, 234.00, 85.00, 149.00, '{"ventas_totales": 3, "ticket_promedio": 78.00, "alertas_stock": 3}'::jsonb, 
 FALSE, CURRENT_TIMESTAMP, 1)
ON CONFLICT (reporte_id) DO NOTHING;

INSERT INTO entradas (entrada_id, reporte_id, tipo_entrada, monto, descripcion, marca_baja) VALUES
(1, 1, 1, 234.00, 'Ingresos por ventas del dia', FALSE)
ON CONFLICT (entrada_id) DO NOTHING;

INSERT INTO salidas (salida_id, reporte_id, tipo_salida, monto, descripcion, marca_baja) VALUES
(1, 1, 2, 35.00, 'Compra bolsas plasticas y rollos', FALSE),
(2, 1, 2, 50.00, 'Pago transporte flete mercaderia', FALSE)
ON CONFLICT (salida_id) DO NOTHING;

INSERT INTO movimientos (movimiento_id, reporte_id, monto, tipo_movimiento, metodo_movimiento, detalle, marca_baja, fecha_creacion, usuario_creacion) VALUES
(1, 1, 32.00,  1, 1, 'Cobro Venta #1 Efectivo', FALSE, CURRENT_TIMESTAMP - INTERVAL '4 hours', 3),
(2, 1, 56.00,  1, 2, 'Cobro Venta #2 QR Simple', FALSE, CURRENT_TIMESTAMP - INTERVAL '3 hours', 3),
(3, 1, 146.00, 1, 4, 'Cobro Venta #3 Mixto', FALSE, CURRENT_TIMESTAMP - INTERVAL '1 hour', 3),
(4, 1, 85.00,  2, 1, 'Salidas operativas caja chica', FALSE, CURRENT_TIMESTAMP - INTERVAL '30 minutes', 2)
ON CONFLICT (movimiento_id) DO NOTHING;

-- 14. SINCRONIZAR TODAS LAS SECUENCIAS AL MAXIMO ID INSERTADO
-- Esto evita el error "duplicate key value violates unique constraint" cuando crees nuevos registros desde Spring Boot
CREATE SEQUENCE IF NOT EXISTS conceptos_concepto_id_seq;
ALTER TABLE conceptos ALTER COLUMN concepto_id SET DEFAULT nextval('conceptos_concepto_id_seq');
SELECT setval('conceptos_concepto_id_seq', COALESCE((SELECT MAX(concepto_id) FROM conceptos), 1));

CREATE SEQUENCE IF NOT EXISTS sucursales_sucursal_id_seq;
ALTER TABLE sucursales ALTER COLUMN sucursal_id SET DEFAULT nextval('sucursales_sucursal_id_seq');
SELECT setval('sucursales_sucursal_id_seq', COALESCE((SELECT MAX(sucursal_id) FROM sucursales), 1));

CREATE SEQUENCE IF NOT EXISTS usuarios_usuario_id_seq;
ALTER TABLE usuarios ALTER COLUMN usuario_id SET DEFAULT nextval('usuarios_usuario_id_seq');
SELECT setval('usuarios_usuario_id_seq', COALESCE((SELECT MAX(usuario_id) FROM usuarios), 1));

CREATE SEQUENCE IF NOT EXISTS sesiones_sesion_id_seq;
ALTER TABLE sesiones ALTER COLUMN sesion_id SET DEFAULT nextval('sesiones_sesion_id_seq');
SELECT setval('sesiones_sesion_id_seq', COALESCE((SELECT MAX(sesion_id) FROM sesiones), 1));

CREATE SEQUENCE IF NOT EXISTS clientes_cliente_id_seq;
ALTER TABLE clientes ALTER COLUMN cliente_id SET DEFAULT nextval('clientes_cliente_id_seq');
SELECT setval('clientes_cliente_id_seq', COALESCE((SELECT MAX(cliente_id) FROM clientes), 1));

CREATE SEQUENCE IF NOT EXISTS proveedores_proveedor_id_seq;
ALTER TABLE proveedores ALTER COLUMN proveedor_id SET DEFAULT nextval('proveedores_proveedor_id_seq');
SELECT setval('proveedores_proveedor_id_seq', COALESCE((SELECT MAX(proveedor_id) FROM proveedores), 1));

CREATE SEQUENCE IF NOT EXISTS productos_producto_id_seq;
ALTER TABLE productos ALTER COLUMN producto_id SET DEFAULT nextval('productos_producto_id_seq');
SELECT setval('productos_producto_id_seq', COALESCE((SELECT MAX(producto_id) FROM productos), 1));

CREATE SEQUENCE IF NOT EXISTS lotes_lote_id_seq;
ALTER TABLE lotes ALTER COLUMN lote_id SET DEFAULT nextval('lotes_lote_id_seq');
SELECT setval('lotes_lote_id_seq', COALESCE((SELECT MAX(lote_id) FROM lotes), 1));

CREATE SEQUENCE IF NOT EXISTS inventario_inventario_id_seq;
ALTER TABLE inventario ALTER COLUMN inventario_id SET DEFAULT nextval('inventario_inventario_id_seq');
SELECT setval('inventario_inventario_id_seq', COALESCE((SELECT MAX(inventario_id) FROM inventario), 1));

CREATE SEQUENCE IF NOT EXISTS inventario_pendientes_inventario_pendiente_id_seq;
ALTER TABLE inventario_pendientes ALTER COLUMN inventario_pendiente_id SET DEFAULT nextval('inventario_pendientes_inventario_pendiente_id_seq');
SELECT setval('inventario_pendientes_inventario_pendiente_id_seq', COALESCE((SELECT MAX(inventario_pendiente_id) FROM inventario_pendientes), 1));

CREATE SEQUENCE IF NOT EXISTS ventas_venta_id_seq;
ALTER TABLE ventas ALTER COLUMN venta_id SET DEFAULT nextval('ventas_venta_id_seq');
SELECT setval('ventas_venta_id_seq', COALESCE((SELECT MAX(venta_id) FROM ventas), 1));

CREATE SEQUENCE IF NOT EXISTS detalles_ventas_detalle_venta_id_seq;
ALTER TABLE detalles_ventas ALTER COLUMN detalle_venta_id SET DEFAULT nextval('detalles_ventas_detalle_venta_id_seq');
SELECT setval('detalles_ventas_detalle_venta_id_seq', COALESCE((SELECT MAX(detalle_venta_id) FROM detalles_ventas), 1));

CREATE SEQUENCE IF NOT EXISTS reportes_reporte_id_seq;
ALTER TABLE reportes ALTER COLUMN reporte_id SET DEFAULT nextval('reportes_reporte_id_seq');
SELECT setval('reportes_reporte_id_seq', COALESCE((SELECT MAX(reporte_id) FROM reportes), 1));

CREATE SEQUENCE IF NOT EXISTS entradas_entrada_id_seq;
ALTER TABLE entradas ALTER COLUMN entrada_id SET DEFAULT nextval('entradas_entrada_id_seq');
SELECT setval('entradas_entrada_id_seq', COALESCE((SELECT MAX(entrada_id) FROM entradas), 1));

CREATE SEQUENCE IF NOT EXISTS salidas_salida_id_seq;
ALTER TABLE salidas ALTER COLUMN salida_id SET DEFAULT nextval('salidas_salida_id_seq');
SELECT setval('salidas_salida_id_seq', COALESCE((SELECT MAX(salida_id) FROM salidas), 1));

CREATE SEQUENCE IF NOT EXISTS movimientos_movimiento_id_seq;
ALTER TABLE movimientos ALTER COLUMN movimiento_id SET DEFAULT nextval('movimientos_movimiento_id_seq');
SELECT setval('movimientos_movimiento_id_seq', COALESCE((SELECT MAX(movimiento_id) FROM movimientos), 1));