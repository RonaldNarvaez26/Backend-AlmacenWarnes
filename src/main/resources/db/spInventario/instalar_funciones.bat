@echo off
echo =======================================================
echo Instalando Procedimientos y Funciones en AlmacenWarnes
echo =======================================================

:: RUTA EXACTA A PSQL (Cambia el "18" o version por la de tu PostgreSQL si es diferente)
set "PSQL_PATH=C:\Program Files\PostgreSQL\18\bin\psql.exe"

:: Pide la contrasena una sola vez y la guarda temporalmente
set /p pg_pass="Ingresa la contrasena del usuario postgres: "
set PGPASSWORD=%pg_pass%

:: Ejecuta los scripts SQL en orden
echo Instalando 1/8: Parches DDL de Tablas...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "00_parches_ddl.sql"

echo Instalando 2/8: Generador de Codigos...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spProductosGenerarCodigoInterno.sql"

echo Instalando 3/8: Aprobacion de Sala de Espera...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spInventarioPendientesAprobar.sql"

echo Instalando 4/8: Listado de Lotes Activos...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spInventarioLotesListar.sql"

echo Instalando 5/8: Ingreso Directo Supervisor...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spInventarioIngresoDirectoSupervisor.sql"

echo Instalando 6/8: Resumen de KPIs...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spInventarioKpiResumen.sql"

echo Instalando 7/8: Catalogo Maestro...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "spProductosListarCatalogo.sql"

echo Instalando 8/8: Sincronizacion de Secuencias Auto-incrementables...
"%PSQL_PATH%" -U postgres -d AlmacenWarnes -f "secuencias.sql"

:: Limpia la contrasena de la memoria por seguridad
set PGPASSWORD=

echo =======================================================
echo Instalacion terminada exitosamente.
echo =======================================================
pause
