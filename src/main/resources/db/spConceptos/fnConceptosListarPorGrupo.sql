-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     LISTAR CONCEPTOS POR GRUPO EXCLUYENDO EL CÓDIGO 0
-- ============================================================================================
CREATE OR REPLACE FUNCTION fn_conceptos_listar_por_grupo(
    p_grupo_id INT
)
RETURNS TABLE (
    concepto_id INT,
    codigo INT,
    descripcion VARCHAR
) 
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.concepto_id,
        c.codigo,
        c.descripcion::VARCHAR
    FROM Conceptos c
    WHERE c.grupo_id = p_grupo_id
      AND c.codigo <> 0
      AND c.marca_baja = false
    ORDER BY c.codigo ASC;
END;
$$;