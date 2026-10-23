# User Service — Ficha técnica

## 1. Información general

| Campo | Descripción |
|---|---|
| Función | Administrar los perfiles de los usuarios |
| Tecnologías | Java 21, Spring Boot, Spring Data JPA |
| Puerto local | 8082 |
| Base de datos | PostgreSQL (`user_db`) |
| Comunicación actual | REST con Auth Service |
| Descubrimiento | Eureka configurado |
| Kafka | Pendiente de integración |

## 2. ¿Qué hace?

El User Service administra la información de los perfiles de los usuarios de TechMarket. Permite crear usuarios, consultar el listado y buscar un perfil por identificador o correo electrónico.

Sus responsabilidades son:

- Crear perfiles de usuario.
- Consultar usuarios registrados.
- Buscar usuarios por identificador o correo.
- Asignar el rol `USER` al crear un perfil.
- Mantener la información de los usuarios en su propia base de datos.

Las credenciales y las contraseñas pertenecen a Auth Service. User Service no debe almacenar contraseñas ni hashes de contraseñas.

## 3. Endpoints principales

Las rutas descritas son las internas del servicio. Las rutas externas pueden cambiar según la configuración del API Gateway.

### POST /users — Crear usuario

**Objetivo:** registrar el perfil de un usuario.

Solicitud de ejemplo:

    {
      "nombre": "Usuario Demo",
      "email": "demo@example.com"
    }

El servicio valida el nombre y el formato del correo. Si el correo no está registrado, crea el perfil y asigna el rol `USER` automáticamente.

Respuesta representativa:

    {
      "id": 1,
      "nombre": "Usuario Demo",
      "email": "demo@example.com",
      "rol": "USER"
    }

El identificador es ilustrativo.

**Errores relevantes:**
- `400 Bad Request`: datos inválidos.
- `409 Conflict`: el correo ya está registrado.

### GET /users — Consultar usuarios

**Objetivo:** obtener la lista de perfiles registrados.

No requiere parámetros.

La respuesta es una lista de objetos de usuario con los campos que expone la entidad `User`.

### GET /users/{id} — Buscar por identificador

**Objetivo:** obtener un perfil por su ID.

Ejemplo:

    GET /users/1

Si el usuario no existe, el servicio genera un error `404 Not Found`.

### GET /users/email?email={email} — Buscar por correo

**Objetivo:** obtener un perfil utilizando su correo electrónico.

Ejemplo:

    GET /users/email?email=demo@example.com

Este endpoint lo utiliza Auth Service durante el inicio de sesión.

Si no existe un usuario con ese correo, el servicio genera un error `404 Not Found`. Auth Service transforma ese caso en un error de autenticación para no revelar si el correo está registrado.

## 4. Estructura del código y datos

| Componente | Función |
|---|---|
| `controller/UserController` | Expone los endpoints REST |
| `service/UserService` | Contiene las reglas de negocio |
| `repository/UserRepository` | Consulta y persiste usuarios |
| `model/User` | Representa el perfil del usuario |
| `dto/UserRequest` | Valida los datos recibidos al crear un perfil |

### Modelo de datos

La entidad `User` se almacena en la tabla `users`.

| Campo | Propósito |
|---|---|
| `id` | Identificador generado automáticamente |
| `nombre` | Nombre del usuario |
| `email` | Correo único y obligatorio |
| `rol` | Rol asignado al usuario |

El correo tiene una restricción de unicidad y no puede ser nulo.

## 5. Comunicación con otros servicios

### Integración con Auth Service

Auth Service utiliza este servicio durante el registro y el inicio de sesión.

**Durante el registro:**

1. Auth Service envía el nombre y el correo a `POST /users`.
2. User Service crea el perfil y asigna el rol `USER`.
3. Devuelve el usuario creado, incluido su identificador.
4. Auth Service utiliza ese identificador para guardar las credenciales.

**Durante el inicio de sesión:**

1. Auth Service solicita el perfil mediante `GET /users/email`.
2. User Service devuelve los datos del usuario.
3. Auth Service verifica la contraseña almacenada en su propia base de datos y genera el JWT si es correcta.

La comunicación actual es síncrona mediante REST.

### Consideración para Docker y Eureka

La configuración de User Service utiliza actualmente:

- Puerto `8082`.
- Base de datos `user_db`.
- URL de PostgreSQL configurada con `localhost`.
- URL de Eureka configurada con `localhost:8761`.

Estas direcciones deben revisarse al ejecutar los servicios dentro de Docker. En un contenedor, `localhost` apunta al propio contenedor, no automáticamente a otro servicio.

Aunque Eureka está configurado, debe comprobarse que el registro y el descubrimiento funcionen correctamente en el entorno de ejecución.

## 6. Seguridad y configuración

- La creación de usuarios asigna actualmente el rol `USER` de forma predeterminada.
- El correo debe ser único.
- La contraseña de PostgreSQL se obtiene mediante la variable de entorno `DB_PASSWORD`.
- No se deben subir contraseñas ni secretos al repositorio.

**Importante:** en el controlador revisado no se observan reglas de autorización por endpoint. Debemos verificar la configuración completa de seguridad y las rutas del Gateway para determinar qué operaciones están protegidas. Los endpoints de consulta, especialmente `GET /users`, no deberían exponer información sin los permisos correspondientes.

## 7. Pruebas y pendientes

### Pruebas importantes

- Crear un usuario válido.
- Rechazar un nombre o correo inválido.
- Rechazar un correo duplicado.
- Consultar todos los usuarios.
- Buscar un usuario por ID existente e inexistente.
- Buscar por correo existente e inexistente.
- Comprobar la comunicación con Auth Service.
- Verificar la ejecución mediante Docker y el registro en Eureka.

Los resultados deben marcarse como completados únicamente después de ejecutar las pruebas.

### Pendientes técnicos

- Verificar las direcciones de conexión en Docker.
- Confirmar el funcionamiento de Eureka.
- Revisar la autenticación y autorización de los endpoints.
- Definir si se necesitan operaciones adicionales para actualizar o eliminar perfiles.
- Documentar los eventos Kafka cuando se acuerden los flujos del sistema.

### Kafka

Kafka todavía está pendiente de integración en este servicio. Cuando se defina su participación, documentaremos los eventos que publica o consume, sus topics, los datos intercambiados y el manejo de errores.
