@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul
set PGCLIENTENCODING=UTF8

echo ====================================================================
echo   SISTEMA ERP ALMACEN WARNES - EJECUTOR DE STORED PROCEDURES
echo   Modulos: Inventario y Proveedores
echo ====================================================================
echo.

:: 1. RESOLUCION AUTOMATICA DE DIRECTORIOS
set "SCRIPT_DIR=%~dp0"
if exist "!SCRIPT_DIR!spProveedor" (
    set "PROV_DIR=!SCRIPT_DIR!spProveedor"
    set "INV_DIR=!SCRIPT_DIR!spInventario"
) else if exist "!SCRIPT_DIR!src\main\resources\db\spProveedor" (
    set "PROV_DIR=!SCRIPT_DIR!src\main\resources\db\spProveedor"
    set "INV_DIR=!SCRIPT_DIR!src\main\resources\db\spInventario"
) else (
    echo [ERROR] No se encontraron las carpetas 'spProveedor' e 'spInventario'.
    echo Ubicacion actual: !SCRIPT_DIR!
    echo.
    pause
    exit /b 1
)

:: 2. DETECCION AUTOMATICA DE PSQL
set "PSQL_BIN="
where psql.exe >nul 2>&1
if !errorlevel! equ 0 (
    set "PSQL_BIN=psql.exe"
) else (
    for %%V in (16 17 18 15 14 13 12) do (
        if not defined PSQL_BIN (
            if exist "C:\Program Files\PostgreSQL\%%V\bin\psql.exe" (
                set "PSQL_BIN=C:\Program Files\PostgreSQL\%%V\bin\psql.exe"
            )
        )
    )
)

if not defined PSQL_BIN (
    echo [ERROR] No se encontro psql.exe en el sistema ni en Program Files\PostgreSQL.
    echo Por favor ingresa la ruta completa a psql.exe:
    set /p "PSQL_BIN=> "
    if not exist "!PSQL_BIN!" (
        echo [ERROR] La ruta especificada no existe. Abortando.
        echo.
        pause
        exit /b 1
    )
)

echo [OK] Motor PostgreSQL detectado: "!PSQL_BIN!"
echo.

:: 3. PARAMETROS DE CONEXION
set "DB_HOST=127.0.0.1"
set "DB_PORT=5432"
set "DB_NAME=AlmacenWarnes"
set "DB_USER=postgres"

echo Configuracion de conexion:
echo  - Host:       %DB_HOST%:%DB_PORT%
echo  - Base Datos: %DB_NAME%
echo  - Usuario:    %DB_USER%
echo.

set "INPUT_PASS="
set /p "INPUT_PASS=Ingresa la contrasena de %DB_USER% [ENTER para 'admin']: "
if "!INPUT_PASS!"=="" (
    set "PGPASSWORD=admin"
) else (
    set "PGPASSWORD=!INPUT_PASS!"
)

:: Test de conexion rapido
echo.
echo Probando conexion con PostgreSQL...
"!PSQL_BIN!" -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT 1;" >nul 2>&1
if !errorlevel! neq 0 (
    echo [ERROR] No se pudo conectar a la base de datos '%DB_NAME%' con el usuario '%DB_USER%'.
    echo Verifica que el servicio de PostgreSQL este activo y que la contrasena sea correcta.
    echo.
    set PGPASSWORD=
    pause
    exit /b 1
)
echo [OK] Conexion exitosa con PostgreSQL.
echo.

set /a TOTAL_EXITO=0
set /a TOTAL_FALLO=0

:: ====================================================================
:: 4. EJECUTAR PROCEDIMIENTOS DE PROVEEDOR
:: ====================================================================
echo ====================================================================
echo [1/2] EJECUTANDO PROCEDIMIENTOS DEL MODULO PROVEEDOR
echo ====================================================================

call :EJECUTAR_SQL "!PROV_DIR!\spProveedorKpiResumen.sql" "1/5. KPIs de Proveedores"
call :EJECUTAR_SQL "!PROV_DIR!\spProveedorListar.sql" "2/5. Listado Avanzado de Proveedores"
call :EJECUTAR_SQL "!PROV_DIR!\spProveedorAbm.sql" "3/5. Funciones ABM - Registrar, Modificar, Baja"
call :EJECUTAR_SQL "!PROV_DIR!\spProveedorCatalogoProductos.sql" "4/5. Catalogo de Productos por Proveedor"
call :EJECUTAR_SQL "!PROV_DIR!\spProveedorHistorialLotes.sql" "5/5. Historial de Lotes y Trazabilidad"

echo.

:: ====================================================================
:: 5. EJECUTAR PROCEDIMIENTOS DE INVENTARIO
:: ====================================================================
echo ====================================================================
echo [2/2] EJECUTANDO PROCEDIMIENTOS DEL MODULO INVENTARIO
echo ====================================================================

call :EJECUTAR_SQL "!INV_DIR!\spProductosGenerarCodigoInterno.sql" "1/9. Generador de Codigo Interno"
call :EJECUTAR_SQL "!INV_DIR!\spInventarioPendientesAprobar.sql" "2/9. Aprobacion de Sala de Espera"
call :EJECUTAR_SQL "!INV_DIR!\spInventarioLotesListar.sql" "3/9. Listado de Lotes y Stock Fisico"
call :EJECUTAR_SQL "!INV_DIR!\spInventarioIngresoDirectoSupervisor.sql" "4/9. Ingreso Directo Supervisor"
call :EJECUTAR_SQL "!INV_DIR!\spInventarioKpiResumen.sql" "5/9. Resumen de KPIs de Inventario"
call :EJECUTAR_SQL "!INV_DIR!\spProductosListarCatalogo.sql" "6/9. Catalogo Maestro de Productos"
call :EJECUTAR_SQL "!INV_DIR!\spInventarioDescontarUnidades.sql" "7/9. Descuento Individual de Unidades"
call :EJECUTAR_SQL "!INV_DIR!\InsertarDatosConceptos.sql" "8/9. Catalogo Parametrico de Conceptos"
call :EJECUTAR_SQL "!INV_DIR!\secuencias.sql" "9/9. Sincronizacion de Secuencias"

echo.
echo ====================================================================
echo RESUMEN DE EJECUCION:
echo  - Exitosos: !TOTAL_EXITO!
echo  - Con error: !TOTAL_FALLO!
echo ====================================================================

set PGPASSWORD=

if !TOTAL_FALLO! equ 0 (
    echo [EXITO TOTAL] Todos los SPs de Proveedor e Inventario se compilaron correctamente en '%DB_NAME%'.
) else (
    echo [ATENCION] Hubo scripts con errores. Revisa el detalle mostrado arriba.
)

echo.
echo Presiona cualquier tecla para cerrar esta ventana...
pause >nul
exit /b 0

:: --------------------------------------------------------------------
:: FUNCION PARA EJECUTAR UN ARCHIVO SQL
:: --------------------------------------------------------------------
:EJECUTAR_SQL
set "ARCHIVO=%~1"
set "DESCRIPCION=%~2"

if not exist "!ARCHIVO!" (
    echo    [NO ENCONTRADO] No existe el archivo: %~nx1
    set /a TOTAL_FALLO+=1
    goto :eof
)

echo  - Instalando !DESCRIPCION!...
"!PSQL_BIN!" -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "!ARCHIVO!" >nul 2>&1
if !errorlevel! equ 0 (
    echo    [OK] %~nx1
    set /a TOTAL_EXITO+=1
) else (
    echo    [FALLO] Error al ejecutar %~nx1. Mostrando detalle:
    "!PSQL_BIN!" -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "!ARCHIVO!"
    set /a TOTAL_FALLO+=1
)
goto :eof
