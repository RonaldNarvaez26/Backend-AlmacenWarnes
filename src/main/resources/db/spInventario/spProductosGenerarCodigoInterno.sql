CREATE OR REPLACE FUNCTION spProductosGenerarCodigoInterno(p_prefijo VARCHAR)
RETURNS VARCHAR 
LANGUAGE plpgsql
AS $$
DECLARE
    v_prefijo_limpio VARCHAR(10);
    v_max_correlativo INT := 0;
    v_nuevo_codigo VARCHAR(10);
BEGIN
    v_prefijo_limpio := UPPER(TRIM(COALESCE(p_prefijo, 'PRO')));

    SELECT COALESCE(MAX(
        CAST(SUBSTRING(codigo_interno FROM LENGTH(v_prefijo_limpio) + 1) AS INT)
    ), 0)
    INTO v_max_correlativo
    FROM productos
    WHERE codigo_interno LIKE v_prefijo_limpio || '%'
      AND SUBSTRING(codigo_interno FROM LENGTH(v_prefijo_limpio) + 1) ~ '^[0-9]+$'
      AND marca_baja = FALSE;

    v_nuevo_codigo := v_prefijo_limpio || LPAD(CAST(v_max_correlativo + 1 AS VARCHAR), 3, '0');
    
    RETURN v_nuevo_codigo;
END;
$$;
