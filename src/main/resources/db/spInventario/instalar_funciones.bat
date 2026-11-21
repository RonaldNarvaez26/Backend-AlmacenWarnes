@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul
set PGCLIENTENCODING=UTF8

echo =======================================================
echo Instalando Procedimientos y Funciones de Inventario
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

:: Ejecuta los scripts SQL en orden
echo Instalando 1/9: Generador de Codigos...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spProductosGenerarCodigoInterno.sql"

echo Instalando 2/9: Aprobacion de Sala de Espera...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spInventarioPendientesAprobar.sql"

echo Instalando 3/9: Listado de Lotes Activos...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spInventarioLotesListar.sql"

echo Instalando 4/9: Ingreso Directo Supervisor...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spInventarioIngresoDirectoSupervisor.sql"

echo Instalando 5/9: Resumen de KPIs...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spInventarioKpiResumen.sql"

echo Instalando 6/9: Catalogo Maestro...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spProductosListarCatalogo.sql"

echo Instalando 7/9: Descuento Individual de Unidades...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0spInventarioDescontarUnidades.sql"

echo Instalando 8/9: Tabla Parametrica de Conceptos...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0InsertarDatosConceptos.sql"

echo Instalando 9/9: Sincronizacion de Secuencias Auto-incrementables...
"%PSQL_PATH%" -h localhost -p 5432 -U postgres -d AlmacenWarnes -f "%~dp0secuencias.sql"

:: Limpia la contrasena de la memoria por seguridad
set PGPASSWORD=

echo =======================================================
echo Instalacion de Inventario terminada.
echo =======================================================
pause
