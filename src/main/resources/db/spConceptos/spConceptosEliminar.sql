-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     ELIMINAR LOGICAMENTE UN CONCEPTO
-- SEGURIDAD:       Prohíbe eliminación de conceptos de grupos estructurales del sistema
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_conceptos_eliminar(
    p_concepto_id INT,
    p_usuario_modificacion INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_grupo_id INT;
BEGIN
    -- Obtener el grupo_id del concepto
    SELECT grupo_id INTO v_grupo_id 
    FROM Conceptos 
    WHERE concepto_id = p_concepto_id AND marca_baja = false;

    IF v_grupo_id IS NULL THEN
        RAISE EXCEPTION 'Concepto no encontrado o ya dado de baja (ID: %)', p_concepto_id;
    END IF;

    -- SEGURIDAD: Los conceptos estructurales del sistema jamás deben eliminarse
    IF v_grupo_id IN (1, 3, 5, 6, 7, 8) THEN
        RAISE EXCEPTION 'Acceso denegado: Los conceptos estructurales del sistema (grupo %) no pueden ser eliminados.', v_grupo_id;
    END IF;

    -- Si es de métodos de pago (grupo 4), solo Supervisor o Propietario pueden dar de baja
    IF v_grupo_id = 4 THEN
        IF NOT EXISTS (
            SELECT 1 FROM Usuarios 
            WHERE usuario_id = p_usuario_modificacion AND rol_id IN (2, 3) AND marca_baja = false
        ) THEN
            RAISE EXCEPTION 'Acceso denegado: Solo el Supervisor o Propietario pueden dar de baja métodos de pago.';
        END IF;
    END IF;

    -- Baja lógica
    UPDATE Conceptos
    SET 
        marca_baja = true
    WHERE concepto_id = p_concepto_id
      AND marca_baja = false;
END;
$$;