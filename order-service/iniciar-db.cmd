@echo off
setlocal
cd /d "%~dp0"
docker info >nul 2>&1
if errorlevel 1 (
 echo Abre Docker Desktop y espera a que inicie. Despues vuelve a ejecutar este archivo.
 pause
 exit /b 1
)
docker container inspect order-db-local >nul 2>&1
if errorlevel 1 (
 docker run -d --name order-db-local -p 127.0.0.1:5436:5432 -e POSTGRES_DB=order-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=techmarket_local -v order-db-local-data:/var/lib/postgresql/data postgres:16
) else (
 docker start order-db-local
)
if errorlevel 1 (
 echo No se pudo iniciar. Revisa el error anterior y si el puerto 5436 esta ocupado.
 pause
 exit /b 1
)
for /l %%i in (1,1,30) do (
 docker exec order-db-local pg_isready -U postgres -d order-db >nul 2>&1
 if not errorlevel 1 goto listo
 timeout /t 2 /nobreak >nul
)
echo La base aun no responde. Revisa Docker Desktop y sus logs.
pause
exit /b 1
:listo
echo Base order-db lista en localhost:5436.
echo Usuario: postgres. Clave local: techmarket_local.
pause
