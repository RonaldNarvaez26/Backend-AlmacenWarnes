@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul
set PGCLIENTENCODING=UTF8

echo =======================================================
echo Instalando Procedimientos y Funciones de Proveedores
echo Base de datos: AlmacenWarnes
echo =======================================================
echo.

:: DETECCION AUTOMATICA DE PSQL
set "PSQL_PATH="
where psql.exe >nul 2>&1
if !errorlevel! equ 0 (
    set "PSQL_PATH=psql.exe"
) else (
    for %%V in (16 17 18 15 14 13 12) do (
        if not defined PSQL_PATH (
            if exist "C:\Program Files\PostgreSQL\%%V\bin\psql.exe" (
                set "PSQL_PATH=C:\Program Files\PostgreSQL\%%V\bin\psql.exe"
            )
        )
    )
)

if not defined PSQL_PATH (
    echo [ERROR] No se encontro psql.exe.
    set /p "PSQL_PATH=Ingresa la ruta completa a psql.exe: "
)

echo [OK] Usando: "!PSQL_PATH!"
echo.

:: Pide la contrasena una sola vez o usa admin por defecto
set /p pg_pass="Ingresa la contrasena de postgres [ENTER para 'admin']: "
if "%pg_pass%"=="" (
    set "PGPASSWORD=admin"
) else (
    set "PGPASSWORD=%pg_pass%"
)

echo Instalando 1/5: Resumen de KPIs de Proveedores...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spProveedorKpiResumen.sql"

echo Instalando 2/5: Listado con Busqueda Avanzada de Proveedores...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spProveedorListar.sql"

echo Instalando 3/5: Procedimientos ABM (Registrar, Actualizar, Baja)...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spProveedorAbm.sql"

echo Instalando 4/5: Catalogo de Productos por Proveedor...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spProveedorCatalogoProductos.sql"

echo Instalando 5/5: Historial de Lotes y Trazabilidad...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spProveedorHistorialLotes.sql"

:: Limpia la contrasena de la memoria por seguridad
set PGPASSWORD=

echo =======================================================
echo Instalacion de Modulo de Proveedores completada.
echo =======================================================
pause
