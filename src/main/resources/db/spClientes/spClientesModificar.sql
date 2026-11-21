-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     ACTUALIZA LOS DATOS DE UN CLIENTE EXISTENTE (VALIDA COLISIÓN DE NIT)
-- ============================================================================================
CREATE OR REPLACE PROCEDURE sp_clientes_modificar(
    p_cliente_id INT,
    p_nit VARCHAR(20),
    p_nombres VARCHAR(50),
    p_apellidos VARCHAR(50),
    p_nombre_razon_social VARCHAR(50),
    p_telefono VARCHAR(20),
    p_usuario_modificacion INT DEFAULT 1
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nit_limpio VARCHAR(20);
    v_nombres_limpio VARCHAR(50);
    v_apellidos_limpio VARCHAR(50);
    v_razon_social_limpio VARCHAR(50);
    v_telefono_limpio VARCHAR(20);
BEGIN
    -- Validar que el cliente exista y esté activo
    IF NOT EXISTS (SELECT 1 FROM Clientes WHERE cliente_id = p_cliente_id AND marca_baja = false) THEN
        RAISE EXCEPTION 'El cliente con ID % no existe o se encuentra inactivo.', p_cliente_id;
    END IF;

    v_nit_limpio := NULLIF(TRIM(p_nit), '');
    v_nombres_limpio := NULLIF(TRIM(p_nombres), '');
    v_apellidos_limpio := NULLIF(TRIM(p_apellidos), '');
    v_razon_social_limpio := NULLIF(TRIM(p_nombre_razon_social), '');
    v_telefono_limpio := NULLIF(TRIM(p_telefono), '');

    -- Si se especifica un NIT, verificar que no pertenezca a otro cliente activo
    IF v_nit_limpio IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM Clientes 
            WHERE nit = v_nit_limpio 
              AND cliente_id <> p_cliente_id 
              AND marca_baja = false
        ) THEN
            RAISE EXCEPTION 'Ya existe otro cliente activo registrado con el NIT/CI: %', v_nit_limpio;
        END IF;
    END IF;

    -- Actualizar datos del cliente
    UPDATE Clientes
    SET nit = v_nit_limpio,
        nombres = v_nombres_limpio,
        apellidos = v_apellidos_limpio,
        nombre_razon_social = v_razon_social_limpio,
        telefono = v_telefono_limpio,
        fecha_modificacion = CURRENT_TIMESTAMP,
        usuario_modificacion = p_usuario_modificacion
    WHERE cliente_id = p_cliente_id;
END;
$$;
