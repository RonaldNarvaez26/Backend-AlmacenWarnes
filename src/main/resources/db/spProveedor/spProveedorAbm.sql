-- ============================================================================
-- PROCEDIMIENTOS Y FUNCIONES PARA EL ABM DE PROVEEDORES (AlmacenWarnes)
-- ============================================================================

-- 1. REGISTRAR PROVEEDOR / DISTRIBUIDOR
CREATE OR REPLACE FUNCTION spProveedorRegistrar(
    p_nombre_empresa VARCHAR(50),
    p_nombre_contacto VARCHAR(50),
    p_numero_contacto VARCHAR(15),
    p_anotaciones VARCHAR(200),
    p_usuario_creacion INT DEFAULT 1
)
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    v_nuevo_id INT;
    v_nombre_limpio VARCHAR(50);
BEGIN
    v_nombre_limpio := TRIM(p_nombre_empresa);

    -- Validaciones de obligatoriedad
    IF v_nombre_limpio IS NULL OR v_nombre_limpio = '' THEN
        RAISE EXCEPTION 'El nombre o razon social de la empresa es obligatorio.';
    END IF;

    IF p_nombre_contacto IS NULL OR TRIM(p_nombre_contacto) = '' THEN
        RAISE EXCEPTION 'El nombre del contacto logistico es obligatorio.';
    END IF;

    IF p_numero_contacto IS NULL OR TRIM(p_numero_contacto) = '' THEN
        RAISE EXCEPTION 'El numero de contacto o telefono es obligatorio.';
    END IF;

    -- Validar que no exista un proveedor activo con el mismo nombre
    IF EXISTS (
        SELECT 1 FROM proveedores
        WHERE LOWER(TRIM(nombre_empresa)) = LOWER(v_nombre_limpio)
          AND marca_baja = FALSE
    ) THEN
        RAISE EXCEPTION 'Ya existe un distribuidor activo registrado con el nombre: %', v_nombre_limpio;
    END IF;

    INSERT INTO proveedores (
        nombre_empresa,
        nombre_contacto,
        numero_contacto,
        anotaciones,
        marca_baja,
        fecha_creacion,
        usuario_creacion
    ) VALUES (
        v_nombre_limpio,
        TRIM(p_nombre_contacto),
        TRIM(p_numero_contacto),
        COALESCE(TRIM(p_anotaciones), 'Sin anotaciones contractuales'),
        FALSE,
        CURRENT_TIMESTAMP,
        COALESCE(p_usuario_creacion, 1)
    ) RETURNING proveedor_id INTO v_nuevo_id;

    RETURN v_nuevo_id;
END;
$$;

-- 2. ACTUALIZAR PROVEEDOR / DISTRIBUIDOR
CREATE OR REPLACE PROCEDURE spProveedorActualizar(
    p_proveedor_id INT,
    p_nombre_empresa VARCHAR(50),
    p_nombre_contacto VARCHAR(50),
    p_numero_contacto VARCHAR(15),
    p_anotaciones VARCHAR(200),
    p_usuario_modificacion INT DEFAULT 1
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nombre_limpio VARCHAR(50);
BEGIN
    v_nombre_limpio := TRIM(p_nombre_empresa);

    -- Validar existencia
    IF NOT EXISTS (SELECT 1 FROM proveedores WHERE proveedor_id = p_proveedor_id AND marca_baja = FALSE) THEN
        RAISE EXCEPTION 'El proveedor con ID % no existe o esta dado de baja.', p_proveedor_id;
    END IF;

    IF v_nombre_limpio IS NULL OR v_nombre_limpio = '' THEN
        RAISE EXCEPTION 'El nombre o razon social de la empresa no puede quedar vacio.';
    END IF;

    IF p_nombre_contacto IS NULL OR TRIM(p_nombre_contacto) = '' THEN
        RAISE EXCEPTION 'El nombre del contacto logistico no puede quedar vacio.';
    END IF;

    IF p_numero_contacto IS NULL OR TRIM(p_numero_contacto) = '' THEN
        RAISE EXCEPTION 'El numero de contacto no puede quedar vacio.';
    END IF;

    -- Validar colision con otro proveedor
    IF EXISTS (
        SELECT 1 FROM proveedores
        WHERE LOWER(TRIM(nombre_empresa)) = LOWER(v_nombre_limpio)
          AND proveedor_id <> p_proveedor_id
          AND marca_baja = FALSE
    ) THEN
        RAISE EXCEPTION 'El nombre de empresa % ya se encuentra registrado en otro proveedor activo.', v_nombre_limpio;
    END IF;

    UPDATE proveedores
    SET nombre_empresa = v_nombre_limpio,
        nombre_contacto = TRIM(p_nombre_contacto),
        numero_contacto = TRIM(p_numero_contacto),
        anotaciones = COALESCE(TRIM(p_anotaciones), 'Sin anotaciones contractuales')
    WHERE proveedor_id = p_proveedor_id;
END;
$$;

-- 3. DAR DE BAJA LOGICA (ELIMINAR) PROVEEDOR
CREATE OR REPLACE PROCEDURE spProveedorEliminar(
    p_proveedor_id INT,
    p_usuario_modificacion INT DEFAULT 1
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM proveedores WHERE proveedor_id = p_proveedor_id AND marca_baja = FALSE) THEN
        RAISE EXCEPTION 'El proveedor con ID % no existe o ya fue dado de baja.', p_proveedor_id;
    END IF;

    UPDATE proveedores
    SET marca_baja = TRUE
    WHERE proveedor_id = p_proveedor_id;
END;
$$;

