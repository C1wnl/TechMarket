# TechMarket — Payment Service, Notification Service y Apache Kafka

Implementación de dos microservicios para gestionar pagos y notificaciones con **comunicación asíncrona** mediante Apache Kafka. Este README describe el código revisado de `Proyectos MS.zip` y las pruebas manuales reportadas posteriormente. **Los cambios locales posteriores al ZIP deben comprobarse antes de publicar.** Los servicios de Auth, User, Product y Order del equipo **no están integrados aquí**.

## Tecnologías

- Java 21, Maven, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Jakarta Validation.
- PostgreSQL 16 (una base de datos por microservicio).
- Apache Kafka 4.1.2, modo KRaft, ejecutado con Docker Compose.
- Spring Kafka, Jackson 3, Postman, Docker Desktop.

## Estructura

```text
TechMarket/
├── payment-service/
│   ├── compose.yaml
│   ├── pom.xml
│   └── src/main/java/com/marketplace/payment/
│       ├── controller/       # PaymentController
│       ├── dto/              # PaymentRequest, PaymentStatusRequest
│       ├── event/            # PaymentEvent
│       ├── exception/        # GlobalExceptionHandler
│       ├── model/            # Payment
│       ├── outbox/           # PaymentOutboxEvent, Repository, Publisher
│       ├── repository/       # PaymentRepository
│       └── service/          # PaymentService
├── notification-service/
│   ├── compose.yaml
│   ├── pom.xml
│   └── src/main/java/com/marketplace/notification/
│       ├── config/           # KafkaErrorConfig
│       ├── controller/       # NotificationController
│       ├── dto/              # NotificationRequest
│       ├── event/            # PaymentEvent
│       ├── kafka/            # PaymentEventConsumer
│       ├── model/            # Notification, ProcessedPaymentEvent
│       ├── repository/       # NotificationRepository, ProcessedPaymentEventRepository
│       └── service/          # NotificationService
└── kafka/
    └── docker-compose.yaml
```

## Arquitectura

```text
POST /payments → Payment Service (8085) → PostgreSQL payment-db (5437)
PUT /payments/{id}/status
         └─ transacción PostgreSQL: cambiar estado + insertar payment_outbox
              └─ PaymentOutboxPublisher (tarea periódica)
                   └─ Kafka (localhost:9092), topic payment-events
                        └─ Notification Service (8086), PaymentEventConsumer
                             ├─ PostgreSQL notification-db (5438)
                             │    ├─ notifications
                             │    └─ processed_payment_events (eventId único)
                             └─ Si falla: reintentos → topic payment-events-dlt
```

Los estados finales de pago son `APROBADO`, `RECHAZADO` y `CANCELADO`. Solo cambios hacia `APROBADO` o `RECHAZADO` generan eventos. El estado inicial de una **notificación** es `PENDIENTE` y no representa el estado del pago; no implica entrega real por correo o mensajería.

## Requisitos y puertos

| Componente | Puerto local | Ubicación |
|---|---:|---|
| Payment REST API | 8085 | IntelliJ / Java |
| Notification REST API | 8086 | IntelliJ / Java |
| PostgreSQL Payment | 5437 | Docker |
| PostgreSQL Notification | 5438 | Docker |
| Kafka | 9092 | Docker |

Se requieren Java 21, Docker Desktop, Maven o IntelliJ IDEA con soporte Maven, y Postman (opcional para las pruebas HTTP).

> **Seguridad local:** los archivos entregados incluyen la contraseña de ejemplo `techmarket_local`. Usar solo para desarrollo en la propia máquina; reemplazarla mediante variables de entorno en despliegues compartidos. No publicar credenciales reales.

## Puesta en marcha (Windows PowerShell)

Los comandos deben ejecutarse desde las carpetas indicadas. Los microservicios Java se ejecutan **en el host**, no dentro de Docker; por eso utilizan `localhost` para llegar a PostgreSQL y Kafka.

**1. Bases de datos:**

```powershell
cd payment-service
docker compose up -d
cd ..\notification-service
docker compose up -d
```

**2. Kafka:**

Usar el Compose **portátil** incluido como archivo aparte (`kafka/docker-compose.yaml`), no el que contiene el identificador del volumen de una computadora. Si se preservan datos locales existentes, hacer una migración o seguir usando el Compose local hasta terminar las pruebas.

```powershell
cd ..\kafka
docker compose -f docker-compose.yaml up -d
```

**3. Crear los topics (una sola vez en un broker nuevo):**

```powershell
docker exec techmarket-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic payment-events --partitions 1 --replication-factor 1
docker exec techmarket-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic payment-events-dlt --partitions 1 --replication-factor 1
docker exec techmarket-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

**4. Iniciar aplicaciones:** abrir `payment-service` y `notification-service` en IntelliJ y ejecutar `PaymentServiceApplication` y `NotificationServiceApplication` respectivamente. También se pueden ejecutar mediante `mvn spring-boot:run` desde cada carpeta si Maven está instalado y configurado.

Configuraciones principales: `payment-service/src/main/resources/application.properties` y `notification-service/src/main/resources/application.properties`. Ambas aceptan `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS` según su archivo actual. El broker anunciado (`localhost:9092`) está pensado para aplicaciones ejecutadas en Windows, no para otros contenedores.

## API de pagos (localhost:8085)

| Método | Ruta | Acción |
|---|---|---|
| POST | `/payments` | Crear pago en `PENDIENTE` (201) |
| GET | `/payments` | Listar pagos |
| GET | `/payments/{id}` | Consultar pago |
| GET | `/payments/order/{orderId}` | Listar por pedido |
| GET | `/payments/user/{userId}` | Listar por usuario |
| PUT | `/payments/{id}/status` | Cambiar estado |

Crear pago:

```http
POST http://localhost:8085/payments
Content-Type: application/json
```

```json
{"orderId":30,"userId":1,"monto":3200.00,"metodoPago":"TRANSFERENCIA"}
```

Actualizar estado (usar el ID devuelto en la creación):

```http
PUT http://localhost:8085/payments/1/status
Content-Type: application/json
```

```json
{"estado":"APROBADO"}
```

Estados válidos: `PENDIENTE`, `APROBADO`, `RECHAZADO`, `CANCELADO`. Un estado final no puede cambiarse a otro distinto; el mismo estado no vuelve a generar evento. Solicitudes inválidas devuelven errores HTTP (por ejemplo, 400, 404 o 409 según el caso).

## API de notificaciones (localhost:8086)

| Método | Ruta | Acción |
|---|---|---|
| POST | `/notifications` | Crear notificación manual |
| GET | `/notifications` | Listar |
| GET | `/notifications/{id}` | Consultar por ID |
| GET | `/notifications/user/{userId}` | Listar por usuario |
| PUT | `/notifications/{id}/status` | Cambiar estado |

Ejemplo de POST manual:

```json
{"userId":1,"tipo":"PAGO_APROBADO","mensaje":"Tu pago fue aprobado correctamente."}
```

Ejemplo de actualización de estado:

```json
{"estado":"ENVIADA"}
```

Los estados permitidos por el Controller son `PENDIENTE`, `ENVIADA` y `LEIDA`. Un mismo `userId` puede tener múltiples notificaciones. El identificador único de cada registro es `id`.

## Contrato de evento Kafka

Topic principal: `payment-events`.

```json
{
  "eventId": "uuid-del-evento",
  "eventType": "PAYMENT_APPROVED",
  "paymentId": 5,
  "orderId": 20,
  "userId": 1,
  "monto": 1850.00,
  "estado": "APROBADO",
  "fechaEvento": "2026-10-26T05:09:11Z"
}
```

Tipos aceptados: `PAYMENT_APPROVED` con estado `APROBADO`, y `PAYMENT_REJECTED` con estado `RECHAZADO`. Un evento inválido pasa por 1 intento inicial + 2 reintentos (intervalo aproximado de 1 segundo), y, si no logra procesarse, se publica en `payment-events-dlt`.

**Outbox:** Payment registra el cambio de estado y el JSON en la tabla `payment_outbox` dentro de la misma transacción. `PaymentOutboxPublisher` consulta hasta 20 pendientes aproximadamente cada 5 segundos y los marca como `enviado = true` después de una confirmación de Kafka. Los registros no confirmados permanecen pendientes.

**Deduplicación:** Notification registra los `eventId` consumidos en `processed_payment_events`. La implementación actual se probó con reenvíos secuenciales, pero no garantiza todavía el comportamiento correcto con varios consumidores simultáneos. La entrega del productor es **al menos una vez**.

## Pruebas manuales de integración

1. Crear pago mediante POST, verificar `PENDIENTE`.
2. Actualizar a `APROBADO`, consultar `GET /notifications` y encontrar nueva notificación `PAGO_APROBADO`.
3. Repetir la misma actualización y comprobar que no se genera otro evento desde Payment.
4. Leer los mensajes del topic con:

```powershell
docker exec -it techmarket-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic payment-events --from-beginning
```

5. Reenviar un JSON con **el mismo `eventId`** previamente procesado. Verificar en Notification el log `Evento duplicado ignorado`.
6. Enviar un evento intencionalmente inválido (por ejemplo, `PAYMENT_APPROVED` con `estado` `INVALIDO`); verificar que llega al DLT:

```powershell
docker exec -it techmarket-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic payment-events-dlt --from-beginning
```

7. Detener Kafka con `docker stop techmarket-kafka`, crear y aprobar otro pago, iniciar Kafka con `docker start techmarket-kafka` y comprobar en los logs `Outbox ... enviado a Kafka` y la posterior notificación.

En pruebas del DLT, ver trazas `ListenerExecutionFailedException` por un evento inválido es esperado si posteriormente se recupera enviándolo a `payment-events-dlt`.

## Colección de Postman

La colección `TechMarket_Postman_Collection.json` reúne las peticiones REST de Payment y Notification. Importarla mediante **Postman → Import**, iniciar los servicios y ejecutar las peticiones de creación antes de las dependientes de un ID. Los eventos internos de Kafka y la recuperación Outbox también requieren verificar logs o topics desde PowerShell.

## Configuración de Kafka para el equipo

El archivo **compartido** `kafka/docker-compose.yaml` debe usar un volumen administrado por Compose:

```yaml
    volumes:
      - kafka_data:/var/lib/kafka/data

volumes:
  kafka_data:
```

**No compartir** un bloque con `external: true` y un nombre hexadecimal de volumen perteneciente a otra computadora. El Compose portable usa `localhost:9092`, apropiado si Java se ejecuta en la misma PC que Docker; no es la configuración adecuada si los microservicios también corren en contenedores o en equipos diferentes. Los topics `payment-events` y `payment-events-dlt` se crean con los comandos de instalación indicados anteriormente.

## Consideraciones y trabajo pendiente

- La configuración actual de Kafka usa un solo broker y una partición: **solo desarrollo local**. El ZIP anterior contenía un Compose sin volumen declarado; el Compose portable entregado por separado ahora **sí** declara `kafka_data`. No ejecutar `docker compose down -v` si se desean conservar datos. Cambiar el volumen de un broker existente sin migrarlo no conserva automáticamente su contenido.
- El Outbox no tiene coordinación para varias instancias de Payment Service; se requieren bloqueos/reclamación de registros o un relay especializado para evitar envíos simultáneos.
- La deduplicación utiliza una consulta `existsById` previa al insert; el identificador es único, pero el control concurrente requiere tratamiento de conflictos o reserva atómica.
- Faltan pruebas automatizadas sustantivas, métricas, observabilidad, autenticación/autorización, migraciones de esquema (en lugar de `ddl-auto=update`) y entrega real de notificaciones.
- El ZIP revisado todavía contiene `notification.service.url` en `payment-service/application.properties`; el usuario indicó que lo retiraría de su copia local. Confirmar antes del commit.
- El ZIP revisado contiene dos clases `NotificationServiceApplicationTests` en paquetes diferentes; el usuario indicó que limpiaría la duplicada. Confirmar antes del commit. El docente pidió pruebas manuales con Postman; las automatizadas no forman parte del alcance requerido.
- El `compose.yaml` de Notification expone PostgreSQL en todas las interfaces; para desarrollo local puede limitarse a `127.0.0.1:5438:5432`, como Payment.
- La conexión futura con Auth/User/Order/Product y la existencia real de `userId`/`orderId` quedan fuera de esta entrega.

## Alcance de la entrega

Esta versión muestra dos microservicios independientes con bases de datos propias, comunicación asíncrona por Kafka, reintentos con DLT, deduplicación básica y recuperación de publicaciones mediante Transactional Outbox. Las pruebas manuales se realizaron localmente; este README no constituye una certificación de despliegue en producción.

## Checklist para GitHub

- Verificar que no se suban `.idea/`, `target/`, `.env`, secretos reales, ni un `docker-compose.local.yaml`.
- Conservar `pom.xml`, `src/`, `compose.yaml` de PostgreSQL, README y el Compose **portátil** de Kafka.
- Comprobar con `git status` y `git diff --cached --name-only` antes de confirmar cambios.
- Comprobar desde una clonación nueva que los servicios y topics arrancan con las instrucciones de este README.
- Los comandos aquí asumen que Payment y Notification se ejecutan directamente desde IntelliJ en la PC, no dentro de contenedores.
