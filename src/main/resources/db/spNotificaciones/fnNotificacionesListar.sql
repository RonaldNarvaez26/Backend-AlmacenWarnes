-- ============================================================================================
-- AUTOR:           Kenneth Lopez / Ronald Narvaez
-- MODULO:          Notificaciones y Alertas Internas en Tiempo Real
-- DESCRIPCION:     Listado dinámico y consolidado de alertas activas según el rol y sucursal,
--                  integrado con la tabla física Notificaciones, Grupo 10 y Grupo 11 de Conceptos.
--                  Evalúa en tiempo real:
--                  1. Alertas registradas en tabla física Notificaciones (Grupo 10)
--                  2. Alertas dinámicas de Stock Físico (Agotado / Bajo)
--                  3. Solicitudes activas de acceso al Dashboard
--                  4. Alertas dinámicas de Vencimiento de Lotes (Vencidos y Próximos a Vencer por Categoría)
-- ============================================================================================

DROP FUNCTION IF EXISTS fn_notificaciones_listar(INT, INT, INT);
DROP FUNCTION IF EXISTS fn_notificaciones_listar(INT, INT);
DROP FUNCTION IF EXISTS fn_notificaciones_listar(INT);
DROP FUNCTION IF EXISTS fn_notificaciones_listar();

CREATE OR REPLACE FUNCTION fn_notificaciones_listar(
    p_usuario_id INT,
    p_rol_id INT,
    p_sucursal_id INT DEFAULT NULL
)
RETURNS TABLE (
    id_notificacion VARCHAR,
    tipo_alerta VARCHAR,
    severidad VARCHAR,
    titulo VARCHAR,
    mensaje VARCHAR,
    sucursal_id INT,
    codigo_sucursal VARCHAR,
    referencia_id INT,
    ruta_accion VARCHAR,
    fecha_evento TIMESTAMP,
    leido BOOLEAN,
    fecha_leido TIMESTAMP,
    tipo_notificacion INT
)
LANGUAGE plpgsql
AS $$
#variable_conflict use_column
DECLARE
    v_es_propietario BOOLEAN := (p_rol_id = 3);
    v_es_supervisor  BOOLEAN := (p_rol_id = 2);
    v_es_cajero      BOOLEAN := (p_rol_id = 1);
BEGIN
    RETURN QUERY
    WITH alertas_consolidadas AS (
        -- =========================================================================
        -- 1. NOTIFICACIONES ACTIVAS DE LA TABLA FISICA NOTIFICACIONES
        -- Mapeadas con Grupo 10 de Conceptos (tipo_notificacion)
        -- =========================================================================
        SELECT 
            n.id_notificacion::VARCHAR AS id_notificacion,
            CASE n.tipo_notificacion
                WHEN 1 THEN 'STOCK_AGOTADO'
                WHEN 2 THEN 'STOCK_BAJO'
                WHEN 3 THEN 'SALA_ESPERA'
                WHEN 4 THEN 'SALA_ESPERA_OBSERVADO'
                WHEN 5 THEN 'ACCESO_DASHBOARD'
                WHEN 6 THEN 'ACCESO_DASHBOARD_APROBADO'
                WHEN 7 THEN 'RECUPERACION_PASSWORD'
                WHEN 8 THEN 'USUARIO_BLOQUEADO'
                WHEN 9 THEN 'LOTE_PROXIMO_VENCER'
                WHEN 10 THEN 'LOTE_VENCIDO'
                ELSE 'GENERAL'
            END::VARCHAR AS tipo_alerta,
            CASE n.tipo_notificacion
                WHEN 1 THEN 'CRITICA'
                WHEN 2 THEN 'ADVERTENCIA'
                WHEN 3 THEN 'ALTA'
                WHEN 4 THEN 'ALTA'
                WHEN 5 THEN 'ALTA'
                WHEN 6 THEN 'INFORMATIVA'
                WHEN 7 THEN 'CRITICA'
                WHEN 8 THEN 'ALTA'
                WHEN 9 THEN 'ADVERTENCIA'
                WHEN 10 THEN 'CRITICA'
                ELSE 'INFORMATIVA'
            END::VARCHAR AS severidad,
            n.titulo_alerta::VARCHAR AS titulo,
            n.mensaje_alerta::VARCHAR AS mensaje,
            p_sucursal_id AS sucursal_id,
            COALESCE((SELECT s.codigo_interno FROM sucursales s WHERE s.sucursal_id = p_sucursal_id), 'WARNES')::VARCHAR AS codigo_sucursal,
            COALESCE(n.usuario_destino, n.id_notificacion) AS referencia_id,
            n.direccion::VARCHAR AS ruta_accion,
            n.fecha_emision::TIMESTAMP AS fecha_evento,
            n.leido::BOOLEAN AS leido,
            n.fecha_leido::TIMESTAMP AS fecha_leido,
            n.tipo_notificacion::INT AS tipo_notificacion
        FROM notificaciones n
        JOIN conceptos c ON c.grupo_id = 10 AND c.codigo = n.tipo_notificacion AND c.marca_baja = FALSE
        WHERE n.leido = FALSE
          AND (
              -- Destinada específicamente a este usuario
              n.usuario_destino = p_usuario_id
              OR
              -- O destinada a su rol (o broadcast) sin usuario exclusivo
              (n.usuario_destino IS NULL AND (n.rol_destino IS NULL OR n.rol_destino = p_rol_id))
          )

        UNION ALL

        -- =========================================================================
        -- 2. ALERTAS DINAMICAS EN TIEMPO REAL: STOCK FISICO (Agotado = 0, Bajo = 1..10)
        -- =========================================================================
        SELECT 
            ('STOCK_' || i.sucursal_id || '_' || i.producto_id)::VARCHAR AS id_notificacion,
            CASE 
                WHEN COALESCE(SUM(i.stock), 0) = 0 THEN 'STOCK_AGOTADO'::VARCHAR
                ELSE 'STOCK_BAJO'::VARCHAR
            END AS tipo_alerta,
            CASE 
                WHEN COALESCE(SUM(i.stock), 0) = 0 THEN 'CRITICA'::VARCHAR
                ELSE 'ADVERTENCIA'::VARCHAR
            END AS severidad,
            CASE 
                WHEN COALESCE(SUM(i.stock), 0) = 0 THEN 'Producto Agotado (Stock 0)'::VARCHAR
                ELSE ('Stock por Agotarse (' || SUM(i.stock) || ' u.)')::VARCHAR
            END AS titulo,
            CASE 
                WHEN COALESCE(SUM(i.stock), 0) = 0 
                    THEN ('El producto ' || p.nombre_producto || ' (' || p.codigo_interno || ') se encuentra sin stock en ' || s.codigo_interno)::VARCHAR
                ELSE ('El producto ' || p.nombre_producto || ' tiene solo ' || SUM(i.stock) || ' unidades disponibles en ' || s.codigo_interno)::VARCHAR
            END AS mensaje,
            i.sucursal_id,
            s.codigo_interno::VARCHAR AS codigo_sucursal,
            i.producto_id AS referencia_id,
            '/inventario'::VARCHAR AS ruta_accion,
            MAX(i.fecha_creacion)::TIMESTAMP AS fecha_evento,
            FALSE::BOOLEAN AS leido,
            NULL::TIMESTAMP AS fecha_leido,
            CASE 
                WHEN COALESCE(SUM(i.stock), 0) = 0 THEN 1
                ELSE 2
            END::INT AS tipo_notificacion
        FROM inventario i
        JOIN productos p ON p.producto_id = i.producto_id AND p.marca_baja = FALSE
        JOIN sucursales s ON s.sucursal_id = i.sucursal_id AND s.marca_baja = FALSE
        WHERE i.marca_baja = FALSE
          AND (
              (v_es_propietario AND (p_sucursal_id IS NULL OR p_sucursal_id = 0 OR i.sucursal_id = p_sucursal_id))
              OR
              ((v_es_supervisor OR v_es_cajero) AND i.sucursal_id = p_sucursal_id)
          )
        GROUP BY i.sucursal_id, s.codigo_interno, i.producto_id, p.nombre_producto, p.codigo_interno
        HAVING COALESCE(SUM(i.stock), 0) <= 10

        UNION ALL

        -- =========================================================================
        -- 3. SOLICITUDES ACTIVAS DE DASHBOARD (GRUPO 10) DIRECTAS
        -- Para Propietario (3): Solicitudes pendientes de supervisores
        -- =========================================================================
        SELECT 
            ('DASH_REQ_' || u.usuario_id)::VARCHAR AS id_notificacion,
            'ACCESO_DASHBOARD'::VARCHAR AS tipo_alerta,
            'ALTA'::VARCHAR AS severidad,
            'Solicitud de Acceso a Dashboard'::VARCHAR AS titulo,
            ('El Supervisor ' || u.nombres || ' ' || COALESCE(u.apellidos, '') || 
             ' (@' || u.usuario || ') solicita autorización para visualizar el Dashboard.')::VARCHAR AS mensaje,
            u.sucursal_id,
            COALESCE(s.codigo_interno, 'CENTRAL')::VARCHAR AS codigo_sucursal,
            u.usuario_id AS referencia_id,
            '/dashboard'::VARCHAR AS ruta_accion,
            c.fecha_creacion::TIMESTAMP AS fecha_evento,
            FALSE::BOOLEAN AS leido,
            NULL::TIMESTAMP AS fecha_leido,
            5::INT AS tipo_notificacion
        FROM conceptos c
        JOIN usuarios u ON u.usuario_id = c.codigo AND u.marca_baja = FALSE AND u.rol_id = 2
        LEFT JOIN sucursales s ON s.sucursal_id = u.sucursal_id
        WHERE v_es_propietario
          AND c.grupo_id = 10
          AND c.codigo > 0
          AND UPPER(TRIM(c.descripcion)) = 'PENDIENTE'
          AND c.marca_baja = FALSE

        UNION ALL

        -- =========================================================================
        -- 4. ALERTAS DINAMICAS DE VENCIMIENTO POR CATEGORIA (GRUPOS 10 Y 11)
        -- Cruzando Inventario, Lotes, Productos, Sucursales, Categoría y Umbral
        -- =========================================================================
        SELECT 
            CASE 
                WHEN (l.fecha_vencimiento - CURRENT_DATE) < 0 
                    THEN ('LOTE_VENC_' || i.sucursal_id || '_' || l.lote_id)::VARCHAR
                ELSE ('LOTE_POR_VENCER_' || i.sucursal_id || '_' || l.lote_id)::VARCHAR
            END AS id_notificacion,
            CASE 
                WHEN (l.fecha_vencimiento - CURRENT_DATE) < 0 THEN 'LOTE_VENCIDO'::VARCHAR
                ELSE 'LOTE_PROXIMO_VENCER'::VARCHAR
            END AS tipo_alerta,
            CASE 
                WHEN (l.fecha_vencimiento - CURRENT_DATE) < 0 THEN 'CRITICA'::VARCHAR
                WHEN (l.fecha_vencimiento - CURRENT_DATE) <= 7 THEN 'ALTA'::VARCHAR
                ELSE 'ADVERTENCIA'::VARCHAR
            END AS severidad,
            CASE 
                WHEN (l.fecha_vencimiento - CURRENT_DATE) < 0 
                    THEN ('Lote Vencido (' || cat.descripcion || ')')::VARCHAR
                ELSE ('Próximo a Vencer en ' || (l.fecha_vencimiento - CURRENT_DATE) || ' días (' || cat.descripcion || ')')::VARCHAR
            END AS titulo,
            CASE 
                WHEN (l.fecha_vencimiento - CURRENT_DATE) < 0 
                    THEN ('El lote ' || l.descripcion || ' de ' || p.nombre_producto || ' (' || i.stock || ' u.) en ' || s.codigo_interno || ' venció el ' || TO_CHAR(l.fecha_vencimiento, 'DD/MM/YYYY') || '.')::VARCHAR
                ELSE ('El lote ' || l.descripcion || ' de ' || p.nombre_producto || ' (' || i.stock || ' u. en ' || s.codigo_interno || ') vence el ' || TO_CHAR(l.fecha_vencimiento, 'DD/MM/YYYY') || ' (Umbral ' || cat.descripcion || ': ' || COALESCE(NULLIF(regexp_replace(umb.descripcion, '[^0-9]', '', 'g'), ''), '30')::INT || ' días).')::VARCHAR
            END AS mensaje,
            i.sucursal_id,
            s.codigo_interno::VARCHAR AS codigo_sucursal,
            l.lote_id AS referencia_id,
            '/inventario'::VARCHAR AS ruta_accion,
            COALESCE(l.fecha_creacion, CURRENT_TIMESTAMP)::TIMESTAMP AS fecha_evento,
            FALSE::BOOLEAN AS leido,
            NULL::TIMESTAMP AS fecha_leido,
            CASE 
                WHEN (l.fecha_vencimiento - CURRENT_DATE) < 0 THEN 10
                ELSE 9
            END::INT AS tipo_notificacion
        FROM inventario i
        JOIN lotes l ON l.lote_id = i.lote_id AND l.marca_baja = FALSE AND l.fecha_vencimiento IS NOT NULL
        JOIN productos p ON p.producto_id = i.producto_id AND p.marca_baja = FALSE
        JOIN sucursales s ON s.sucursal_id = i.sucursal_id AND s.marca_baja = FALSE
        JOIN conceptos cat ON cat.grupo_id = 2 AND cat.codigo = p.categoria_id AND cat.marca_baja = FALSE
        LEFT JOIN conceptos umb ON umb.grupo_id = 11 AND umb.codigo = cat.codigo AND umb.marca_baja = FALSE
        WHERE i.marca_baja = FALSE
          AND i.stock > 0
          AND (
              -- Lote Vencido (< 0) O Próximo a Vencer (0 <= dias_restantes <= umbral)
              (l.fecha_vencimiento - CURRENT_DATE) < 0
              OR
              (l.fecha_vencimiento - CURRENT_DATE) <= COALESCE(NULLIF(regexp_replace(umb.descripcion, '[^0-9]', '', 'g'), ''), '30')::INT
          )
          AND (
              (v_es_propietario AND (p_sucursal_id IS NULL OR p_sucursal_id = 0 OR i.sucursal_id = p_sucursal_id))
              OR
              ((v_es_supervisor OR v_es_cajero) AND i.sucursal_id = p_sucursal_id)
          )
    )
    SELECT DISTINCT ON (alertas_consolidadas.id_notificacion)
        alertas_consolidadas.id_notificacion,
        alertas_consolidadas.tipo_alerta,
        alertas_consolidadas.severidad,
        alertas_consolidadas.titulo,
        alertas_consolidadas.mensaje,
        alertas_consolidadas.sucursal_id,
        alertas_consolidadas.codigo_sucursal,
        alertas_consolidadas.referencia_id,
        alertas_consolidadas.ruta_accion,
        alertas_consolidadas.fecha_evento,
        alertas_consolidadas.leido,
        alertas_consolidadas.fecha_leido,
        alertas_consolidadas.tipo_notificacion
    FROM alertas_consolidadas
    ORDER BY 
        alertas_consolidadas.id_notificacion,
        CASE alertas_consolidadas.severidad
            WHEN 'CRITICA' THEN 1
            WHEN 'ALTA' THEN 2
            WHEN 'ADVERTENCIA' THEN 3
            ELSE 4
        END,
        alertas_consolidadas.fecha_evento DESC;
END;
$$;
