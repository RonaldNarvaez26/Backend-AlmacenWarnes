CREATE OR REPLACE FUNCTION spProductosGenerarCodigoInterno(p_prefijo VARCHAR)
RETURNS VARCHAR 
LANGUAGE plpgsql
AS $$
DECLARE
    v_limpio VARCHAR(100);
    v_prefijo_3 VARCHAR(3);
    v_max_productos INT := 0;
    v_max_pendientes INT := 0;
    v_max_correlativo INT := 0;
    v_nuevo_codigo VARCHAR(10);
BEGIN
    -- 1. Normalizar acentos y filtrar unicamente letras A-Z
    v_limpio := UPPER(COALESCE(p_prefijo, ''));
    v_limpio := TRANSLATE(v_limpio, 'ÁÉÍÓÚÀÈÌÒÙÄËÏÖÜÂÊÎÔÛÑ', 'AEIOUAEIOUAEIOUAEIOUN');
    v_limpio := REGEXP_REPLACE(v_limpio, '[^A-Z]', '', 'g');

    -- 2. Extraer prefijo de exactamente 3 letras (rellenando con 'X' si tiene menos de 3)
    IF LENGTH(v_limpio) = 0 THEN
        v_prefijo_3 := 'PRO';
    ELSIF LENGTH(v_limpio) < 3 THEN
        v_prefijo_3 := RPAD(v_limpio, 3, 'X');
    ELSE
        v_prefijo_3 := SUBSTRING(v_limpio FROM 1 FOR 3);
    END IF;

    -- 3. Buscar el correlativo maximo en la tabla `productos` para ese prefijo de 3 letras
    SELECT COALESCE(MAX(
        CAST(SUBSTRING(codigo_interno FROM 4) AS INT)
    ), 0)
    INTO v_max_productos
    FROM productos
    WHERE codigo_interno LIKE v_prefijo_3 || '%'
      AND LENGTH(codigo_interno) >= 4
      AND SUBSTRING(codigo_interno FROM 4) ~ '^[0-9]+$'
      AND marca_baja = FALSE;

    -- 4. Buscar el correlativo maximo en la tabla `inventario_pendientes` para ese prefijo de 3 letras
    SELECT COALESCE(MAX(
        CAST(SUBSTRING(codigo_interno FROM 4) AS INT)
    ), 0)
    INTO v_max_pendientes
    FROM inventario_pendientes
    WHERE codigo_interno LIKE v_prefijo_3 || '%'
      AND LENGTH(codigo_interno) >= 4
      AND SUBSTRING(codigo_interno FROM 4) ~ '^[0-9]+$'
      AND marca_baja = FALSE;

    -- 5. Tomar el maximo de ambos origenes para evitar colisiones
    v_max_correlativo := GREATEST(v_max_productos, v_max_pendientes);

    -- 6. Formatear el siguiente codigo con ceros a la izquierda (ej. LEC004)
    v_nuevo_codigo := v_prefijo_3 || LPAD(CAST(v_max_correlativo + 1 AS VARCHAR), 3, '0');
    
    RETURN v_nuevo_codigo;
END;
$$;
