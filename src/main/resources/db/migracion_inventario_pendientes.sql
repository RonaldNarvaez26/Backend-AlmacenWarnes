-- Migración de esquema: Incorporación de columnas de lote, auditoría y subsanación en inventario_pendientes
ALTER TABLE inventario_pendientes
ADD COLUMN IF NOT EXISTS proveedor_id INTEGER NULL REFERENCES proveedores(proveedor_id),
ADD COLUMN IF NOT EXISTS descripcion_lote VARCHAR(150) NULL,
ADD COLUMN IF NOT EXISTS precio_lote DECIMAL(8,2) NULL,
ADD COLUMN IF NOT EXISTS fecha_vencimiento DATE NULL,
ADD COLUMN IF NOT EXISTS observacion_revision VARCHAR(250) NULL,
ADD COLUMN IF NOT EXISTS fecha_subsanacion TIMESTAMP WITHOUT TIME ZONE NULL;

