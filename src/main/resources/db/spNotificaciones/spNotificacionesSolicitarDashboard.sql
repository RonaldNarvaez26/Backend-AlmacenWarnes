-- ============================================================================================
-- AUTOR:           Kenneth Lopez / Ronald Narvaez
-- MODULO:          Notificaciones y Alertas Internas en Tiempo Real
-- DESCRIPCION:     Permite a un Supervisor solicitar acceso temporal al Dashboard.
--                  Almacena el estado en la tabla Conceptos (grupo_id = 10, codigo = usuario_id)
--                  e inserta un registro formal en la tabla física Notificaciones (tipo 5).
-- ============================================================================================

DROP PROCEDURE IF EXISTS sp_notificaciones_solicitar_dashboard(INT);
DROP PROCEDURE IF EXISTS sp_notificaciones_solicitar_dashboard(INT, VARCHAR, VARCHAR);
DROP PROCEDURE IF EXISTS sp_notificaciones_solicitar_dashboard;

CREATE OR REPLACE PROCEDURE sp_notificaciones_solicitar_dashboard(
    IN p_usuario_id INT,
    OUT p_estado VARCHAR,
    OUT p_mensaje VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_rol_id INT;
    v_activo BOOLEAN;
    v_nombre VARCHAR;
    v_usuario VARCHAR;
    v_concepto_id INT;
    v_descripcion VARCHAR;
    v_marca_baja BOOLEAN;
BEGIN
    -- 1. Validar existencia y estado del usuario solicitante
    SELECT rol_id, (marca_baja = FALSE), (nombres || ' ' || COALESCE(apellidos, '')), usuario
    INTO v_rol_id, v_activo, v_nombre, v_usuario
    FROM usuarios
    WHERE usuario_id = p_usuario_id;

    IF v_rol_id IS NULL THEN
        p_estado := 'ERROR';
        p_mensaje := 'El usuario solicitante no existe en el sistema.';
        RETURN;
    END IF;

    IF NOT v_activo THEN
        p_estado := 'ERROR';
        p_mensaje := 'El usuario se encuentra inactivo o dado de baja.';
        RETURN;
    END IF;

    -- 2. Validar que el rol sea Supervisor (rol_id = 2)
    IF v_rol_id <> 2 THEN
        p_estado := 'ERROR';
        p_mensaje := 'Solo los usuarios con rol Supervisor pueden solicitar acceso temporal al Dashboard.';
        RETURN;
    END IF;

    -- 3. Buscar si ya existe una solicitud registrada en Conceptos Grupo 10
    SELECT concepto_id, descripcion, marca_baja
    INTO v_concepto_id, v_descripcion, v_marca_baja
    FROM conceptos
    WHERE grupo_id = 10 AND codigo = p_usuario_id
    ORDER BY concepto_id DESC
    LIMIT 1;

    -- Evaluar si ya está pendiente o aprobado
    IF v_concepto_id IS NOT NULL AND v_marca_baja = FALSE THEN
        IF UPPER(TRIM(v_descripcion)) = 'PENDIENTE' THEN
            p_estado := 'ADVERTENCIA';
            p_mensaje := 'Ya cuenta con una solicitud de acceso al Dashboard pendiente de revision por el Propietario.';
            RETURN;
        ELSIF UPPER(TRIM(v_descripcion)) = 'APROBADO' THEN
            p_estado := 'INFO';
            p_mensaje := 'El acceso al Dashboard ya se encuentra concedido y vigente.';
            RETURN;
        END IF;
    END IF;

    -- 4. Registrar o actualizar en Conceptos (Grupo 10)
    IF v_concepto_id IS NULL THEN
        INSERT INTO conceptos (
            grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion
        )
        VALUES (
            10, p_usuario_id, 'PENDIENTE', FALSE, CURRENT_TIMESTAMP, p_usuario_id
        );
    ELSE
        UPDATE conceptos
        SET descripcion = 'PENDIENTE',
            marca_baja = FALSE,
            fecha_creacion = CURRENT_TIMESTAMP,
            usuario_creacion = p_usuario_id
        WHERE concepto_id = v_concepto_id;
    END IF;

    -- 5. Insertar notificación formal en la tabla física Notificaciones (tipo_notificacion = 5)
    INSERT INTO notificaciones (
        usuario_destino,
        rol_destino,
        tipo_notificacion,
        titulo_alerta,
        mensaje_alerta,
        direccion,
        leido,
        fecha_emision,
        fecha_leido
    ) VALUES (
        NULL,
        3, -- Rol 3: Propietario
        5, -- Concepto 5: Solicitud de Acceso a Dashboard
        'Solicitud de Acceso a Dashboard',
        ('El Supervisor ' || TRIM(v_nombre) || ' (@' || v_usuario || ') solicita autorización para visualizar el Dashboard.')::VARCHAR(255),
        '/dashboard',
        FALSE,
        CURRENT_TIMESTAMP,
        NULL
    );

    p_estado := 'EXITO';
    p_mensaje := 'Solicitud de acceso al Dashboard enviada exitosamente al Propietario.';

EXCEPTION
    WHEN OTHERS THEN
        p_estado := 'ERROR';
        p_mensaje := 'Error interno al registrar la solicitud: ' || SQLERRM;
END;
$$;
