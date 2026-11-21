-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     BUSCA UN CLIENTE ACTIVO POR SU NIT/CI EXACTO (OPTIMIZADO PARA POS)
-- ============================================================================================
CREATE OR REPLACE FUNCTION sp_clientes_buscar_nit(
    p_nit VARCHAR(20)
)
RETURNS TABLE (
    cliente_id INT,
    nit VARCHAR(20),
    nombres VARCHAR(50),
    apellidos VARCHAR(50),
    nombre_razon_social VARCHAR(50),
    telefono VARCHAR(20),
    marca_baja BOOLEAN,
    fecha_creacion TIMESTAMP
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.cliente_id,
        c.nit,
        c.nombres,
        c.apellidos,
        c.nombre_razon_social,
        c.telefono,
        c.marca_baja,
        c.fecha_creacion
    FROM Clientes c
    WHERE c.nit = TRIM(p_nit)
      AND c.marca_baja = false
    LIMIT 1;
END;
$$;
