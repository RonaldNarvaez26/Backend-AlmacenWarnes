-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     LISTAR SUCURSALES CON FILTROS DINAMICOS Y CANTIDAD DE USUARIOS ASIGNADOS
-- ============================================================================================
CREATE OR REPLACE FUNCTION fn_sucursales_listar(
    p_mostrar_bajas BOOLEAN DEFAULT FALSE,
    p_texto_busqueda VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    sucursal_id INT,
    codigo_interno VARCHAR,
    ciudad VARCHAR,
    direccion VARCHAR,
    telefono VARCHAR,
    fecha_creacion TIMESTAMP,
    cantidad_usuarios INT 
) 
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        s.sucursal_id,
        s.codigo_interno,
        s.ciudad,
        s.direccion,
        s.telefono,
        s.fecha_creacion,
        
        -- SUBCONSULTA: Cuenta los usuarios activos vinculados a esta sucursal
        (SELECT COUNT(*)::INT 
         FROM Usuarios u 
         WHERE u.sucursal_id = s.sucursal_id 
           AND u.marca_baja = false) AS cantidad_usuarios

    FROM Sucursales s
    WHERE (p_mostrar_bajas = true OR s.marca_baja = false)
      AND (p_texto_busqueda IS NULL 
           OR s.codigo_interno ILIKE '%' || p_texto_busqueda || '%'
           OR s.ciudad ILIKE '%' || p_texto_busqueda || '%'
           OR s.direccion ILIKE '%' || p_texto_busqueda || '%')
    ORDER BY s.sucursal_id ASC;
END;
$$;