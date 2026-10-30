-- ============================================================================
-- PARCHES DDL PREVIOS PARA MODULO DE INVENTARIO (AlmacenWarnes)
-- ============================================================================

-- 1. Columnas complementarias en inventario_pendientes para soportar recepcion de lotes
ALTER TABLE inventario_pendientes ADD COLUMN IF NOT EXISTS proveedor_id INT NULL REFERENCES proveedores(proveedor_id);
ALTER TABLE inventario_pendientes ADD COLUMN IF NOT EXISTS descripcion_lote VARCHAR(100) NULL;
ALTER TABLE inventario_pendientes ADD COLUMN IF NOT EXISTS precio_lote DECIMAL(8,2) NULL;
ALTER TABLE inventario_pendientes ADD COLUMN IF NOT EXISTS fecha_vencimiento DATE NULL;

-- 2. Permitir codigo de barras nulo o vacio para productos artesanales o ferreteria sin codigo inicial
ALTER TABLE productos ALTER COLUMN codigo_barras DROP NOT NULL;
