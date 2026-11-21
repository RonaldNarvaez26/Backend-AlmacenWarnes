-- ============================================================================================
-- AUTOR:           Ronald Narvaez Estevez
-- DESCRIPCION:     REGISTRA UN NUEVO CLIENTE (O REACTIVA SI YA EXISTÍA CON MARCA_BAJA)
--                  RETORNA EL cliente_id GENERADO O EXISTENTE
-- ============================================================================================
CREATE OR REPLACE FUNCTION sp_clientes_insertar(
    p_nit VARCHAR(20),
    p_nombres VARCHAR(50),
    p_apellidos VARCHAR(50),
    p_nombre_razon_social VARCHAR(50),
    p_telefono VARCHAR(20),
    p_usuario_creacion INT DEFAULT 1
)
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    v_cliente_id INT;
    v_nit_limpio VARCHAR(20);
    v_nombres_limpio VARCHAR(50);
    v_apellidos_limpio VARCHAR(50);
    v_razon_social_limpio VARCHAR(50);
    v_telefono_limpio VARCHAR(20);
BEGIN
    v_nit_limpio := NULLIF(TRIM(p_nit), '');
    v_nombres_limpio := NULLIF(TRIM(p_nombres), '');
    v_apellidos_limpio := NULLIF(TRIM(p_apellidos), '');
    v_razon_social_limpio := NULLIF(TRIM(p_nombre_razon_social), '');
    v_telefono_limpio := NULLIF(TRIM(p_telefono), '');

    -- Validación: Al menos debe proporcionarse un identificador (NIT, Razón Social o Nombre)
    IF v_nit_limpio IS NULL AND v_razon_social_limpio IS NULL AND v_nombres_limpio IS NULL THEN
        RAISE EXCEPTION 'Debe proporcionar al menos el NIT/CI, la Razón Social o el Nombre del cliente.';
    END IF;

    -- Si se proporciona NIT, verificar si ya existe un cliente con ese NIT
    IF v_nit_limpio IS NOT NULL THEN
        SELECT cliente_id INTO v_cliente_id
        FROM Clientes
        WHERE nit = v_nit_limpio
          AND marca_baja = false
        LIMIT 1;

        -- Si ya existe activo con ese NIT, actualizar datos complementarios y retornar su ID
        IF v_cliente_id IS NOT NULL THEN
            UPDATE Clientes
            SET nombres = COALESCE(v_nombres_limpio, nombres),
                apellidos = COALESCE(v_apellidos_limpio, apellidos),
                nombre_razon_social = COALESCE(v_razon_social_limpio, nombre_razon_social),
                telefono = COALESCE(v_telefono_limpio, telefono),
                fecha_modificacion = CURRENT_TIMESTAMP,
                usuario_modificacion = p_usuario_creacion
            WHERE cliente_id = v_cliente_id;

            RETURN v_cliente_id;
        END IF;

        -- Si existe pero estaba dado de baja lógica, reactivarlo con los nuevos datos
        SELECT cliente_id INTO v_cliente_id
        FROM Clientes
        WHERE nit = v_nit_limpio
          AND marca_baja = true
        LIMIT 1;

        IF v_cliente_id IS NOT NULL THEN
            UPDATE Clientes
            SET nombres = v_nombres_limpio,
                apellidos = v_apellidos_limpio,
                nombre_razon_social = v_razon_social_limpio,
                telefono = v_telefono_limpio,
                marca_baja = false,
                fecha_modificacion = CURRENT_TIMESTAMP,
                usuario_modificacion = p_usuario_creacion
            WHERE cliente_id = v_cliente_id;

            RETURN v_cliente_id;
        END IF;
    END IF;

    -- Inserción de un nuevo cliente
    INSERT INTO Clientes (
        nit,
        nombres,
        apellidos,
        nombre_razon_social,
        telefono,
        marca_baja,
        fecha_creacion,
        usuario_creacion,
        fecha_modificacion,
        usuario_modificacion
    ) VALUES (
        v_nit_limpio,
        v_nombres_limpio,
        v_apellidos_limpio,
        v_razon_social_limpio,
        v_telefono_limpio,
        false,
        CURRENT_TIMESTAMP,
        p_usuario_creacion,
        CURRENT_TIMESTAMP,
        p_usuario_creacion
    ) RETURNING cliente_id INTO v_cliente_id;

    RETURN v_cliente_id;
END;
$$;
