
# Auth Service — Ficha técnica

## 1. Información general

| Campo | Descripción |
|---|---|
| Función | Registro e inicio de sesión |
| Tecnologías | Java 21, Spring Boot, Spring Security |
| Puerto local | 8081 |
| Base de datos | PostgreSQL (`auth_db`) |
| Comunicación actual | REST con User Service |
| Kafka | Pendiente de integración |

## 2. ¿Qué hace?

El Auth Service administra las credenciales, verifica la identidad de los usuarios y genera tokens JWT para autenticar sus solicitudes.

Sus responsabilidades son:

- Registrar las credenciales de los usuarios.
- Verificar el correo y la contraseña.
- Generar y validar tokens JWT.
- Identificar al usuario autenticado y su rol.

El perfil del usuario, como nombre, correo y rol, se gestiona en User Service. Auth Service conserva el identificador del usuario y el hash de su contraseña.

## 3. Endpoints principales

Las rutas descritas son las internas de Auth Service. Las rutas externas pueden incluir un prefijo configurado en API Gateway.

### POST /register — Registrar usuario

**Objetivo:** crear el perfil del usuario mediante User Service y guardar sus credenciales.

Solicitud de ejemplo:

```json
{
  "nombre": "Usuario Demo",
  "email": "demo@example.com",
  "password": "ContraseñaDePrueba123"
}
```

Flujo:

1. Valida los datos recibidos.
2. Solicita a User Service la creación del perfil.
3. Obtiene el identificador del usuario.
4. Genera el hash BCrypt de la contraseña.
5. Guarda las credenciales en `auth_db`.

Respuesta de ejemplo:

```json
{
  "message": "Usuario registrado correctamente",
  "userId": 1
}
```

El identificador es ilustrativo.

### POST /login — Iniciar sesión

**Objetivo:** verificar las credenciales y generar un JWT.

Solicitud de ejemplo:

```json
{
  "email": "demo@example.com",
  "password": "ContraseñaDePrueba123"
}
```

Si las credenciales son correctas, devuelve el token, el identificador y el rol del usuario.

Respuesta de ejemplo:

```json
{
  "token": "<jwt>",
  "userId": 1,
  "rol": "USER"
}
```

El token y el identificador son ilustrativos.

### GET /me — Consultar identidad

**Objetivo:** consultar la identidad asociada al token JWT.

Encabezado requerido:

`Authorization: Bearer <jwt>`

Respuesta de ejemplo:

```json
{
  "userId": 1,
  "rol": "USER"
}
```

Requiere autenticación.

## 4. Estructura del código

| Componente | Función |
|---|---|
| `controller` | Recibe las solicitudes HTTP |
| `service` | Coordina el registro y el inicio de sesión |
| `client` | Se comunica con User Service |
| `security` y `config` | Configuran la seguridad y la autenticación JWT |
| `model` y `repository` | Gestionan las credenciales |
| `dto` | Define los datos de entrada y salida |
| `exception` | Gestiona los errores |

## 5. Comunicación con otros servicios

Actualmente, Auth Service utiliza REST para solicitar la creación y consulta de perfiles en User Service.

**Consideración para Docker:** `UserClient` utiliza actualmente `http://localhost:8082`. Dentro de un contenedor, `localhost` apunta al propio contenedor, por lo que debe revisarse la dirección utilizada para comunicarse con User Service.

Aunque el proyecto incluye configuración de Eureka, `UserClient` todavía utiliza una dirección fija. La integración mediante descubrimiento de servicios debe verificarse y configurarse si se desea utilizar.

## 6. Seguridad y datos

- Las contraseñas se almacenan como hashes BCrypt, no en texto plano.
- El JWT contiene el identificador del usuario, el rol y las fechas de emisión y expiración.
- La duración configurada del token es de una hora.
- La clave de firma se configura mediante la variable de entorno `JWT_SECRET`.
- Incluir un rol en el token no garantiza por sí solo que existan restricciones de acceso para cada rol.

La tabla `credential` almacena:

| Campo | Propósito |
|---|---|
| `id` | Identificador de la credencial |
| `userId` | Identificador asociado al perfil en User Service |
| `passwordHash` | Hash BCrypt de la contraseña |

Cada microservicio debe gestionar sus propios datos; no se debe acceder directamente a la base de datos de otro servicio.

## 7. Pruebas y pendientes

### Pruebas importantes

- Registro válido.
- Registro con correo duplicado.
- Login correcto e incorrecto.
- Acceso a `/me` sin token y con token.
- Rechazo de tokens inválidos o expirados.
- Comunicación con User Service.
- Ejecución de los servicios mediante Docker.

Los resultados deben marcarse como completados únicamente después de ejecutar las pruebas.

### Pendientes técnicos

- Verificar las direcciones y variables de entorno en Docker.
- Revisar la integración con Eureka.
- Confirmar las restricciones de acceso por rol.
- Documentar las rutas externas definitivas de API Gateway.
- Diseñar los eventos Kafka cuando se acuerden los flujos del sistema.

### Kafka y Saga

Kafka y Saga se documentarán cuando se implementen.

Para cada evento futuro se especificarán:

- Nombre y versión del evento.
- Microservicio productor y consumidores.
- Topic y estructura del mensaje.
- Momento en que se publica.
- Manejo de errores, reintentos y duplicados.

No se deben documentar eventos como implementados hasta que existan en el código y se hayan probado.

---

**Mantenimiento:** actualizar esta ficha cuando cambien los endpoints, los contratos, la configuración o el comportamiento del microservicio.
