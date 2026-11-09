-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     MODIFICAR LA DESCRIPCION DE UN CONCEPTO
-- SEGURIDAD:       Protege grupos estructurales del sistema (solo Propietario / rol 3)
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_conceptos_modificar(
    p_concepto_id INT,
    p_descripcion VARCHAR,
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_grupo_id INT;
BEGIN
    -- Validar obligatoriedad de la descripción
    IF p_descripcion IS NULL OR TRIM(p_descripcion) = '' THEN
        RAISE EXCEPTION 'La descripción del concepto no puede estar vacía.';
    END IF;

    -- Obtener el grupo_id del concepto a modificar
    SELECT grupo_id INTO v_grupo_id 
    FROM Conceptos 
    WHERE concepto_id = p_concepto_id AND marca_baja = false;

    IF v_grupo_id IS NULL THEN
        RAISE EXCEPTION 'Concepto no encontrado o dado de baja (ID: %)', p_concepto_id;
    END IF;

    -- SEGURIDAD: Proteger grupos estructurales del sistema
    IF v_grupo_id IN (1, 3, 5, 6, 7, 8) THEN
        IF NOT EXISTS (
            SELECT 1 FROM Usuarios 
            WHERE usuario_id = p_usuario_modificacion AND rol_id = 3 AND marca_baja = false
        ) THEN
            RAISE EXCEPTION 'Acceso denegado: El grupo % es estructural del sistema y solo puede ser modificado por el Propietario.', v_grupo_id;
        END IF;
    END IF;

    -- Modificar descripción
    UPDATE Conceptos
    SET 
        descripcion = TRIM(p_descripcion)
    WHERE concepto_id = p_concepto_id
      AND marca_baja = false;
END;
$$;