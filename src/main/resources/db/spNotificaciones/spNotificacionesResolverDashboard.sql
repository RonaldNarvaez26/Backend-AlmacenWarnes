-- ============================================================================================
-- AUTOR:           Kenneth Lopez / Ronald Narvaez
-- MODULO:          Notificaciones y Alertas Internas en Tiempo Real
-- DESCRIPCION:     Permite al Propietario aprobar o rechazar la solicitud de acceso al Dashboard
--                  hecha por un Supervisor, actualizando Conceptos Grupo 10 y la tabla Notificaciones.
-- ============================================================================================

DROP PROCEDURE IF EXISTS sp_notificaciones_resolver_dashboard(INT, BOOLEAN, INT);
DROP PROCEDURE IF EXISTS sp_notificaciones_resolver_dashboard(INT, BOOLEAN, INT, VARCHAR, VARCHAR);
DROP PROCEDURE IF EXISTS sp_notificaciones_resolver_dashboard;

CREATE OR REPLACE PROCEDURE sp_notificaciones_resolver_dashboard(
    IN p_supervisor_id INT,
    IN p_autorizar BOOLEAN,
    IN p_propietario_id INT,
    OUT p_estado VARCHAR,
    OUT p_mensaje VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_rol_propietario INT;
    v_rol_supervisor  INT;
    v_activo_prop     BOOLEAN;
    v_activo_sup      BOOLEAN;
    v_concepto_id     INT;
    v_nombre_sup      VARCHAR;
    v_nuevo_estado    VARCHAR;
BEGIN
    -- 1. Validar que quien autoriza sea Propietario activo (rol_id = 3)
    SELECT rol_id, (marca_baja = FALSE)
    INTO v_rol_propietario, v_activo_prop
    FROM usuarios
    WHERE usuario_id = p_propietario_id;

    IF v_rol_propietario IS NULL OR NOT v_activo_prop OR v_rol_propietario <> 3 THEN
        p_estado := 'ERROR';
        p_mensaje := 'Acceso denegado: Solo el Propietario puede autorizar o revocar accesos al Dashboard.';
        RETURN;
    END IF;

    -- 2. Validar que el usuario objetivo sea Supervisor activo (rol_id = 2)
    SELECT rol_id, (marca_baja = FALSE), (nombres || ' ' || COALESCE(apellidos, ''))
    INTO v_rol_supervisor, v_activo_sup, v_nombre_sup
    FROM usuarios
    WHERE usuario_id = p_supervisor_id;

    IF v_rol_supervisor IS NULL OR NOT v_activo_sup OR v_rol_supervisor <> 2 THEN
        p_estado := 'ERROR';
        p_mensaje := 'El usuario especificado no existe o no cuenta con rol de Supervisor.';
        RETURN;
    END IF;

    -- 3. Localizar la solicitud en Conceptos Grupo 10
    SELECT concepto_id
    INTO v_concepto_id
    FROM conceptos
    WHERE grupo_id = 10 AND codigo = p_supervisor_id
    ORDER BY concepto_id DESC
    LIMIT 1;

    IF v_concepto_id IS NULL THEN
        IF p_autorizar THEN
            INSERT INTO conceptos (
                grupo_id, codigo, descripcion, marca_baja, fecha_creacion, usuario_creacion
            )
            VALUES (
                10, p_supervisor_id, 'APROBADO', FALSE, CURRENT_TIMESTAMP, p_propietario_id
            );
        ELSE
            p_estado := 'ERROR';
            p_mensaje := 'No se encontro ninguna solicitud de acceso registrada para este supervisor.';
            RETURN;
        END IF;
    ELSE
        IF p_autorizar THEN
            v_nuevo_estado := 'APROBADO';
        ELSE
            v_nuevo_estado := 'RECHAZADO';
        END IF;

        UPDATE conceptos
        SET descripcion = v_nuevo_estado,
            marca_baja = FALSE,
            fecha_creacion = CURRENT_TIMESTAMP,
            usuario_creacion = p_propietario_id
        WHERE concepto_id = v_concepto_id;
    END IF;

    -- 4. Marcar como leída la solicitud previa en la tabla Notificaciones
    UPDATE notificaciones
    SET leido = TRUE,
        fecha_leido = CURRENT_TIMESTAMP
    WHERE tipo_notificacion = 5
      AND rol_destino = 3
      AND leido = FALSE
      AND mensaje_alerta LIKE '%' || TRIM(v_nombre_sup) || '%';

    -- 5. Insertar notificación para el Supervisor en Notificaciones (tipo_notificacion = 6)
    IF p_autorizar THEN
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
            p_supervisor_id,
            2,
            6, -- Concepto 6: Acceso a Dashboard Autorizado
            'Acceso a Dashboard Autorizado',
            'El Propietario ha autorizado formalmente tu acceso al Dashboard Ejecutivo.',
            '/dashboard',
            FALSE,
            CURRENT_TIMESTAMP,
            NULL
        );
        p_mensaje := 'Acceso al Dashboard concedido exitosamente al supervisor ' || TRIM(v_nombre_sup) || '.';
    ELSE
        p_mensaje := 'Acceso al Dashboard revocado/rechazado para el supervisor ' || TRIM(v_nombre_sup) || '.';
    END IF;

    p_estado := 'EXITO';

EXCEPTION
    WHEN OTHERS THEN
        p_estado := 'ERROR';
        p_mensaje := 'Error interno al resolver la solicitud de dashboard: ' || SQLERRM;
END;
$$;
