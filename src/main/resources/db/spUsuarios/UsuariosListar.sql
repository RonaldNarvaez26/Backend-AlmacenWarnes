-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     LISTAR USUARIOS CON FILTROS DINAMICOS Y BUSCADOR (TIPO TABLA)
-- ============================================================================================
CREATE OR REPLACE FUNCTION fn_usuarios_listar(
    p_mostrar_bajas BOOLEAN DEFAULT FALSE,
    p_rol_id INT DEFAULT NULL,
    p_sucursal_id INT DEFAULT NULL, -- NUEVO FILTRO DINÁMICO
    p_texto_busqueda VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    sucursal_id INT,
    usuario_id INT,
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
		u.sucursal_id,
        u.usuario_id,
        u.usuario,
        u.nombres,
        u.apellidos,
        u.rol_id,
        c.descripcion::VARCHAR AS descripcion_rol,
        u.estado,
        u.fecha_creacion
    FROM Usuarios u
    LEFT JOIN Conceptos c 
           ON u.rol_id = c.codigo 
          AND c.grupo_id = 1
          AND c.marca_baja = false
    WHERE (p_mostrar_bajas = true OR u.marca_baja = false)
      AND (p_rol_id IS NULL OR u.rol_id = p_rol_id)
      AND (p_sucursal_id IS NULL OR u.sucursal_id = p_sucursal_id)
      AND (p_texto_busqueda IS NULL 
           OR u.usuario ILIKE '%' || p_texto_busqueda || '%'
           OR u.nombres ILIKE '%' || p_texto_busqueda || '%'
           OR u.apellidos ILIKE '%' || p_texto_busqueda || '%')
    ORDER BY u.usuario_id DESC;
END;
$$;