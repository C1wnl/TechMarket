docker compose up -d payment-db# TechMarket - Payment Service

Microservicio de pagos para TechMarket.

## Tecnologías
- Java 21
- Spring Boot 4.1.1
- Maven
- Spring Web MVC
- Spring Data JPA
- PostgreSQL 16
- Jakarta Validation

## Puerto
`8085`

## Base de datos
`payment-db`

Por defecto se conecta a:
`jdbc:postgresql://localhost:5437/payment-db`

## Ejecutar PostgreSQL con Docker
Desde esta carpeta:

```bash
docker compose up -d payment-db
```

## Ejecutar la aplicación
En IntelliJ IDEA, ejecutar `PaymentServiceApplication`.

También se puede ejecutar con Maven Wrapper en Windows:

```bat
mvnw.cmd spring-boot:run
```

## Endpoints

### Crear pago
`POST /payments`

```json
{
  "orderId": 1,
  "userId": 1,
  "monto": 1499.90,
  "metodoPago": "TARJETA"
}
```

El pago se crea con estado `PENDIENTE`.

### Consultar pagos
`GET /payments`

### Consultar pago por ID
`GET /payments/{id}`

### Consultar pagos de un pedido
`GET /payments/order/{orderId}`

### Consultar pagos de un usuario
`GET /payments/user/{userId}`

### Actualizar estado
`PUT /payments/{id}/status`

```json
{
  "estado": "APROBADO"
}
```

Estados permitidos:
- PENDIENTE
- APROBADO
- RECHAZADO
- CANCELADO

## Nota de integración
Kafka/RabbitMQ, Eureka y API Gateway se integrarán en una etapa posterior para respetar la arquitectura global del equipo.
