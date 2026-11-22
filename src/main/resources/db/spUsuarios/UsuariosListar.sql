-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez / Kenneth Lopez
-- DESCRIPCION:     LISTAR USUARIOS CON FILTROS DINAMICOS Y BUSCADOR (TIPO TABLA)
-- ============================================================================================
DROP FUNCTION IF EXISTS fn_usuarios_listar;
DROP FUNCTION IF EXISTS fn_usuarios_listar(BOOLEAN, INT, INT, VARCHAR);

CREATE OR REPLACE FUNCTION fn_usuarios_listar(
    p_mostrar_bajas BOOLEAN DEFAULT FALSE,
    p_rol_id INT DEFAULT NULL,
    p_sucursal_id INT DEFAULT NULL,
    p_texto_busqueda VARCHAR DEFAULT NULL
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
) 
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        u.usuario_id,
        COALESCE(u.sucursal_id, 0) AS sucursal_id,
        u.usuario,
        u.nombres,
        u.apellidos,
        u.rol_id,
        COALESCE(c.descripcion, 'Sin Rol')::VARCHAR AS descripcion_rol,
        u.estado,
        u.fecha_creacion
    FROM usuarios u
    LEFT JOIN conceptos c 
           ON u.rol_id = c.codigo 
          AND c.grupo_id = 1
          AND c.marca_baja = false
    WHERE (p_mostrar_bajas = true OR u.marca_baja = false)
      AND (p_rol_id IS NULL OR u.rol_id = p_rol_id)
      AND (p_sucursal_id IS NULL OR u.sucursal_id = p_sucursal_id OR (p_sucursal_id = 0 AND (u.sucursal_id = 0 OR u.sucursal_id IS NULL)))
      AND (p_texto_busqueda IS NULL OR TRIM(p_texto_busqueda) = ''
           OR u.usuario ILIKE '%' || p_texto_busqueda || '%'
           OR u.nombres ILIKE '%' || p_texto_busqueda || '%'
           OR u.apellidos ILIKE '%' || p_texto_busqueda || '%')
    ORDER BY u.usuario_id ASC;
END;
$$;
