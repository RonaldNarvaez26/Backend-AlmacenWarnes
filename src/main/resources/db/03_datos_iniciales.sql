-- ============================================================================
-- SCRIPT CONSOLIDADO DE DATOS INICIALES (SEED DATA) - ALMACEN WARNES
-- Archivo: 03_datos_iniciales.sql
-- Motor: PostgreSQL 14+ / Spring Boot 3+ (Bcrypt JWT)
-- Descripción: Limpieza total de tablas, datos paramétricos completos (Conceptos 1 al 8),
--              Sucursales, 6 Usuarios de prueba con password '123456', Proveedores abundantes,
--              Catálogo de 15 productos, 20 Lotes, 20 Existencias en Inventario,
--              Sala de Espera (4 pendientes) y sincronización de secuencias.
-- ============================================================================

-- ============================================================================
-- PARCHE PREVIO: Asegurar columnas de auditoría y relaciones en tablas clave
-- ============================================================================
ALTER TABLE sucursales ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP WITHOUT TIME ZONE NULL;
ALTER TABLE sucursales ADD COLUMN IF NOT EXISTS usuario_modificacion INTEGER NULL;

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS sucursal_id INTEGER NULL REFERENCES sucursales(sucursal_id);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS estado INTEGER NOT NULL DEFAULT 1;

ALTER TABLE proveedores ADD COLUMN IF NOT EXISTS sucursal_id INTEGER NULL REFERENCES sucursales(sucursal_id);

ALTER TABLE inventario_pendientes
ADD COLUMN IF NOT EXISTS proveedor_id INTEGER NULL REFERENCES proveedores(proveedor_id),
ADD COLUMN IF NOT EXISTS descripcion_lote VARCHAR(150) NULL,
ADD COLUMN IF NOT EXISTS fecha_vencimiento DATE NULL,
ADD COLUMN IF NOT EXISTS observacion_revision VARCHAR(250) NULL,
ADD COLUMN IF NOT EXISTS fecha_subsanacion TIMESTAMP WITHOUT TIME ZONE NULL;

-- ============================================================================
-- 1. LIMPIEZA TOTAL DE TABLAS (WIPE OUT CON TRUNCATE CASCADE)
-- ============================================================================
TRUNCATE TABLE 
    detalles_ventas,
    ventas,
    entradas,
    salidas,
    movimientos,
    reportes,
    sesiones,
    inventario_pendientes,
    inventario,
    lotes,
    productos,
    proveedores,
    clientes,
    usuarios,
    sucursales,
    conceptos
RESTART IDENTITY CASCADE;

-- ============================================================================
-- 2. DATOS PARAMETRICOS COMPLETOS (TABLA: CONCEPTOS)
-- Grupos:
--   1 = Roles de Usuario (Cajero, Supervisor, Propietario)
--   2 = Categorías de Productos
--   3 = Estados de Producto (Activo, Descontinuado, Suspendido)
--   4 = Métodos de Pago (Efectivo, QR, Tarjeta, Mixto)
--   5 = Estados de Venta (Pendiente, Finalizada, Anulada)
--   6 = Tipos de Movimiento (Entrada, Salida)
--   7 = Estados Inventario Pendiente / Sala de Espera (Pendiente, Aprobado, Rechazado, Observado)
--   8 = Estados de Cuenta de Usuario (Activo, Solicita Recuperación, Bloqueado)
-- ============================================================================
INSERT INTO conceptos (concepto_id, grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion) VALUES
-- Grupo 1: Roles de Usuario
(1, 1, 0, 'Roles de Usuario', FALSE, CURRENT_TIMESTAMP, 1),
(2, 1, 1, 'Cajero',           FALSE, CURRENT_TIMESTAMP, 1),
(3, 1, 2, 'Supervisor',       FALSE, CURRENT_TIMESTAMP, 1),
(4, 1, 3, 'Propietario',      FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 2: Categorías de Productos
(5, 2, 0, 'Categoría de Productos', FALSE, CURRENT_TIMESTAMP, 1),
(6, 2, 1, 'Lácteos y Derivados',     FALSE, CURRENT_TIMESTAMP, 1),
(7, 2, 2, 'Bebidas y Gaseosas',      FALSE, CURRENT_TIMESTAMP, 1),
(8, 2, 3, 'Abarrotes y Despensa',    FALSE, CURRENT_TIMESTAMP, 1),
(9, 2, 4, 'Snacks y Galletas',       FALSE, CURRENT_TIMESTAMP, 1),
(10, 2, 5, 'Limpieza del Hogar',     FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 3: Estado de los Productos
(11, 3, 0, 'Estado de los Productos', FALSE, CURRENT_TIMESTAMP, 1),
(12, 3, 1, 'Activo',                  FALSE, CURRENT_TIMESTAMP, 1),
(13, 3, 2, 'Descontinuado',           FALSE, CURRENT_TIMESTAMP, 1),
(14, 3, 3, 'Suspendido',              FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 4: Métodos de Pago
(15, 4, 0, 'Métodos de Pago', FALSE, CURRENT_TIMESTAMP, 1),
(16, 4, 1, 'Efectivo',        FALSE, CURRENT_TIMESTAMP, 1),
(17, 4, 2, 'QR',              FALSE, CURRENT_TIMESTAMP, 1),
(18, 4, 3, 'Tarjeta',         FALSE, CURRENT_TIMESTAMP, 1),
(19, 4, 4, 'Mixto',           FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 5: Estado de las Ventas
(20, 5, 0, 'Estado de las Ventas', FALSE, CURRENT_TIMESTAMP, 1),
(21, 5, 1, 'Pendiente',            FALSE, CURRENT_TIMESTAMP, 1),
(22, 5, 2, 'Finalizada',           FALSE, CURRENT_TIMESTAMP, 1),
(23, 5, 3, 'Anulada',              FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 6: Tipos de Movimientos
(24, 6, 0, 'Tipos de Movimientos', FALSE, CURRENT_TIMESTAMP, 1),
(25, 6, 1, 'Entrada',              FALSE, CURRENT_TIMESTAMP, 1),
(26, 6, 2, 'Salida',               FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 7: Estado de Inventario Pendiente (Sala de Espera)
(27, 7, 0, 'Estado Inventario Pendiente', FALSE, CURRENT_TIMESTAMP, 1),
(28, 7, 1, 'Pendiente',                   FALSE, CURRENT_TIMESTAMP, 1),
(29, 7, 2, 'Aprobado',                    FALSE, CURRENT_TIMESTAMP, 1),
(30, 7, 3, 'Rechazado',                   FALSE, CURRENT_TIMESTAMP, 1),
(31, 7, 4, 'Observado',                   FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 8: Estados de Cuenta de Usuario (Gestión de Seguridad)
(32, 8, 0, 'Estado Cuenta Usuario', FALSE, CURRENT_TIMESTAMP, 1),
(33, 8, 1, 'Activo',                 FALSE, CURRENT_TIMESTAMP, 1),
(34, 8, 2, 'Solicita Recuperación',  FALSE, CURRENT_TIMESTAMP, 1),
(35, 8, 3, 'Bloqueado',              FALSE, CURRENT_TIMESTAMP, 1);

-- ============================================================================
-- 3. SUCURSALES (TABLA: SUCURSALES)
-- ============================================================================
INSERT INTO sucursales (sucursal_id, codigo_interno, ciudad, direccion, telefono, marca_baja, fecha_creacion, usuario_creacion) VALUES
(1, 'SUC-001', 'Warnes Central', 'Av. 25 de Mayo esq. Warnes #450, Zona Central', '39223344', FALSE, CURRENT_TIMESTAMP, 1),
(2, 'SUC-002', 'Warnes Norte',   'Av. Radial 26 esq. Calle Los Tajibos #120',      '39225566', FALSE, CURRENT_TIMESTAMP, 1);

-- ============================================================================
-- 4. CLIENTES BASE PARA TERMINAL POS Y FACTURACION (TABLA: CLIENTES)
-- ============================================================================
INSERT INTO clientes (cliente_id, nit, nombres, apellidos, nombre_razon_social, telefono, marca_baja, fecha_creacion, usuario_creacion) VALUES
(1, '0',          'Sin Nombre',   'Sin Apellido', 'Control Tributario / Sin Nombre', '00000000', FALSE, CURRENT_TIMESTAMP, 1),
(2, '4892103015', 'Juan Carlos',  'Perez Quispe', 'Perez Distribuciones S.R.L.',     '70123456', FALSE, CURRENT_TIMESTAMP, 1),
(3, '8374651011', 'Maria Elena',  'Gomez Torrez', 'Comercializadora Gomez',          '71543210', FALSE, CURRENT_TIMESTAMP, 1);

-- ============================================================================
-- 5. USUARIOS DE PRUEBA (TABLA: USUARIOS)
-- Contraseña en texto plano para TODOS los 6 usuarios: '123456'
-- Hash generado y validado con Spring Security BCryptPasswordEncoder:
-- $2a$10$8ZGJ5gZH2Nbjhk3tEbs56OYzWwjFrYYgvAukd3NgBnxUNCmZYsIIW
-- ============================================================================
INSERT INTO usuarios (
    usuario_id, sucursal_id, usuario, nombres, apellidos, rol_id, estado, 
    password, marca_baja, fecha_creacion, usuario_creacion
) VALUES
-- 2 Propietarios (rol_id = 3, sucursal_id = NULL para visión global)
(1, 0, 'kenneth.admin',     'Kenneth Gabriel', 'Lopez Castillo',  3, 1, '$2a$10$8ZGJ5gZH2Nbjhk3tEbs56OYzWwjFrYYgvAukd3NgBnxUNCmZYsIIW', FALSE, CURRENT_TIMESTAMP, 1),
(2, 0, 'ronald.admin',      'Ronald',          'Narvaez Alvarez', 3, 1, '$2a$10$8ZGJ5gZH2Nbjhk3tEbs56OYzWwjFrYYgvAukd3NgBnxUNCmZYsIIW', FALSE, CURRENT_TIMESTAMP, 1),

-- 2 Supervisores (rol_id = 2, aislados por sucursal)
(3, 1, 'carlos.supervisor', 'Carlos Eduardo',  'Mendoza Quispe',  2, 1, '$2a$10$8ZGJ5gZH2Nbjhk3tEbs56OYzWwjFrYYgvAukd3NgBnxUNCmZYsIIW', FALSE, CURRENT_TIMESTAMP, 1),
(4, 2, 'patricia.supervisor','Patricia Laura', 'Benitez Rojas',   2, 1, '$2a$10$8ZGJ5gZH2Nbjhk3tEbs56OYzWwjFrYYgvAukd3NgBnxUNCmZYsIIW', FALSE, CURRENT_TIMESTAMP, 1),

-- 2 Cajeros (rol_id = 1, aislados por sucursal)
(5, 1, 'ana.cajera',        'Ana Maria',       'Flores Condori',  1, 1, '$2a$10$8ZGJ5gZH2Nbjhk3tEbs56OYzWwjFrYYgvAukd3NgBnxUNCmZYsIIW', FALSE, CURRENT_TIMESTAMP, 1),
(6, 2, 'luis.cajero',       'Luis Fernando',   'Mamani Vargas',   1, 1, '$2a$10$8ZGJ5gZH2Nbjhk3tEbs56OYzWwjFrYYgvAukd3NgBnxUNCmZYsIIW', FALSE, CURRENT_TIMESTAMP, 1);

-- ============================================================================
-- 6. PROVEEDORES POR SUCURSAL (TABLA: PROVEEDORES)
-- Aislamiento estricto por sucursal_id. Cada sucursal maneja sus propios proveedores
-- 12 Proveedores (5 en Sucursal 1 y 7 en Sucursal 2). 
-- Incluye proveedores dados de baja (#10 y #12) para verificar filtro y restaurar.
-- ============================================================================
INSERT INTO proveedores (
    proveedor_id, sucursal_id, nombre_empresa, nombre_contacto, numero_contacto, 
    anotaciones, marca_baja, fecha_creacion, usuario_creacion
) VALUES
-- SUCURSAL 1 (SUC-001 - Warnes Central)
(1,  1, 'PIL Andina S.A. (Agencia Central)',               'Roberto Gomez',    '70112233', 'Contrato CONTR-PIL-2026: Entregas martes y viernes 08:00 AM. Credito quincenal.', FALSE, CURRENT_TIMESTAMP, 1),
(2,  1, 'Embol S.A. Coca-Cola (Agencia Central)',          'Jorge Camacho',    '72001122', 'Contrato EMB-CB-2026: Preventa lunes, entrega miercoles. Canje botellas retornables.', FALSE, CURRENT_TIMESTAMP, 1),
(3,  1, 'Cerveceria Boliviana Nacional (Agencia Central)', 'Marcelo Quiroga',  '73004455', 'Distribuidor oficial Huari, Pacena y Maltin Warnes Central. Descuento 5% volumen.', FALSE, CURRENT_TIMESTAMP, 1),
(9,  1, 'Molinera & Granos del Oriente (Warnes Central)',   'Gonzalo Rios',     '71020304', 'Distribuidor oficial de cereales, arroz primera y azucar por quintales.', FALSE, CURRENT_TIMESTAMP, 1),
(10, 1, 'Frigorifico Santa Fe Central S.R.L.',             'Esteban Paz',      '74010203', 'Embutidos y fiambres al vacio. Temporalmente suspendido por adecuacion sanitaria.', TRUE, CURRENT_TIMESTAMP, 1), -- Baja logica para probar recuperacion en Sucursal 1

-- SUCURSAL 2 (SUC-002 - Warnes Norte)
(4,  2, 'Industrias Venado S.A. (Agencia Norte)',          'Lucia Vargas',     '71556677', 'Contrato DIST-VEN-2026: Distribuidor abarrotes y salsas Kris. Pedido minimo 20 cajas.', FALSE, CURRENT_TIMESTAMP, 1),
(5,  2, 'Fagal S.R.L. Mabel''s (Agencia Norte)',           'Andrea Salazar',   '78009911', 'Linea de galletas Wafer, Cremositas y Morochas. Visita quincenal Warnes Norte.', FALSE, CURRENT_TIMESTAMP, 1),
(6,  2, 'Unilever Andina Bolivia (Distribuidora Norte)',    'Carla Medina',     '76543210', 'Contrato UNI-BOL-2026: Detergentes OMO, Skip, Rexona y Sedal. Facturacion centralizada.', FALSE, CURRENT_TIMESTAMP, 1),
(7,  2, 'Avicola Sofia Ltda. (Agencia Norte)',             'Javier Gutierrez', '77012399', 'Cadena de frio: embutidos, pollo, quesos y congelados. Entregas diarias 07:00 AM.', FALSE, CURRENT_TIMESTAMP, 1),
(8,  2, 'Nestle Bolivia S.A. (Agencia Norte)',             'Daniel Miranda',   '79901188', 'Linea Nescafe, Milo, Leche Nido y Chocolates Sublime. Pago credito a 30 dias.', FALSE, CURRENT_TIMESTAMP, 1),
(11, 2, 'Cerveceria Boliviana Nacional (Agencia Norte)',   'Rodrigo Morales',  '75098765', 'Distribuidora CBN exclusiva para Warnes Norte. Pedidos semanales jueves.', FALSE, CURRENT_TIMESTAMP, 1),
(12, 2, 'Distribuidora Quimicos Warnes Norte Ltda.',       'Valeria Mendez',   '76081234', 'Articulos de desinfeccion y limpieza industrial. Dado de baja por cambio de razon social.', TRUE, CURRENT_TIMESTAMP, 1); -- Baja logica para probar recuperacion en Sucursal 2

-- ============================================================================
-- 7. CATALOGO MAESTRO DE PRODUCTOS (TABLA: PRODUCTOS)
-- 15 Productos distribuidos en las 5 categorias.
-- Productos #14 y #15 configurados con marca_baja = TRUE para probar recuperacion.
-- ============================================================================
INSERT INTO productos (
    producto_id, categoria_id, codigo_barras, nombre_producto, codigo_interno, 
    precio_compra, precio_venta, estado, marca_baja, fecha_creacion, usuario_creacion
) VALUES
-- Categoria 1: Lácteos y Derivados
(1,  1, '7771234560011', 'Leche Entera PIL Bolsa 946ml',           'LEC001', 5.20,   6.50,  1, FALSE, CURRENT_TIMESTAMP, 1),
(2,  1, '7771234560028', 'Leche Deslactosada PIL 946ml',           'LEC002', 5.80,   7.50,  1, FALSE, CURRENT_TIMESTAMP, 1),
(3,  1, '7771234560035', 'Yogurt Bebible Frutilla PIL 1L',         'YOG001', 11.00, 14.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(4,  1, '7771234560042', 'Mantequilla PIL con Sal 200g',           'MAN001', 12.50, 16.00,  1, FALSE, CURRENT_TIMESTAMP, 1),

-- Categoria 2: Bebidas y Gaseosas
(5,  2, '7772223330018', 'Coca-Cola Sabor Original 2L',            'GAS001', 8.50,  11.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(6,  2, '7772223330025', 'Sprite Lima Limon 2L',                   'GAS002', 8.50,  11.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(7,  2, '7772223330032', 'Agua Vital Sin Gas 2L',                  'AGU001', 4.50,   7.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(8,  2, '7773331110015', 'Malta Maltin Clasica 330ml',             'MAL001', 4.00,   6.00,  1, FALSE, CURRENT_TIMESTAMP, 1),

-- Categoria 3: Abarrotes y Despensa
(9,  3, '7774441110012', 'Cafe Instantaneo Nescafe Tradicion 200g','CAF001', 24.00, 30.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(10, 3, '7774442220019', 'Mayonesa Kris Doypack 500g',             'MAY001', 9.50,  13.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(11, 3, '7774442220026', 'Ketchup Kris Doypack 500g',              'KET001', 8.00,  11.50,  1, FALSE, CURRENT_TIMESTAMP, 1),

-- Categoria 4: Snacks y Galletas
(12, 4, '7775551110016', 'Galletas Wafer Mabel''s Vainilla 120g',  'GAL001', 3.20,   5.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(13, 4, '7775551110023', 'Galletas Cremositas Mabel''s 100g',      'GAL002', 2.80,   4.50,  1, FALSE, CURRENT_TIMESTAMP, 1),
(14, 4, '7775552220013', 'Chocolate Sublime Barra 30g',            'CHO001', 3.50,   5.50,  2, TRUE,  CURRENT_TIMESTAMP, 1), -- Producto dado de baja logica

-- Categoria 5: Limpieza del Hogar
(15, 5, '7776661110010', 'Detergente OMO Multiaccion 2kg',         'DET001', 28.00, 35.00,  1, FALSE, CURRENT_TIMESTAMP, 1),
(16, 5, '7776661110027', 'Lavandina Clasica 1L',                   'LAV001', 5.50,   8.00,  3, TRUE,  CURRENT_TIMESTAMP, 1); -- Producto dado de baja logica

-- ============================================================================
-- 8. LOTES CON TRAZABILIDAD Y FECHAS DE VENCIMIENTO (TABLA: LOTES)
-- 20 Lotes con fechas escalonadas (críticos, próximos a vencer, vigentes, no perecederos)
-- Lotes #16 y #18 configurados con marca_baja = TRUE para probar recuperación.
-- ============================================================================
INSERT INTO lotes (
    lote_id, proveedor_id, descripcion, cantidad, precio_lote, costo_unitario, 
    fecha_vencimiento, marca_baja, fecha_creacion, usuario_creacion
) VALUES
-- Producto 1: Leche Entera PIL (2 lotes: uno por vencer y otro fresco)
(1,  1, 'Lote Leche PIL #1 (Alerta vence en 3 dias)',     30, 150.00, 5.00, CURRENT_DATE + INTERVAL '3 days',   FALSE, CURRENT_TIMESTAMP, 3),
(2,  1, 'Lote Leche PIL #2 Fresco Quincena',              60, 312.00, 5.20, CURRENT_DATE + INTERVAL '35 days',  FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 2: Leche Deslactosada
(3,  1, 'Lote Deslactosada PIL Quincenal',                25, 145.00, 5.80, CURRENT_DATE + INTERVAL '28 days',  FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 3: Yogurt Frutilla (2 lotes: uno en alerta 5 días y otro fresco)
(4,  1, 'Lote Yogurt PIL Frutilla Vence en 5 dias',       20, 220.00, 11.00,CURRENT_DATE + INTERVAL '5 days',   FALSE, CURRENT_TIMESTAMP, 3),
(5,  1, 'Lote Yogurt PIL Frutilla Stock Fresco',          40, 440.00, 11.00,CURRENT_DATE + INTERVAL '45 days',  FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 4: Mantequilla PIL
(6,  1, 'Lote Mantequilla PIL con Sal',                   15, 187.50, 12.50,CURRENT_DATE + INTERVAL '60 days',  FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 5: Coca-Cola 2L (2 lotes: normal y bonificacion)
(7,  2, 'Lote Coca-Cola 2L Cajas Preventa',               50, 425.00, 8.50, CURRENT_DATE + INTERVAL '120 days', FALSE, CURRENT_TIMESTAMP, 3),
(8,  2, 'Lote Coca-Cola 2L Bonificacion Extra',           15, 120.00, 8.00, CURRENT_DATE + INTERVAL '95 days',  FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 6: Sprite 2L
(9,  2, 'Lote Sprite 2L Cajas Preventa',                  30, 255.00, 8.50, CURRENT_DATE + INTERVAL '120 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 7: Agua Vital 2L (2 lotes)
(10, 2, 'Lote Agua Vital 2L Paquetes',                    40, 180.00, 4.50, CURRENT_DATE + INTERVAL '180 days', FALSE, CURRENT_TIMESTAMP, 3),
(11, 2, 'Lote Agua Vital 2L Emergencia Reserva',          20, 90.00,  4.50, CURRENT_DATE + INTERVAL '210 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 8: Malta Maltin
(12, 3, 'Lote Maltin 330ml Sixpacks',                     48, 192.00, 4.00, CURRENT_DATE + INTERVAL '90 days',  FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 9: Cafe Nescafe
(13, 8, 'Lote Nescafe Tradicion Lata 200g',               15, 360.00, 24.00,CURRENT_DATE + INTERVAL '300 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 10: Mayonesa Kris
(14, 4, 'Lote Mayonesa Kris Doypack Cajas',               35, 332.50, 9.50, CURRENT_DATE + INTERVAL '150 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 11: Ketchup Kris
(15, 4, 'Lote Ketchup Kris Doypack Cajas',                30, 240.00, 8.00, CURRENT_DATE + INTERVAL '140 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 12 y 13: Galletas Mabel's
(16, 5, 'Lote Galletas Wafer Mabel''s',                   60, 192.00, 3.20, CURRENT_DATE + INTERVAL '110 days', FALSE, CURRENT_TIMESTAMP, 3),
(17, 5, 'Lote Galletas Cremositas Mabel''s',               50, 140.00, 2.80, CURRENT_DATE + INTERVAL '100 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 14: Chocolate Sublime (Dado de baja logica para restauracion)
(18, 8, 'Lote Chocolate Sublime Descontinuado',           20, 70.00,  3.50, CURRENT_DATE + INTERVAL '15 days',  TRUE,  CURRENT_TIMESTAMP, 3),

-- Producto 15: Detergente OMO (No perecedero)
(19, 6, 'Lote Detergente OMO Bolsas 2kg',                 25, 700.00, 28.00,NULL,                               FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 16: Lavandina (Dado de baja logica por rotura)
(20, 6, 'Lote Lavandina 1L Merma de Transporte',          10, 55.00,  5.50, CURRENT_DATE + INTERVAL '60 days',  TRUE,  CURRENT_TIMESTAMP, 3);

-- ============================================================================
-- 9. INVENTARIO FISICO COMERCIAL (TABLA: INVENTARIO)
-- 20 Registros distribuidos en las 2 sucursales:
--   - Sucursal 1 (Warnes Central, SUC-001): Lotes 1 al 12 (Productos 1 al 8)
--   - Sucursal 2 (Warnes Norte, SUC-002):   Lotes 13 al 20 (Productos 9 al 13, 15)
-- Incluye ítems en descuento por proximidad a vencer e ítems dados de baja lógica.
-- ============================================================================
INSERT INTO inventario (
    inventario_id, sucursal_id, lote_id, producto_id, stock, 
    en_descuento, precio_descuento, marca_baja, fecha_creacion, usuario_creacion
) VALUES
-- SUCURSAL 1 (Warnes Central - SUC-001)
-- Prod 1: Leche PIL
(1,  1, 1,  1,  18, TRUE,  5.00, FALSE, CURRENT_TIMESTAMP, 3), -- En liquidacion por vencer en 3 dias (precio normal 6.50 -> 5.00)
(2,  1, 2,  1,  60, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 3),

-- Prod 2: Leche Deslactosada
(3,  1, 3,  2,  25, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 3),

-- Prod 3: Yogurt Frutilla PIL
(4,  1, 4,  3,  12, TRUE,  12.00,FALSE, CURRENT_TIMESTAMP, 3), -- En oferta por vencer en 5 dias (precio normal 14.00 -> 12.00)
(5,  1, 5,  3,  40, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 3),

-- Prod 4: Mantequilla PIL
(6,  1, 6,  4,  15, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 3),

-- Prod 5: Coca-Cola 2L
(7,  1, 7,  5,  45, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 3),
(8,  1, 8,  5,  15, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 3),

-- Prod 6: Sprite 2L
(9,  1, 9,  6,  28, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 3),

-- Prod 7: Agua Vital 2L
(10, 1, 10, 7,  38, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 3),
(11, 1, 11, 7,  20, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 3),

-- Prod 8: Malta Maltin
(12, 1, 12, 8,  42, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 3),

-- SUCURSAL 2 (Warnes Norte - SUC-002)
-- Prod 9: Cafe Nescafe
(13, 2, 13, 9,  14, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 4),

-- Prod 10: Mayonesa Kris
(14, 2, 14, 10, 32, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 4),

-- Prod 11: Ketchup Kris
(15, 2, 15, 11, 26, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 4),

-- Prod 12 y 13: Galletas Mabel's
(16, 2, 16, 12, 55, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 4),
(17, 2, 17, 13, 45, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 4),

-- Prod 14: Chocolate Sublime (Dado de baja para probar reactivación)
(18, 2, 18, 14, 20, FALSE, NULL, TRUE,  CURRENT_TIMESTAMP, 4),

-- Prod 15: Detergente OMO
(19, 2, 19, 15, 22, FALSE, NULL, FALSE, CURRENT_TIMESTAMP, 4),

-- Prod 16: Lavandina (Dado de baja para probar reactivación)
(20, 2, 20, 16, 10, FALSE, NULL, TRUE,  CURRENT_TIMESTAMP, 4);

-- ============================================================================
-- 10. SALA DE ESPERA (TABLA: INVENTARIO_PENDIENTES)
-- 12 Registros completos para pruebas en vivo de auditoría y aprobación:
--   - SUC-001 (Creados por Cajera Ana Maria #5):
--       IP-1: Reabastecimiento Leche Entera PIL (Prod #1)
--       IP-2: Reabastecimiento Coca-Cola 2L (Prod #5)
--       IP-3: NUEVO Producto: Yogurt Griego PIL 500g (prod_id NULL)
--       IP-4: NUEVO Producto: Jugo del Valle Naranja 1L (prod_id NULL)
--       IP-5: NUEVO Producto: Arroz Grano de Oro 5kg (prod_id NULL)
--       IP-6: OBSERVADO: Malta Maltin 330ml (Revisado por Supervisor Carlos #3)
--   - SUC-002 (Creados por Cajero Luis Mamani #6):
--       IP-7: Reabastecimiento Galletas Wafer Mabel's (Prod #12)
--       IP-8: NUEVO Producto: Queso Menonita San Javier 1kg (prod_id NULL)
--       IP-9: NUEVO Producto: Cerveza Huari Tradicional 620ml (prod_id NULL)
--       IP-10: NUEVO Producto: Lavavajillas Ola Limon 750ml (prod_id NULL)
--       IP-11: OBSERVADO: Mayonesa Kris Doypack 500g (Revisado por Supervisora Patricia #4)
--   - Control de Calidad:
--       IP-12: RECHAZADO: Bebida Energizante Volt 300ml (Rechazado por Supervisor Carlos #3)
-- ============================================================================
INSERT INTO inventario_pendientes (
    inventario_pendiente_id, sucursal_id, producto_id, nombre_producto, codigo_barras, 
    codigo_interno, categoria_id, precio_compra, precio_venta, stock_ingresado, 
    estado, marca_baja, fecha_creacion, usuario_creacion, fecha_revision, usuario_revision, 
    proveedor_id, descripcion_lote, fecha_vencimiento, observacion_revision, fecha_subsanacion
) VALUES
-- SUC-001: Warnes Central (Cajera Ana Maria, ID=5)
(1, 1, 1, 'Leche Entera PIL Bolsa 946ml', '7771234560011', 'LEC001', 1, 5.20, 6.50, 40, 
 1, FALSE, CURRENT_TIMESTAMP - INTERVAL '3 hours', 5, NULL, NULL, 
 1, 'Reabastecimiento PIL Quincena Central', CURRENT_DATE + INTERVAL '30 days', NULL, NULL),

(2, 1, 5, 'Coca-Cola Sabor Original 2L', '7772223330018', 'GAS001', 2, 8.50, 11.00, 30, 
 1, FALSE, CURRENT_TIMESTAMP - INTERVAL '2 hours', 5, NULL, NULL, 
 2, 'Lote Coca-Cola 2L Reposicion Central', CURRENT_DATE + INTERVAL '120 days', NULL, NULL),

(3, 1, NULL, 'Yogurt Griego PIL Natural 500g', '7771234560099', 'YOG002', 1, 12.00, 16.00, 25, 
 1, FALSE, CURRENT_TIMESTAMP - INTERVAL '1 hour', 5, NULL, NULL, 
 1, 'Lote Inicial Yogurt Griego PIL', CURRENT_DATE + INTERVAL '40 days', NULL, NULL),

(4, 1, NULL, 'Jugo del Valle Naranja 1L', '7772223330099', 'BEB001', 2, 7.00, 10.00, 36, 
 1, FALSE, CURRENT_TIMESTAMP - INTERVAL '45 minutes', 5, NULL, NULL, 
 2, 'Lote Del Valle Naranja Tetrabrik', CURRENT_DATE + INTERVAL '180 days', NULL, NULL),

(5, 1, NULL, 'Arroz Grano de Oro Superior 5kg', '7774442220099', 'ABA001', 3, 28.00, 35.00, 20, 
 1, FALSE, CURRENT_TIMESTAMP - INTERVAL '30 minutes', 5, NULL, NULL, 
 9, 'Lote Arroz Grano de Oro Primera', CURRENT_DATE + INTERVAL '365 days', NULL, NULL),

(6, 1, 8, 'Malta Maltin Clasica 330ml', '7773331110015', 'MAL001', 2, 4.80, 6.00, 48, 
 4, FALSE, CURRENT_TIMESTAMP - INTERVAL '4 hours', 5, CURRENT_TIMESTAMP - INTERVAL '2 hours', 3, 
 3, 'Lote Maltin 330ml Observado por Costo', CURRENT_DATE + INTERVAL '90 days', 
 'Costo unitario declarado (Bs 4.80) no coincide con el acuerdo oficial de distribucion (Bs 4.00). Favor subsanar o rectificar el precio.', NULL),

-- SUC-002: Warnes Norte (Cajero Luis Mamani, ID=6)
(7, 2, 12, 'Galletas Wafer Mabel''s Vainilla 120g', '7775551110016', 'GAL001', 4, 3.20, 5.00, 50, 
 1, FALSE, CURRENT_TIMESTAMP - INTERVAL '3 hours', 6, NULL, NULL, 
 5, 'Lote Wafer Reposicion Norte', CURRENT_DATE + INTERVAL '110 days', NULL, NULL),

(8, 2, NULL, 'Queso Menonita San Javier 1kg', '7771239990012', 'LAC001', 1, 32.00, 42.00, 15, 
 1, FALSE, CURRENT_TIMESTAMP - INTERVAL '2 hours', 6, NULL, NULL, 
 7, 'Lote Queso Menonita Tradicional', CURRENT_DATE + INTERVAL '45 days', NULL, NULL),

(9, 2, NULL, 'Cerveza Huari Tradicional 620ml', '7773339990014', 'CER001', 2, 9.50, 13.00, 48, 
 1, FALSE, CURRENT_TIMESTAMP - INTERVAL '1 hour', 6, NULL, NULL, 
 11, 'Lote Huari Tradicional Cajas', CURRENT_DATE + INTERVAL '180 days', NULL, NULL),

(10, 2, NULL, 'Lavavajillas Ola Limon 750ml', '7776669990017', 'LIM001', 5, 8.50, 12.00, 30, 
 1, FALSE, CURRENT_TIMESTAMP - INTERVAL '30 minutes', 6, NULL, NULL, 
 6, 'Lote Lavavajillas Ola Botellas', CURRENT_DATE + INTERVAL '500 days', NULL, NULL),

(11, 2, 10, 'Mayonesa Kris Doypack 500g', '7774442220019', 'MAY001', 3, 10.50, 13.00, 35, 
 4, FALSE, CURRENT_TIMESTAMP - INTERVAL '5 hours', 6, CURRENT_TIMESTAMP - INTERVAL '2 hours', 4, 
 4, 'Lote Mayonesa Kris Observado por Vencimiento', CURRENT_DATE + INTERVAL '150 days', 
 'Fecha de vencimiento reportada presenta borrones en la caja fisica. Verificar con el transportista y actualizar fecha antes de recepcionar.', NULL),

-- Caso Rechazado: Control de calidad en Sucursal 1 (Cajera Ana #5, Supervisor Carlos #3)
(12, 1, NULL, 'Bebida Energizante Volt 300ml', '7778887770019', 'VOL001', 2, 4.00, 6.50, 100, 
 3, FALSE, CURRENT_TIMESTAMP - INTERVAL '1 day', 5, CURRENT_TIMESTAMP - INTERVAL '18 hours', 3, 
 2, 'Lote Volt 300ml Rechazado', CURRENT_DATE + INTERVAL '60 days', 
 'Rechazado en recepcion fisica: Empaque secundario con botellas abolladas y sin registro sanitario Senasag visible.', NULL);

-- ============================================================================
-- 11. VENTAS Y DETALLES DE VENTAS BASE (TABLAS: VENTAS y DETALLES_VENTAS)
-- Permite que el terminal POS, Arqueo de Caja e Historial de Ventas no queden vacíos
-- ============================================================================
INSERT INTO ventas (
    venta_id, sucursal_id, estado, precio_total, metodo_venta, 
    cobro_qr, cobro_efectivo, cobro_tarjeta, monto_recibido, cambio, 
    cliente_id, anulado_por, marca_baja, fecha_creacion, usuario_creacion
) VALUES
(1, 1, 2, 24.00, 1, 0.00, 24.00, 0.00, 30.00, 6.00, 2, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '4 hours', 5),
(2, 1, 2, 30.00, 2, 30.00, 0.00, 0.00, 30.00, 0.00, 3, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '2 hours', 5);

INSERT INTO detalles_ventas (detalle_venta_id, producto_id, venta_id, inventario_id, precio_unitario, cantidad, marca_baja) VALUES
(1, 1, 1, 2, 6.50, 2.00, FALSE), -- 2 Leches PIL = 13.00
(2, 5, 1, 7, 11.00, 1.00, FALSE),-- 1 Coca-Cola = 11.00 (Total Venta 1 = 24.00)
(3, 9, 2, 13, 30.00, 1.00, FALSE);-- 1 Cafe Nescafe = 30.00 (Total Venta 2 = 30.00)

-- ============================================================================
-- 12. SINCRONIZACION AUTOMATICA DE TODAS LAS SECUENCIAS
-- Sincroniza cada secuencia serial al valor maximo actual usando setval()
-- Si la tabla tiene registros: setval(seq, MAX(id), true) -> el siguiente nextval da MAX(id) + 1
-- Si la tabla esta vacia:     setval(seq, 1, false)       -> el siguiente nextval da 1
-- ============================================================================
DO $$
DECLARE
    t RECORD;
    v_max BIGINT;
    v_seq TEXT;
BEGIN
    FOR t IN
        SELECT * FROM (VALUES
            ('conceptos',             'concepto_id'),
            ('sucursales',            'sucursal_id'),
            ('usuarios',              'usuario_id'),
            ('proveedores',           'proveedor_id'),
            ('productos',             'producto_id'),
            ('lotes',                 'lote_id'),
            ('inventario',            'inventario_id'),
            ('inventario_pendientes', 'inventario_pendiente_id'),
            ('clientes',              'cliente_id'),
            ('ventas',                'venta_id'),
            ('detalles_ventas',       'detalle_venta_id'),
            ('reportes',              'reporte_id'),
            ('movimientos',           'movimiento_id'),
            ('entradas',              'entrada_id'),
            ('salidas',               'salida_id'),
            ('sesiones',              'sesion_id')
        ) AS tbl(table_name, column_name)
    LOOP
        v_seq := pg_get_serial_sequence(t.table_name, t.column_name);
        IF v_seq IS NOT NULL THEN
            EXECUTE format('SELECT MAX(%I) FROM %I', t.column_name, t.table_name) INTO v_max;
            IF v_max IS NOT NULL THEN
                PERFORM setval(v_seq, v_max, true);
                RAISE NOTICE 'Secuencia % sincronizada en % (is_called = true)', v_seq, v_max;
            ELSE
                PERFORM setval(v_seq, 1, false);
                RAISE NOTICE 'Secuencia % sincronizada en 1 (is_called = false)', v_seq;
            END IF;
        ELSE
            v_seq := t.table_name || '_' || t.column_name || '_seq';
            IF EXISTS (SELECT 1 FROM pg_class WHERE relkind = 'S' AND relname = v_seq) THEN
                EXECUTE format('SELECT MAX(%I) FROM %I', t.column_name, t.table_name) INTO v_max;
                IF v_max IS NOT NULL THEN
                    PERFORM setval(v_seq, v_max, true);
                    RAISE NOTICE 'Secuencia (fallback) % sincronizada en % (is_called = true)', v_seq, v_max;
                ELSE
                    PERFORM setval(v_seq, 1, false);
                    RAISE NOTICE 'Secuencia (fallback) % sincronizada en 1 (is_called = false)', v_seq;
                END IF;
            ELSE
                RAISE NOTICE 'No se encontro secuencia para %.%', t.table_name, t.column_name;
            END IF;
        END IF;
    END LOOP;
END $$;

-- ============================================================================
-- 13. FUNCIONES Y PROCEDIMIENTOS DEL MODULO DE USUARIOS Y SEGURIDAD
-- Asegura que fn_usuarios_listar, sp_usuarios_insertar, etc. existan y funcionen
-- ============================================================================
CREATE OR REPLACE FUNCTION fn_usuarios_listar(
    p_mostrar_bajas BOOLEAN DEFAULT FALSE,
    p_rol_id INTEGER DEFAULT NULL,
    p_sucursal_id INTEGER DEFAULT NULL,
    p_texto VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    usuario_id INT,
    sucursal_id INT,
    usuario VARCHAR,
    nombres VARCHAR,
    apellidos VARCHAR,
    rol_id INT,
    descripcion_rol VARCHAR,
    estado INT,
    fecha_creacion TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        u.usuario_id,
        u.sucursal_id,
        u.usuario,
        u.nombres,
        u.apellidos,
        u.rol_id,
        COALESCE(c.descripcion, 'Sin Rol')::VARCHAR AS descripcion_rol,
        u.estado,
        u.fecha_creacion
    FROM usuarios u
    LEFT JOIN conceptos c ON c.grupo_id = 1 AND c.codigo = u.rol_id AND c.marca_baja = FALSE
    WHERE (p_mostrar_bajas = TRUE OR u.marca_baja = FALSE)
      AND (p_rol_id IS NULL OR u.rol_id = p_rol_id)
      AND (p_sucursal_id IS NULL OR u.sucursal_id = p_sucursal_id)
      AND (p_texto IS NULL OR TRIM(p_texto) = '' OR 
           u.usuario ILIKE '%' || p_texto || '%' OR 
           u.nombres ILIKE '%' || p_texto || '%' OR 
           u.apellidos ILIKE '%' || p_texto || '%')
    ORDER BY u.usuario_id ASC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE sp_usuarios_insertar(
    IN p_usuario VARCHAR,
    IN p_sucursal_id INT,
    IN p_nombres VARCHAR,
    IN p_apellidos VARCHAR,
    IN p_rol_id INT,
    IN p_password VARCHAR,
    IN p_usuario_creacion INT,
    OUT p_usuario_id INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO usuarios (
        usuario, sucursal_id, nombres, apellidos, rol_id, password, 
        estado, marca_baja, fecha_creacion, usuario_creacion
    ) VALUES (
        p_usuario, p_sucursal_id, p_nombres, p_apellidos, p_rol_id, p_password, 
        1, FALSE, CURRENT_TIMESTAMP, p_usuario_creacion
    ) RETURNING usuarios.usuario_id INTO p_usuario_id;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_usuarios_modificar(
    IN p_usuario_id INT,
    IN p_sucursal_id INT,
    IN p_nombres VARCHAR,
    IN p_apellidos VARCHAR,
    IN p_rol_id INT,
    IN p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE usuarios
    SET sucursal_id = p_sucursal_id,
        nombres = p_nombres,
        apellidos = p_apellidos,
        rol_id = p_rol_id,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE usuario_id = p_usuario_id;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_usuarios_eliminar(
    IN p_usuario_id INT,
    IN p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE usuarios
    SET marca_baja = TRUE,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE usuario_id = p_usuario_id;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_usuarios_solicitar_recuperacion(
    IN p_usuario VARCHAR
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE usuarios
    SET estado = 2,
        fecha_modificacion = CURRENT_TIMESTAMP
    WHERE usuario = p_usuario AND marca_baja = FALSE;
END;
$$;
