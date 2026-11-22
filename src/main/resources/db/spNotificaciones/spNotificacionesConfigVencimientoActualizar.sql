-- ============================================================================================
-- AUTOR:           Kenneth Lopez / Ronald Narvaez
-- MODULO:          Notificaciones y Alertas Internas en Tiempo Real
-- DESCRIPCION:     Procedimientos para configurar los días de anticipación para alertas de
--                  vencimiento por categoría de producto:
--                  1. Modo Global: Actualiza todas las categorías al mismo tiempo.
--                  2. Modo Individual: Actualiza una categoría específica.
-- ============================================================================================

DROP PROCEDURE IF EXISTS sp_notificaciones_config_vencimiento_global(INT, INT, INT, VARCHAR);
DROP PROCEDURE IF EXISTS sp_notificaciones_config_vencimiento_global(INT, INT);
DROP PROCEDURE IF EXISTS sp_notificaciones_config_vencimiento_individual(INT, INT, INT, BOOLEAN, VARCHAR);
DROP PROCEDURE IF EXISTS sp_notificaciones_config_vencimiento_individual(INT, INT, INT);

-- ============================================================================================
-- 1. ACTUALIZACION GLOBAL: Aplica los días de anticipación a TODAS las categorías activas
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_notificaciones_config_vencimiento_global(
    IN p_dias_anticipacion INT,
    IN p_usuario_id INT,
    OUT p_afectados INT,
    OUT p_mensaje VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    r_cat RECORD;
    v_contador INT := 0;
BEGIN
    -- 1. Validación de rango (1 a 365 días)
    IF p_dias_anticipacion IS NULL OR p_dias_anticipacion < 1 OR p_dias_anticipacion > 365 THEN
        p_afectados := 0;
        p_mensaje := 'El tiempo de anticipación debe estar comprendido entre 1 y 365 días.';
        RETURN;
    END IF;

    -- 2. Asegurar existencia de la cabecera del Grupo 11
    IF NOT EXISTS (SELECT 1 FROM conceptos WHERE grupo_id = 11 AND codigo = 0) THEN
        INSERT INTO conceptos (grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion)
        VALUES (11, 0, 'Umbral Días Alerta Vencimiento por Categoría', FALSE, CURRENT_TIMESTAMP, p_usuario_id);
    END IF;

    -- 3. Iterar por cada categoría activa del Grupo 2 y sincronizar Grupo 11
    FOR r_cat IN (
        SELECT codigo 
        FROM conceptos 
        WHERE grupo_id = 2 AND codigo > 0 AND marca_baja = FALSE
        ORDER BY codigo
    ) LOOP
        IF EXISTS (SELECT 1 FROM conceptos WHERE grupo_id = 11 AND codigo = r_cat.codigo) THEN
            UPDATE conceptos
            SET descripcion = p_dias_anticipacion::VARCHAR,
                marca_baja = FALSE
            WHERE grupo_id = 11 AND codigo = r_cat.codigo;
        ELSE
            INSERT INTO conceptos (grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion)
            VALUES (11, r_cat.codigo, p_dias_anticipacion::VARCHAR, FALSE, CURRENT_TIMESTAMP, p_usuario_id);
        END IF;

        v_contador := v_contador + 1;
    END LOOP;

    -- 4. Limpiar alertas no leídas previas de tipo 9 (Lote Próximo a Vencer) para recálculo inmediato
    DELETE FROM notificaciones 
    WHERE tipo_notificacion = 9 AND leido = FALSE;

    p_afectados := v_contador;
    p_mensaje := 'Umbral global de vencimiento configurado exitosamente a ' || p_dias_anticipacion || ' días para ' || v_contador || ' categorías.';
END;
$$;

-- ============================================================================================
-- 2. ACTUALIZACION INDIVIDUAL: Aplica los días de anticipación a una categoría específica
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_notificaciones_config_vencimiento_individual(
    IN p_categoria_id INT,
    IN p_dias_anticipacion INT,
    IN p_usuario_id INT,
    OUT p_resultado BOOLEAN,
    OUT p_mensaje VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nombre_cat VARCHAR;
BEGIN
    -- 1. Validación de rango (1 a 365 días)
    IF p_dias_anticipacion IS NULL OR p_dias_anticipacion < 1 OR p_dias_anticipacion > 365 THEN
        p_resultado := FALSE;
        p_mensaje := 'El tiempo de anticipación debe estar comprendido entre 1 y 365 días.';
        RETURN;
    END IF;

    -- 2. Validar que la categoría exista en Grupo 2 y esté activa
    SELECT descripcion INTO v_nombre_cat
    FROM conceptos
    WHERE grupo_id = 2 AND codigo = p_categoria_id AND marca_baja = FALSE;

    IF v_nombre_cat IS NULL THEN
        p_resultado := FALSE;
        p_mensaje := 'La categoría especificada no existe o se encuentra inactiva.';
        RETURN;
    END IF;

    -- 3. Asegurar existencia de la cabecera del Grupo 11
    IF NOT EXISTS (SELECT 1 FROM conceptos WHERE grupo_id = 11 AND codigo = 0) THEN
        INSERT INTO conceptos (grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion)
        VALUES (11, 0, 'Umbral Días Alerta Vencimiento por Categoría', FALSE, CURRENT_TIMESTAMP, p_usuario_id);
    END IF;

    -- 4. Upsert en Grupo 11 para la categoría
    IF EXISTS (SELECT 1 FROM conceptos WHERE grupo_id = 11 AND codigo = p_categoria_id) THEN
        UPDATE conceptos
        SET descripcion = p_dias_anticipacion::VARCHAR,
            marca_baja = FALSE
        WHERE grupo_id = 11 AND codigo = p_categoria_id;
    ELSE
        INSERT INTO conceptos (grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion)
        VALUES (11, p_categoria_id, p_dias_anticipacion::VARCHAR, FALSE, CURRENT_TIMESTAMP, p_usuario_id);
    END IF;

    -- 5. Limpiar alertas no leídas previas de tipo 9 (Lote Próximo a Vencer) para recálculo inmediato
    DELETE FROM notificaciones 
    WHERE tipo_notificacion = 9 AND leido = FALSE;

    p_resultado := TRUE;
    p_mensaje := 'Umbral de vencimiento para la categoría "' || v_nombre_cat || '" configurado a ' || p_dias_anticipacion || ' días.';
END;
$$;
