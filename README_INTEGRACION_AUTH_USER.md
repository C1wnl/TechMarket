# TechMarket — Guía de integración para Auth/User

## 1. Objetivo

Esta rama `integration/kafka` reúne los avances de Order, Product, Payment y Notification para preparar su integración con Auth, User, API Gateway y Eureka.

La Saga de compra ya fue probada localmente utilizando Docker Desktop, PostgreSQL, Apache Kafka y Postman.

**Importante:** la integración de autenticación JWT y API Gateway todavía está pendiente.

## 2. Arquitectura actual

```text
Frontend
   |
   v
Order Service (8084)
   |
   +---- HTTP ----> Product Service (8083)
   |                  Reservas de inventario
   |
   +---- REST ----> Payment Service (8085)
                         |
                         v
                    Apache Kafka
                    payment-events
                         |
                  +------+------+
                  |             |
                  v             v
             Order Service   Notification Service
             Confirma o      Registra
             cancela         notificaciones
```

Cada microservicio utiliza su propia base de datos PostgreSQL.

## 3. Flujo de compra validado

1. El cliente crea un pedido en `POST /orders`.
2. Order consulta los productos y reserva existencias mediante HTTP.
3. Order solicita crear el pago a Payment por REST.
4. El pedido queda en `PENDIENTE_PAGO`.
5. Payment aprueba o rechaza el pago.
6. Payment publica el resultado mediante Transactional Outbox y Kafka.
7. Order consume el resultado y confirma o libera las reservas.
8. Notification consume el evento de forma independiente.

### Pago aprobado

- Order: `CONFIRMADO`.
- Payment: `APROBADO`.
- Product: reserva `CONFIRMED`.
- Notification: `PAGO_APROBADO`.

### Pago rechazado

- Order: `CANCELADO`.
- Payment: `RECHAZADO`.
- Product: reserva `RELEASED`.
- Notification: `PAGO_RECHAZADO`.

## 4. Tecnologías

- Java 21.
- Spring Boot 4.1.1.
- Spring Security.
- Spring Data JPA.
- Spring Kafka.
- PostgreSQL 16.
- Apache Kafka 4.1.2.
- Docker Desktop.
- Postman.

## 5. Configuración local utilizada

| Servicio | Puerto host | Puerto interno |
|---|---|---|
| Order | 8094 | 8084 |
| Product | 8093 | 8083 |
| Payment | 8085 | 8085 |
| Notification | 8086 | 8086 |
| PostgreSQL Order | 5446 | 5432 |
| PostgreSQL Product | 5445 | 5432 |
| PostgreSQL Payment | 5437 | 5432 |
| PostgreSQL Notification | 5438 | 5432 |
| Kafka externo | 9092 | 9092 |
| Kafka interno | — | 29092 |

Estos nombres y puertos corresponden al laboratorio de integración. Pueden adaptarse cuando se incorpore la infraestructura común.

### Variables de entorno de Order

```properties
SPRING_PROFILES_ACTIVE=local,payment-kafka
DB_URL=jdbc:postgresql://order-db-test:5432/order-db
PRODUCT_SERVICE_URL=http://product-service-test:8083
PAYMENT_SERVICE_URL=http://payment-service-integracion:8085
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
ORDER_KAFKA_GROUP=order-service-group
PAYMENT_METHOD=TARJETA
```

También se requieren credenciales PostgreSQL y la clave interna de inventario. No se incluyen secretos en esta documentación.

## 6. Kafka

Topic principal: `payment-events`.

Topic de errores de Notification: `payment-events-dlt`.

Evento de aprobación:

```json
{
  "eventId": "uuid",
  "eventType": "PAYMENT_APPROVED",
  "paymentId": 3,
  "orderId": 3,
  "userId": 1,
  "monto": 700.00,
  "estado": "APROBADO",
  "fechaEvento": "2026-10-28T03:00:00Z"
}
```

El evento de rechazo utiliza `PAYMENT_REJECTED` y `RECHAZADO`.

Order y Notification consumen el mismo topic mediante grupos distintos.

## 7. Idempotencia

Order acepta la cabecera `Idempotency-Key` en `POST /orders`.

- Repetir una clave con los mismos productos y cantidades devuelve el pedido original.
- Reutilizar la clave con datos diferentes produce `409 Conflict`.
- Una misma compra no debe descontar inventario dos veces.

Payment utiliza una restricción única sobre `order_id` para impedir múltiples pagos distintos asociados al mismo pedido.

Order conserva un Inbox de eventos y mecanismos de recuperación para evitar aplicar dos veces el mismo resultado.

## 8. Qué necesita integrar Auth/User

### Autenticación JWT

Actualmente Order y Product utilizan un perfil `local` para realizar pruebas sin depender de Auth.

El siguiente paso consiste en sustituir la identidad simulada por la identidad autenticada procedente del JWT.

Verificar especialmente:

- Obtención de `userId` desde el token.
- Validación de roles y permisos.
- Protección de creación y consulta de pedidos.
- Protección de administración de productos.
- Acceso únicamente del propietario a sus pedidos, excepto usuarios administradores.

### API Gateway

Configurar rutas externas hacia Order, Product, Payment y Notification según los permisos correspondientes.

Los endpoints internos de reservas de Product no deben exponerse públicamente.

Los endpoints de simulación `/internal/demo/**` deben permanecer deshabilitados fuera de `local`.

### Eureka

Registrar los servicios en Eureka cuando se implemente la configuración conjunta, respetando los puertos y perfiles seleccionados.

La configuración de descubrimiento no debe sustituir las URLs HTTP internas hasta verificar su funcionamiento.

### Payment

El endpoint que modifica el resultado de un pago requiere protección antes de cualquier demostración con usuarios reales.

No debe permitirse que un usuario común apruebe o rechace arbitrariamente pagos.

## 9. Pruebas realizadas

Se validó en Docker Desktop y PostgreSQL:

- Creación de pedidos.
- Reservas de inventario.
- Creación automática de pagos.
- Pago aprobado mediante Kafka.
- Confirmación automática del pedido.
- Pago rechazado mediante Kafka.
- Cancelación y devolución del inventario.
- Notificaciones de aprobación y rechazo.
- Reintentos HTTP con clave de idempotencia.
- Conflictos por reutilizar la clave con cantidades diferentes.
- Prevención de pagos duplicados.

No se han validado todavía todos los escenarios de fallos prolongados, reinicios simultáneos o despliegues distribuidos.

## 10. Recomendaciones de integración

1. Trabajar sobre una copia de esta rama y mantener respaldos.
2. Incorporar Auth/User sin modificar la lógica de reservas y compensaciones.
3. Configurar el JWT y probar primero accesos autorizados y no autorizados.
4. Añadir Gateway y Eureka después de confirmar las conexiones directas.
5. Volver a probar una compra aprobada y una rechazada.
6. No publicar contraseñas, volúmenes Docker, archivos `.env` ni bases de datos.

## 11. Estado de la entrega

Los microservicios de Order, Product, Payment y Notification funcionan de forma independiente en el laboratorio y se comunican mediante HTTP y Kafka.

**Esta rama es una base de integración universitaria, no un despliegue de producción.** La integración global con Auth/User, Gateway y Eureka sigue pendiente.