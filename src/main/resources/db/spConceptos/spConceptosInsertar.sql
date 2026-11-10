-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     AGREGAR UN NUEVO CONCEPTO A UN GRUPO ESPECÍFICO (EJ. CATEGORÍAS)
-- SEGURIDAD:       Protege grupos estructurales del sistema (solo Propietario / rol 3)
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_conceptos_insertar(
    p_grupo_id INT,
    p_descripcion VARCHAR,
    p_usuario_creacion INT,
    INOUT p_nuevo_concepto_id INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_siguiente_codigo INT;
BEGIN
    -- Validar obligatoriedad de la descripción
    IF p_descripcion IS NULL OR TRIM(p_descripcion) = '' THEN
        RAISE EXCEPTION 'La descripción del concepto no puede estar vacía.';
    END IF;

    -- SEGURIDAD: Proteger grupos estructurales del sistema (1: Roles, 3: Estado Prod, 5: Estado Venta, 6: Tipos Mov, 7: Estado Inv Pendiente, 8: Estado Usuario)
    IF p_grupo_id IN (1, 3, 5, 6, 7, 8) THEN
        IF NOT EXISTS (
            SELECT 1 FROM Usuarios 
            WHERE usuario_id = p_usuario_creacion AND rol_id = 3 AND marca_baja = false
        ) THEN
            RAISE EXCEPTION 'Acceso denegado: El grupo % es estructural del sistema y solo puede ser gestionado por el Propietario.', p_grupo_id;
        END IF;
    END IF;

    -- Validar que no exista la misma descripción en el mismo grupo (evitar duplicados)
    IF EXISTS (SELECT 1 FROM Conceptos WHERE grupo_id = p_grupo_id AND descripcion ILIKE TRIM(p_descripcion) AND marca_baja = false) THEN
        RAISE EXCEPTION 'El concepto "%" ya existe en este grupo.', TRIM(p_descripcion);
    END IF;

    -- Calcular el siguiente código correlativo para este grupo
    SELECT COALESCE(MAX(codigo), 0) + 1 INTO v_siguiente_codigo
    FROM Conceptos
    WHERE grupo_id = p_grupo_id;

    -- Insertar el nuevo concepto
    INSERT INTO Conceptos (
        grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion
    )
    VALUES (
        p_grupo_id, v_siguiente_codigo, TRIM(p_descripcion), false, CURRENT_TIMESTAMP, p_usuario_creacion
    )
    RETURNING concepto_id INTO p_nuevo_concepto_id;
END;
$$;