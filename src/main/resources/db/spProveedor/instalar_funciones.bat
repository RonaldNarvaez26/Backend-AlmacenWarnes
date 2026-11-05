@echo off
echo =======================================================
echo Instalando Modulo de Proveedores y Logistica en AlmacenWarnes
echo =======================================================

:: RUTA EXACTA A PSQL (Ajusta la version si es necesario)
set "PSQL_PATH=C:\Program Files\PostgreSQL\18\bin\psql.exe"

:: Pide la contrasena una sola vez y la guarda temporalmente
set /p pg_pass="Ingresa la contrasena del usuario postgres: "
set PGPASSWORD=%pg_pass%

echo Instalando 1/6: Resumen de KPIs de Proveedores...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spProveedorKpiResumen.sql"

echo Instalando 2/6: Listado con Busqueda Avanzada de Proveedores...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spProveedorListar.sql"

echo Instalando 3/6: Procedimientos ABM (Registrar, Actualizar, Baja)...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spProveedorAbm.sql"

echo Instalando 4/6: Catalogo de Productos por Proveedor...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spProveedorCatalogoProductos.sql"

echo Instalando 5/6: Historial de Lotes y Trazabilidad...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spProveedorHistorialLotes.sql"

echo Instalando 6/6: Datos Semilla de Contratos Logísticos...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "entradaDatos.sql"

:: Limpia la contrasena de la memoria por seguridad
set PGPASSWORD=

echo =======================================================
echo Instalacion de Modulo de Proveedores completada exitosamente.
echo =======================================================
pause

