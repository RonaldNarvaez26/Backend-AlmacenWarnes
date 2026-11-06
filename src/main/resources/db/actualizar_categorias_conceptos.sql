-- Actualización y registro de categorías en conceptos (Grupo 2)
UPDATE conceptos SET descripcion = 'Abarrotes' WHERE grupo_id = 2 AND codigo = 3;

INSERT INTO conceptos (concepto_id, grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion)
VALUES 
(30, 2, 4, 'Ferretería y Pinturas', false, CURRENT_TIMESTAMP, 1),
(31, 2, 5, 'Limpieza y Cuidado del Hogar', false, CURRENT_TIMESTAMP, 1),
(32, 2, 6, 'Juguetes', false, CURRENT_TIMESTAMP, 1)
ON CONFLICT (concepto_id) DO UPDATE 
SET descripcion = EXCLUDED.descripcion, marca_baja = false;

-- Corregir categoría de Brocha Monopol (id 5) a 4 (Ferretería)
UPDATE productos SET categoria_id = 4 WHERE producto_id = 5;

-- Sincronizar secuencia
SELECT setval('conceptos_concepto_id_seq', (SELECT MAX(concepto_id) FROM conceptos));

