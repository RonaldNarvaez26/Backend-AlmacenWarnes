-- ============================================================================
-- SCRIPT CONSOLIDADO DE DATOS INICIALES (SEED DATA) - ALMACEN WARNES
-- Archivo: 03_datos_iniciales.sql
-- Motor: PostgreSQL 14+ / Spring Boot 3+ (Bcrypt JWT)
-- Descripción: Limpieza total de tablas, datos paramétricos completos (Conceptos 1 al 9),
--              Sucursales, Clientes, 6 Usuarios de prueba con password '123456', Proveedores,
--              Catálogo de 16 productos, 20 Lotes, 20 Existencias en Inventario,
--              Sala de Espera (12 pendientes), 2 Ventas y 3 Detalles de Venta,
--              Historial de Inventario (Kardex inicial) y sincronización de secuencias.
-- ============================================================================
-- ============================================================================
-- PARCHE PREVIO: Asegurar columnas de auditoría y relaciones en tablas clave
-- ============================================================================
ALTER TABLE sucursales ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP WITHOUT TIME ZONE NULL;
ALTER TABLE sucursales ADD COLUMN IF NOT EXISTS usuario_modificacion INTEGER NULL;

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS sucursal_id INTEGER NULL REFERENCES sucursales(sucursal_id);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS estado INTEGER NOT NULL DEFAULT 1;

ALTER TABLE proveedores ADD COLUMN IF NOT EXISTS sucursal_id INTEGER NULL REFERENCES sucursales(sucursal_id);

ALTER TABLE movimientos ADD COLUMN IF NOT EXISTS sucursal_id INTEGER NULL REFERENCES sucursales(sucursal_id);

ALTER TABLE sesiones ALTER COLUMN hora_sesion_cerrada DROP NOT NULL;
ALTER TABLE sesiones ALTER COLUMN duracion_sesion DROP NOT NULL;

ALTER TABLE reportes ALTER COLUMN fecha_inicio TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE reportes ALTER COLUMN fecha_fin TYPE TIMESTAMP WITHOUT TIME ZONE;

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
    historial_inventario,
    notificaciones,
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
(57, 4, 5, 'Transferencia Bancaria', FALSE, CURRENT_TIMESTAMP, 1),
(58, 4, 6, 'Otros',                 FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 5: Estado de las Ventas
(20, 5, 0, 'Estado de las Ventas', FALSE, CURRENT_TIMESTAMP, 1),
(21, 5, 1, 'Pendiente',            FALSE, CURRENT_TIMESTAMP, 1),
(22, 5, 2, 'Finalizada',           FALSE, CURRENT_TIMESTAMP, 1),
(23, 5, 3, 'Anulada',              FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 6: Tipos de Movimientos
(24, 6, 0, 'Tipo de Movimientos', FALSE, CURRENT_TIMESTAMP, 1),
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
(35, 8, 3, 'Bloqueado',              FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 9: Tipo Movimiento Inventario (Kardex)
(36, 9, 0, 'Tipo Movimiento Inventario', FALSE, CURRENT_TIMESTAMP, 1),
(37, 9, 1, 'Ingreso',                   FALSE, CURRENT_TIMESTAMP, 1),
(38, 9, 2, 'Egreso',                    FALSE, CURRENT_TIMESTAMP, 1),
(39, 9, 3, 'Ajuste',                    FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 10: Tipos de Notificación y Alertas Internas
(40, 10, 0, 'Tipos de Notificación',                 FALSE, CURRENT_TIMESTAMP, 1),
(41, 10, 1, 'Stock Agotado',                         FALSE, CURRENT_TIMESTAMP, 1),
(42, 10, 2, 'Stock Bajo',                            FALSE, CURRENT_TIMESTAMP, 1),
(43, 10, 3, 'Sala de Espera (Ingreso Pendiente)',    FALSE, CURRENT_TIMESTAMP, 1),
(44, 10, 4, 'Sala de Espera (Mercadería Observada)', FALSE, CURRENT_TIMESTAMP, 1),
(45, 10, 5, 'Solicitud de Acceso a Dashboard',       FALSE, CURRENT_TIMESTAMP, 1),
(46, 10, 6, 'Acceso a Dashboard Autorizado',         FALSE, CURRENT_TIMESTAMP, 1),
(47, 10, 7, 'Recuperación de Contraseña',            FALSE, CURRENT_TIMESTAMP, 1),
(48, 10, 8, 'Usuario Bloqueado',                     FALSE, CURRENT_TIMESTAMP, 1),
(49, 10, 9, 'Lote Próximo a Vencer',                 FALSE, CURRENT_TIMESTAMP, 1),
(50, 10, 10, 'Lote Vencido',                         FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 11: Umbrales de Alerta de Vencimiento por Categoría (en días)
(51, 11, 0, 'Umbral Días Alerta Vencimiento por Categoría', FALSE, CURRENT_TIMESTAMP, 1),
(52, 11, 1, '15', FALSE, CURRENT_TIMESTAMP, 1),
(53, 11, 2, '30', FALSE, CURRENT_TIMESTAMP, 1),
(54, 11, 3, '45', FALSE, CURRENT_TIMESTAMP, 1),
(55, 11, 4, '30', FALSE, CURRENT_TIMESTAMP, 1),
(56, 11, 5, '60', FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 12: Tipos de Entrada
(59, 12, 0, 'Tipos de Entrada',            FALSE, CURRENT_TIMESTAMP, 1),
(60, 12, 1, 'Venta Comercial',             FALSE, CURRENT_TIMESTAMP, 1),
(61, 12, 2, 'Cobro de Deuda / Préstamo',   FALSE, CURRENT_TIMESTAMP, 1),
(62, 12, 3, 'Aporte de Capital / Fondo',   FALSE, CURRENT_TIMESTAMP, 1),
(63, 12, 4, 'Ingreso Extraordinario',      FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 13: Tipos de Salida
(64, 13, 0, 'Tipos de Salida',             FALSE, CURRENT_TIMESTAMP, 1),
(65, 13, 1, 'Adelanto / Entrega a Empleado', FALSE, CURRENT_TIMESTAMP, 1),
(66, 13, 2, 'Pago de Préstamo / Deuda',    FALSE, CURRENT_TIMESTAMP, 1),
(67, 13, 3, 'Gasto Operativo / Servicios', FALSE, CURRENT_TIMESTAMP, 1),
(68, 13, 4, 'Pago a Proveedor',            FALSE, CURRENT_TIMESTAMP, 1),
(69, 13, 5, 'Retiro de Utilidades / Propietario', FALSE, CURRENT_TIMESTAMP, 1),
(70, 13, 6, 'Egreso Extraordinario',       FALSE, CURRENT_TIMESTAMP, 1),

-- Grupo 14: Categorías de Movimiento Financiero
(71, 14, 0, 'Categorías de Movimiento Financiero', FALSE, CURRENT_TIMESTAMP, 1),
(72, 14, 1, 'Venta Comercial',             FALSE, CURRENT_TIMESTAMP, 1),
(73, 14, 2, 'Préstamo / Deuda',            FALSE, CURRENT_TIMESTAMP, 1),
(74, 14, 3, 'Adelanto a Empleado',         FALSE, CURRENT_TIMESTAMP, 1),
(75, 14, 4, 'Gasto Operativo',             FALSE, CURRENT_TIMESTAMP, 1),
(76, 14, 5, 'Pago a Proveedor',            FALSE, CURRENT_TIMESTAMP, 1),
(77, 14, 6, 'Aporte de Capital',           FALSE, CURRENT_TIMESTAMP, 1),
(78, 14, 7, 'Retiro de Propietario',       FALSE, CURRENT_TIMESTAMP, 1),
(79, 14, 8, 'Otro Extra-Venta',            FALSE, CURRENT_TIMESTAMP, 1);

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
(8,  2, 'Lote Coca-Cola 2L Bonificacion Extra',           15, 120.00, 8.00, CURRENT_DATE + INTERVAL '18 days',  FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 6: Sprite 2L
(9,  2, 'Lote Sprite 2L Cajas Preventa',                  30, 255.00, 8.50, CURRENT_DATE + INTERVAL '120 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 7: Agua Vital 2L (2 lotes)
(10, 2, 'Lote Agua Vital 2L Paquetes',                    40, 180.00, 4.50, CURRENT_DATE + INTERVAL '180 days', FALSE, CURRENT_TIMESTAMP, 3),
(11, 2, 'Lote Agua Vital 2L Emergencia Reserva',          20, 90.00,  4.50, CURRENT_DATE + INTERVAL '210 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 8: Malta Maltin
(12, 3, 'Lote Maltin 330ml Sixpacks',                     48, 192.00, 4.00, CURRENT_DATE - INTERVAL '2 days',   FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 9: Cafe Nescafe
(13, 8, 'Lote Nescafe Tradicion Lata 200g',               15, 360.00, 24.00,CURRENT_DATE + INTERVAL '300 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 10: Mayonesa Kris
(14, 4, 'Lote Mayonesa Kris Doypack Cajas',               35, 332.50, 9.50, CURRENT_DATE + INTERVAL '150 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 11: Ketchup Kris
(15, 4, 'Lote Ketchup Kris Doypack Cajas',                30, 240.00, 8.00, CURRENT_DATE + INTERVAL '140 days', FALSE, CURRENT_TIMESTAMP, 3),

-- Producto 12 y 13: Galletas Mabel's
(16, 5, 'Lote Galletas Wafer Mabel''s',                   60, 192.00, 3.20, CURRENT_DATE + INTERVAL '110 days', FALSE, CURRENT_TIMESTAMP, 3),
(17, 5, 'Lote Galletas Cremositas Mabel''s',               50, 140.00, 2.80, CURRENT_DATE + INTERVAL '12 days',  FALSE, CURRENT_TIMESTAMP, 3),

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
-- 11.0. SESIONES DE CAJA (TABLA: SESIONES)
-- Permite registrar turnos laborales de cajeros, auditar horas reales de apertura/cierre,
-- duración en minutos, y mantener un turno activo listo para operar en la interfaz.
-- ============================================================================
INSERT INTO sesiones (
    sesion_id, usuario_id, hora_sesion_abierta, hora_sesion_cerrada, duracion_sesion, marca_baja
) VALUES
-- Sesión 1: Turno anterior cerrado con arqueo de Ana María Flores (ID=5, Warnes Central)
-- Duración real: 4 horas y 30 minutos (270 minutos)
(1, 5, CURRENT_TIMESTAMP - INTERVAL '8 hours', CURRENT_TIMESTAMP - INTERVAL '3 hours 30 minutes', 270, FALSE),

-- Sesión 2: Turno activo/abierto de Ana María Flores (ID=5, Warnes Central)
-- Abierto hace 2 horas, con ventas y movimientos pendientes de arqueo
(2, 5, CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL, NULL, FALSE),

-- Sesión 3: Turno cerrado con arqueo de Luis Fernando Mamani (ID=6, Warnes Norte)
-- Duración real: 4 horas (240 minutos)
(3, 6, CURRENT_TIMESTAMP - INTERVAL '7 hours', CURRENT_TIMESTAMP - INTERVAL '3 hours', 240, FALSE);

-- ============================================================================
-- 11.1. VENTAS Y DETALLES DE VENTAS BASE (TABLAS: VENTAS y DETALLES_VENTAS)
-- Permite que el terminal POS, Arqueo de Caja, Libro Mayor e Historial no queden vacíos
-- ============================================================================
INSERT INTO ventas (
    venta_id, sucursal_id, estado, precio_total, metodo_venta, 
    cobro_qr, cobro_efectivo, cobro_tarjeta, monto_recibido, cambio, 
    cliente_id, anulado_por, marca_baja, fecha_creacion, usuario_creacion
) VALUES
-- SUCURSAL 1: Warnes Central - Turno 1 Cerrado de Ana (hace 8h a 3.5h)
(1, 1, 2, 50.00,  1, 0.00,  50.00,  0.00, 50.00,  0.00, 2, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '7 hours 30 minutes', 5),
(2, 1, 2, 75.00,  2, 75.00,  0.00,  0.00, 75.00,  0.00, 3, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '6 hours 45 minutes', 5),
(3, 1, 2, 100.00, 3, 0.00,   0.00, 100.00, 100.00, 0.00, 2, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '5 hours 30 minutes', 5),
(4, 1, 2, 60.00,  4, 30.00, 30.00,  0.00, 60.00,  0.00, 1, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '4 hours 15 minutes', 5),
(5, 1, 3, 40.00,  1, 0.00,  40.00,  0.00, 40.00,  0.00, 1, 3,    TRUE,  CURRENT_TIMESTAMP - INTERVAL '4 hours', 5),

-- SUCURSAL 2: Warnes Norte - Turno Cerrado de Luis (hace 7h a 3h)
(6, 2, 2, 80.00,  1, 0.00,  80.00,  0.00, 100.00, 20.00, 2, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '6 hours 15 minutes', 6),
(7, 2, 2, 50.00,  2, 50.00,  0.00,  0.00, 50.00,   0.00, 3, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '4 hours 45 minutes', 6),

-- SUCURSAL 1: Warnes Central - Turno 2 Activo de Ana (hace 2h en adelante)
(8, 1, 2, 80.00,  1, 0.00,  80.00,  0.00, 100.00, 20.00, 2, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '1 hour 30 minutes', 5),
(9, 1, 2, 40.00,  2, 40.00,  0.00,  0.00, 40.00,   0.00, 1, NULL, FALSE, CURRENT_TIMESTAMP - INTERVAL '45 minutes', 5);

INSERT INTO detalles_ventas (detalle_venta_id, producto_id, venta_id, inventario_id, precio_unitario, cantidad, marca_baja) VALUES
-- Venta 1 (50.00): 5 Leches Enteras PIL a 10.00 Bs
(1, 1, 1, 2, 10.00, 5.00, FALSE),
-- Venta 2 (75.00): 3 Coca-Cola 2L a 25.00 Bs
(2, 5, 2, 7, 25.00, 3.00, FALSE),
-- Venta 3 (100.00): 2 Nescafe Tradicion 200g a 50.00 Bs
(3, 9, 3, 13, 50.00, 2.00, FALSE),
-- Venta 4 (60.00): 6 Yogurts Frutilla PIL a 10.00 Bs
(4, 3, 4, 4, 10.00, 6.00, FALSE),
-- Venta 5 (40.00, Anulada): 4 Leches Deslactosadas PIL a 10.00 Bs
(5, 2, 5, 3, 10.00, 4.00, TRUE),
-- Venta 6 (80.00): 4 Mayonesas Kris a 20.00 Bs
(6, 10, 6, 14, 20.00, 4.00, FALSE),
-- Venta 7 (50.00): 2 Ketchups Kris a 25.00 Bs
(7, 11, 7, 15, 25.00, 2.00, FALSE),
-- Venta 8 (80.00, Turno Activo): 8 Leches Enteras PIL a 10.00 Bs
(8, 1, 8, 2, 10.00, 8.00, FALSE),
-- Venta 9 (40.00, Turno Activo): 4 Coca-Cola 2L a 10.00 Bs
(9, 5, 9, 7, 10.00, 4.00, FALSE);

-- ============================================================================
-- 11.2. REPORTES BASE, ENTRADAS, SALIDAS Y MOVIMIENTOS (LIBRO MAYOR)
-- ============================================================================
INSERT INTO reportes (
    reporte_id, sucursal_id, periodo, fecha_inicio, fecha_fin, 
    monto_base, monto_entradas, monto_salidas, utilidad, 
    datos_snapshot, marca_baja, fecha_creacion, usuario_creacion
) VALUES
-- Reporte 1: Turno de Ana María Flores (Sesión 1, duración 4h 30m)
-- Total Ventas: 285.00 (Efectivo 80, QR 105, Tarjeta 100) + Movimiento Entrada 50 = Entradas 335.00
-- Total Movimientos Salida: 30 + 25 = Salidas 55.00. Utilidad: 280.00.
(1, 1, 'Turno', 
 (SELECT hora_sesion_abierta FROM sesiones WHERE sesion_id = 1), 
 (SELECT hora_sesion_cerrada FROM sesiones WHERE sesion_id = 1), 
 0.00, 335.00, 55.00, 280.00, 
'{"resumenVentas":{"conteo":4,"efectivo":80.00,"qr":105.00,"tarjeta":100.00,"total":285.00},"resumenMovimientos":{"entradas":50.00,"salidas":55.00,"cantidad":3},"totalesArqueo":{"entradasDeclaradas":285.00,"salidasDeclaradas":0.00,"totalEntradasFinal":335.00,"totalSalidasFinal":55.00,"utilidad":280.00,"diferencia":0.00},"topProductos":[{"producto":"Cafe Instantaneo Nescafe Tradicion 200g","cantidad":2,"subtotal":100.00},{"producto":"Coca-Cola Sabor Original 2L","cantidad":3,"subtotal":75.00},{"producto":"Yogurt Bebible Frutilla PIL 1L","cantidad":6,"subtotal":60.00},{"producto":"Leche Entera PIL Bolsa 946ml","cantidad":5,"subtotal":50.00}]}'::jsonb, 
FALSE, (SELECT hora_sesion_cerrada FROM sesiones WHERE sesion_id = 1), 5),

-- Reporte 2: Turno de Luis Fernando Mamani (Sesión 3, duración 4h 00m)
-- Total Ventas: 130.00 (Efectivo 80, QR 50). Salidas: 0.00. Utilidad: 130.00.
(2, 2, 'Turno', 
 (SELECT hora_sesion_abierta FROM sesiones WHERE sesion_id = 3), 
 (SELECT hora_sesion_cerrada FROM sesiones WHERE sesion_id = 3), 
 0.00, 130.00,  0.00, 130.00, 
'{"resumenVentas":{"conteo":2,"efectivo":80.00,"qr":50.00,"tarjeta":0.00,"total":130.00},"resumenMovimientos":{"entradas":0.00,"salidas":0.00,"cantidad":0},"totalesArqueo":{"entradasDeclaradas":130.00,"salidasDeclaradas":0.00,"totalEntradasFinal":130.00,"totalSalidasFinal":0.00,"utilidad":130.00,"diferencia":0.00},"topProductos":[{"producto":"Mayonesa Kris Doypack 500g","cantidad":4,"subtotal":80.00},{"producto":"Ketchup Kris Doypack 500g","cantidad":2,"subtotal":50.00}]}'::jsonb, 
FALSE, (SELECT hora_sesion_cerrada FROM sesiones WHERE sesion_id = 3), 6);

INSERT INTO entradas (entrada_id, reporte_id, tipo_entrada, monto, descripcion, marca_baja) VALUES
-- Reporte 1 (Warnes Central, Turno Ana): 80 + 105 + 100 + 50 = 335.00 Bs
(1, 1, 1, 80.00,  'Ventas en Efectivo (Sistema)', FALSE),
(2, 1, 2, 105.00, 'Ventas por QR (Digital)', FALSE),
(3, 1, 3, 100.00, 'Ventas con Tarjeta (POS)', FALSE),
(4, 1, 5, 50.00,  'Movimientos de entrada del turno', FALSE),
-- Reporte 2 (Warnes Norte, Turno Luis): 80 + 50 = 130.00 Bs
(5, 2, 1, 80.00,  'Ventas en Efectivo (Sistema)', FALSE),
(6, 2, 2, 50.00,  'Ventas por QR (Digital)', FALSE);

INSERT INTO salidas (salida_id, reporte_id, tipo_salida, monto, descripcion, marca_baja) VALUES
-- Reporte 1 (Warnes Central, Turno Ana): 30 + 25 = 55.00 Bs
(1, 1, 1, 30.00, 'Adelanto de sueldo cajera Ana', FALSE),
(2, 1, 3, 25.00, 'Gasto operativo insumos limpieza', FALSE),
(3, 1, 1, 20.00, 'Adelanto anulado error admin', TRUE);

INSERT INTO movimientos (
    movimiento_id, sucursal_id, reporte_id, monto, tipo_movimiento, 
    metodo_movimiento, detalle, marca_baja, fecha_creacion, usuario_creacion
) VALUES
-- Movimientos sellados en Reporte 1 (Turno cerrado de Ana)
(1, 1, 1, 50.00, 1, 1, 'Cobro servicio fotocopias e impresiones', FALSE, CURRENT_TIMESTAMP - INTERVAL '7 hours', 5),
(2, 1, 1, 30.00, 2, 1, 'Adelanto de sueldo a cajera Ana', FALSE, CURRENT_TIMESTAMP - INTERVAL '6 hours', 5),
(3, 1, 1, 25.00, 2, 1, 'Gasto operativo insumos de limpieza', FALSE, CURRENT_TIMESTAMP - INTERVAL '5 hours', 5),
(4, 1, 1, 20.00, 2, 1, 'Adelanto anulado por error', TRUE, CURRENT_TIMESTAMP - INTERVAL '4 hours 30 minutes', 5),

-- Movimientos pendientes de arqueo en Sesión 2 activa (Turno activo de Ana)
(5, 1, NULL, 25.00, 1, 1, 'Ingreso extraordinario cobro flete', FALSE, CURRENT_TIMESTAMP - INTERVAL '1 hour 15 minutes', 5),
(6, 1, NULL, 15.00, 2, 1, 'Compra cafe y azucar para el personal', FALSE, CURRENT_TIMESTAMP - INTERVAL '30 minutes', 5);

-- ============================================================================
-- 12. HISTORIAL DE INVENTARIO (KARDEX INICIAL)
-- Registro estático del movimiento inicial de apertura para cada existencia física
-- ============================================================================
INSERT INTO Historial_Inventario (
    historial_id, sucursal_id, producto_id, tipo_movimiento,
    stock_anterior, stock_resultante,
    descripcion, marca_baja, fecha_creacion,
    usuario_creacion, usuario_autorizacion
) VALUES
-- SUCURSAL 1 (Warnes Central - SUC-001) - Supervisor Carlos Mendoza (ID=3)
(1,  1, 1,  1, 0,  18, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(2,  1, 1,  1, 18, 78, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(3,  1, 2,  1, 0,  25, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(4,  1, 3,  1, 0,  12, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(5,  1, 3,  1, 12, 52, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(6,  1, 4,  1, 0,  15, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(7,  1, 5,  1, 0,  45, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(8,  1, 5,  1, 45, 60, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(9,  1, 6,  1, 0,  28, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(10, 1, 7,  1, 0,  38, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(11, 1, 7,  1, 38, 58, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),
(12, 1, 8,  1, 0,  42, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 3, 3),

-- SUCURSAL 2 (Warnes Norte - SUC-002) - Supervisora Patricia Benitez (ID=4)
(13, 2, 9,  1, 0, 14, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 4, 4),
(14, 2, 10, 1, 0, 32, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 4, 4),
(15, 2, 11, 1, 0, 26, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 4, 4),
(16, 2, 12, 1, 0, 55, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 4, 4),
(17, 2, 13, 1, 0, 45, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 4, 4),
(18, 2, 15, 1, 0, 22, 'Stock inicial de apertura de almacén', FALSE, CURRENT_TIMESTAMP, 4, 4);

-- ============================================================================
-- 13. NOTIFICACIONES INICIALES (TABLA: NOTIFICACIONES)
-- ============================================================================
INSERT INTO Notificaciones (
    usuario_destino, rol_destino, tipo_notificacion,
    titulo_alerta, mensaje_alerta, direccion,
    leido, fecha_emision, fecha_leido
) VALUES
(5, 1, 4, 'Mercadería Observada para Corrección', 'El lote de Malta Maltin Clasica 330ml en SUC-001 fue observado: Costo unitario declarado no coincide con el acuerdo oficial.', '/inventario?tab=pendientes', FALSE, CURRENT_TIMESTAMP, NULL),
(NULL, 2, 3, 'Ingreso Pendiente en Sala de Espera', 'Existen 5 lotes pendientes de autorización en SUC-001.', '/inventario?tab=pendientes', FALSE, CURRENT_TIMESTAMP, NULL),
(NULL, 3, 3, 'Ingreso Pendiente en Sala de Espera', 'Existen 9 lotes pendientes de autorización a nivel global.', '/inventario?tab=pendientes', FALSE, CURRENT_TIMESTAMP, NULL),
(NULL, 3, 7, 'Solicitud de Restablecimiento de Contraseña', 'El usuario pedro.cajero2 (@pedro.cajero2) ha solicitado el restablecimiento de su contraseña de acceso.', '/admin/usuarios', FALSE, CURRENT_TIMESTAMP, NULL);

-- ============================================================================
-- 14. SINCRONIZACION DE SECUENCIAS
-- ============================================================================
SELECT setval('conceptos_concepto_id_seq', (SELECT MAX(concepto_id) FROM conceptos));
SELECT setval('sucursales_sucursal_id_seq', (SELECT MAX(sucursal_id) FROM sucursales));
SELECT setval('usuarios_usuario_id_seq', (SELECT MAX(usuario_id) FROM usuarios));
SELECT setval('proveedores_proveedor_id_seq', (SELECT MAX(proveedor_id) FROM proveedores));
SELECT setval('productos_producto_id_seq', (SELECT MAX(producto_id) FROM productos));
SELECT setval('lotes_lote_id_seq', (SELECT MAX(lote_id) FROM lotes));
SELECT setval('inventario_inventario_id_seq', (SELECT MAX(inventario_id) FROM inventario));
SELECT setval('inventario_pendientes_inventario_pendiente_id_seq', (SELECT MAX(inventario_pendiente_id) FROM inventario_pendientes));
SELECT setval('clientes_cliente_id_seq', (SELECT MAX(cliente_id) FROM clientes));
SELECT setval('ventas_venta_id_seq', (SELECT MAX(venta_id) FROM ventas));
SELECT setval('detalles_ventas_detalle_venta_id_seq', (SELECT MAX(detalle_venta_id) FROM detalles_ventas));
SELECT setval('historial_inventario_historial_id_seq', (SELECT MAX(historial_id) FROM historial_inventario));
SELECT setval('notificaciones_id_notificacion_seq', COALESCE((SELECT MAX(id_notificacion) FROM notificaciones), 1), (SELECT COUNT(*) > 0 FROM notificaciones));
SELECT setval('reportes_reporte_id_seq', (SELECT MAX(reporte_id) FROM reportes));
SELECT setval('movimientos_movimiento_id_seq', (SELECT MAX(movimiento_id) FROM movimientos));
SELECT setval('entradas_entrada_id_seq', (SELECT MAX(entrada_id) FROM entradas));
SELECT setval('salidas_salida_id_seq', (SELECT MAX(salida_id) FROM salidas));
SELECT setval('sesiones_sesion_id_seq', (SELECT MAX(sesion_id) FROM sesiones));
