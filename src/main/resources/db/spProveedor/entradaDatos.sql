-- ============================================================================
-- SCRIPT DE DATOS SEMILLA PARA EL MODULO DE PROVEEDORES (AlmacenWarnes)
-- Respeta 100% el esquema fisico original (8 columnas de la tabla proveedores)
-- ============================================================================

INSERT INTO proveedores (
    proveedor_id,
    nombre_empresa,
    nombre_contacto,
    numero_contacto,
    anotaciones,
    marca_baja,
    fecha_creacion,
    usuario_creacion
) VALUES
(1, 'PIL Andina S.A.', 'Roberto Gomez', '70112233', 'Contrato CONTR-PIL-2026: Entregas martes y viernes 08:00 AM. Credito quincenal.', FALSE, CURRENT_TIMESTAMP, 1),
(2, 'Industrias Venado S.A.', 'Lucia Vargas', '71556677', 'Contrato DIST-VEN-2026: Distribuidor mayorista abarrotes y salsas. Pedido min 20 cajas.', FALSE, CURRENT_TIMESTAMP, 1),
(3, 'EMBOL S.A. (Coca-Cola)', 'Jorge Camacho', '72001122', 'Contrato EMB-CB-2026: Preventa los lunes, entrega miercoles. Canje botellas retornables.', FALSE, CURRENT_TIMESTAMP, 1),
(4, 'Monopol Ltda.', 'Fernando Rojas', '73009988', 'Contrato MON-LPZ-2026: Linea pinturas, latex y anticorrosivos. Descuento 8% pronto pago.', FALSE, CURRENT_TIMESTAMP, 1),
(5, 'Unilever Andina Bolivia', 'Carla Medina', '76543210', 'Contrato UNI-BOL-2026: Detergentes OMO, jabones y linea hogar. Facturacion centralizada.', FALSE, CURRENT_TIMESTAMP, 1),
(6, 'Distribuidora Arcor Bolivia', 'Gonzalo Morales', '78912345', 'Contrato ARC-BOL-2026: Golosinas, chocolates y galletas. Bonificacion 12+1 quincenal.', FALSE, CURRENT_TIMESTAMP, 1)
ON CONFLICT (proveedor_id) DO UPDATE
SET nombre_contacto = EXCLUDED.nombre_contacto,
    numero_contacto = EXCLUDED.numero_contacto,
    anotaciones = EXCLUDED.anotaciones;

-- Sincronizar secuencia de proveedores
SELECT setval('proveedores_proveedor_id_seq', COALESCE((SELECT MAX(proveedor_id) FROM proveedores), 1));

