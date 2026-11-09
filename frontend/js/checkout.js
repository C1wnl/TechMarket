
const checkoutContainer = document.querySelector("#checkout-container");

const CART_PREFIX = "techmarket_cart_";
const IDEMPOTENCY_PREFIX = "techmarket_checkout_idempotency_key_";
const PENDING_ORDER_PREFIX = "techmarket_pending_order_";

const POLLING_INTERVAL = 2000;
const MAX_POLLING_ATTEMPTS = 30;

let currentUserId = null;
let CART_KEY = null;
let IDEMPOTENCY_KEY = null;
let PENDING_ORDER_KEY = null;

// ========================================
// 1. IDENTIFICAR USUARIO
// ========================================

function obtenerIdDesdeToken() {
    if (typeof API.getToken !== "function") return null;

    const token = API.getToken();

    if (!token) return null;

    try {
        const payloadBase64 = token.split(".")[1]
            .replace(/-/g, "+")
            .replace(/_/g, "/");

        const payload = JSON.parse(
            atob(payloadBase64.padEnd(
                Math.ceil(payloadBase64.length / 4) * 4,
                "="
            ))
        );

        return payload.sub ?? null;
    } catch (error) {
        console.error("No se pudo interpretar el token:", error);
        return null;
    }
}

async function inicializarUsuario() {
    const session = await API.get("/auth/me");

    const userId = session.userId ?? session.id ?? obtenerIdDesdeToken();

    if (userId === null || userId === undefined) {
        throw new Error("No se pudo identificar al usuario.");
    }

    currentUserId = String(userId);

    CART_KEY = `${CART_PREFIX}${currentUserId}`;
    IDEMPOTENCY_KEY = `${IDEMPOTENCY_PREFIX}${currentUserId}`;
    PENDING_ORDER_KEY = `${PENDING_ORDER_PREFIX}${currentUserId}`;
}

// ========================================
// 2. UTILIDADES
// ========================================

function obtenerCarrito() {
    try {
        const cart = JSON.parse(
            localStorage.getItem(CART_KEY) || "[]"
        );

        return Array.isArray(cart) ? cart : [];

    } catch (error) {
        console.error("Error al recuperar el carrito:", error);
        return [];
    }
}

function guardarCarrito(cart) {
    localStorage.setItem(CART_KEY, JSON.stringify(cart));

    window.dispatchEvent(new Event("cart-updated"));
}

function obtenerIdempotencyKey() {
    let key = localStorage.getItem(IDEMPOTENCY_KEY);

    if (!key) {
        key = crypto.randomUUID();
        localStorage.setItem(IDEMPOTENCY_KEY, key);
    }

    return key;
}

function formatearPrecio(precio) {
    return new Intl.NumberFormat("es-MX", {
        style: "currency",
        currency: "MXN"
    }).format(Number(precio) || 0);
}

function esperar(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function traducirEstado(estado) {
    const estados = {
        PENDIENTE_PAGO: "Pendiente de pago",
        CONFIRMADO: "Confirmado",
        CANCELADO: "Cancelado",
        PENDIENTE: "Pendiente"
    };

    return estados[estado] || estado || "Desconocido";
}

// ========================================
// 3. MOSTRAR RESULTADOS
// ========================================

function mostrarResultado(titulo, mensaje, pedido, exitoso) {
    checkoutContainer.innerHTML = "";

    const result = document.createElement("section");
    result.className = "checkout-summary";

    const heading = document.createElement("h2");
    heading.textContent = titulo;

    const orderNumber = document.createElement("p");
    orderNumber.textContent =
        `Número de pedido: ${pedido.numeroPedido}`;

    const status = document.createElement("p");
    status.textContent =
        `Estado: ${traducirEstado(pedido.estado)}`;

    const information = document.createElement("p");
    information.textContent = mensaje;

    const profileLink = document.createElement("a");
    profileLink.href = "profile.html";
    profileLink.className = "product-button";
    profileLink.textContent = "Consultar mis pedidos";

    result.append(
        heading,
        orderNumber,
        status,
        information,
        profileLink
    );

    if (!exitoso) {
        const cartLink = document.createElement("a");

        cartLink.href = "cart.html";
        cartLink.className = "product-button";
        cartLink.textContent = "Volver al carrito";

        result.appendChild(cartLink);
    }

    checkoutContainer.appendChild(result);
}

// ========================================
// 4. MOSTRAR PROCESAMIENTO
// ========================================

function mostrarMensajeProcesando(pedido) {
    checkoutContainer.innerHTML = "";

    const summary = document.createElement("section");
    summary.className = "checkout-summary";

    const title = document.createElement("h2");
    title.textContent = "Procesando tu compra...";

    const number = document.createElement("p");
    number.textContent =
        `Número de pedido: ${pedido.numeroPedido}`;

    const status = document.createElement("p");
    status.textContent =
        `Estado actual: ${traducirEstado(pedido.estado)}`;

    const information = document.createElement("p");
    information.textContent =
        "Estamos esperando la respuesta del servicio de pagos. No crees otro pedido mientras se procesa.";

    summary.append(
        title,
        number,
        status,
        information
    );

    checkoutContainer.appendChild(summary);
}

// ========================================
// 5. CONSULTAR ESTADO DE LA SAGA
// ========================================

async function consultarEstadoPedido(orderId) {
    for (
        let attempt = 0;
        attempt < MAX_POLLING_ATTEMPTS;
        attempt++
    ) {
        await esperar(POLLING_INTERVAL);

        const pedido = await API.get(
            `/orders/${encodeURIComponent(orderId)}`
        );

        console.log(
            `Estado del pedido ${orderId}:`,
            pedido.estado
        );

        if (pedido.estado === "CONFIRMADO") {
            // Limpiar únicamente el carrito de este usuario.
            guardarCarrito([]);

            localStorage.removeItem(IDEMPOTENCY_KEY);
            localStorage.removeItem(PENDING_ORDER_KEY);

            mostrarResultado(
                "¡Compra confirmada!",
                "Tu pago fue aprobado y tu pedido ha sido confirmado correctamente.",
                pedido,
                true
            );

            return;
        }

        if (pedido.estado === "CANCELADO") {
            // Conservar el carrito para que pueda volver a intentarlo.
            localStorage.removeItem(PENDING_ORDER_KEY);
            localStorage.removeItem(IDEMPOTENCY_KEY);

            mostrarResultado(
                "No fue posible completar tu compra",
                "El pago fue rechazado y el pedido ha sido cancelado. Tus productos siguen en el carrito para que puedas intentarlo nuevamente.",
                pedido,
                false
            );

            return;
        }

        mostrarMensajeProcesando(pedido);
    }

    const message = document.createElement("p");

    message.textContent =
        "Tu pedido continúa procesándose. No necesitas crear otro pedido. Puedes consultar su estado desde tu perfil.";

    const profileLink = document.createElement("a");

    profileLink.href = "profile.html";
    profileLink.className = "product-button";
    profileLink.textContent = "Consultar mis pedidos";

    checkoutContainer.innerHTML = "";

    checkoutContainer.append(message, profileLink);
}

// ========================================
// 6. CREAR PEDIDO
// ========================================

async function crearPedido(productos) {
    const confirmButton = document.querySelector(
        "#confirm-order-button"
    );

    if (confirmButton) {
        confirmButton.disabled = true;
        confirmButton.textContent = "Creando pedido...";
    }

    try {
        const pedido = await API.post(
            "/orders",
            {
                items: productos.map(producto => ({
                    productId: producto.id,
                    cantidad: producto.quantity
                }))
            },
            {
                "Idempotency-Key": obtenerIdempotencyKey()
            }
        );

        localStorage.setItem(
            PENDING_ORDER_KEY,
            String(pedido.numeroPedido)
        );

        mostrarMensajeProcesando(pedido);

        await consultarEstadoPedido(pedido.numeroPedido);

    } catch (error) {
        console.error("Error al crear el pedido:", error);

        const message = document.querySelector(
            "#checkout-error-message"
        );

        if (message) {
            message.textContent =
                `No fue posible crear el pedido: ${error.message}`;
        }

        if (confirmButton) {
            confirmButton.disabled = false;
            confirmButton.textContent = "Reintentar pedido";
        }
    }
}

// ========================================
// 7. CARGAR CHECKOUT
// ========================================

async function cargarCheckout() {
    if (!checkoutContainer) return;

    try {
        await inicializarUsuario();

        // Recuperar pedido pendiente de esta cuenta.
        const pendingOrder = localStorage.getItem(
            PENDING_ORDER_KEY
        );

        if (pendingOrder) {
            mostrarMensajeProcesando({
                numeroPedido: pendingOrder,
                estado: "PENDIENTE_PAGO"
            });

            await consultarEstadoPedido(pendingOrder);
            return;
        }

        const cart = obtenerCarrito();

        if (cart.length === 0) {
            checkoutContainer.innerHTML = `
                <p>No tienes productos en tu carrito.</p>

                <a href="index.html" class="product-button">
                    Volver a la tienda
                </a>
            `;

            return;
        }

        // Recuperar los datos actuales de Product Service.
        const productos = await Promise.all(
            cart.map(async item => {
                const producto = await API.get(
                    `/products/${encodeURIComponent(item.productId)}`
                );

                return {
                    ...producto,
                    quantity: Number(item.quantity)
                };
            })
        );

        checkoutContainer.innerHTML = "";

        const summary = document.createElement("section");
        summary.className = "checkout-summary";

        const title = document.createElement("h2");
        title.textContent = "Resumen de tu compra";

        const productList = document.createElement("div");
        productList.className = "checkout-products";

        let total = 0;

        productos.forEach(producto => {
            const subtotal =
                Number(producto.precio) * producto.quantity;

            total += subtotal;

            const item = document.createElement("article");
            item.className = "checkout-item";

            const name = document.createElement("h3");
            name.textContent = producto.nombre;

            const quantity = document.createElement("p");
            quantity.textContent =
                `Cantidad: ${producto.quantity}`;

            const price = document.createElement("p");
            price.textContent =
                `Precio unitario: ${formatearPrecio(producto.precio)}`;

            const subtotalElement = document.createElement("p");
            subtotalElement.textContent =
                `Subtotal: ${formatearPrecio(subtotal)}`;

            item.append(
                name,
                quantity,
                price,
                subtotalElement
            );

            productList.appendChild(item);
        });

        const totalElement = document.createElement("h2");
        totalElement.textContent =
            `Total: ${formatearPrecio(total)}`;

        const stockInsuficiente = productos.some(producto =>
            !Number.isInteger(producto.quantity) ||
            producto.quantity <= 0 ||
            producto.quantity > Number(producto.stock) ||
            Number(producto.stock) <= 0
        );

        const stockMessage = document.createElement("p");

        if (stockInsuficiente) {
            stockMessage.textContent =
                "Algunos productos no tienen suficiente stock. Regresa al carrito para ajustar las cantidades.";
        }

        const confirmButton = document.createElement("button");

        confirmButton.id = "confirm-order-button";
        confirmButton.className = "product-button";
        confirmButton.textContent = "Confirmar pedido";
        confirmButton.disabled = stockInsuficiente;

        confirmButton.addEventListener("click", () => {
            crearPedido(productos);
        });

        const errorMessage = document.createElement("p");
        errorMessage.id = "checkout-error-message";

        summary.append(
            title,
            productList,
            totalElement,
            stockMessage,
            confirmButton,
            errorMessage
        );

        checkoutContainer.appendChild(summary);

    } catch (error) {
        console.error("Error al preparar el checkout:", error);

        checkoutContainer.innerHTML = `
            <p>No fue posible preparar tu compra.</p>

            <a href="cart.html" class="product-button">
                Volver al carrito
            </a>
        `;
    }
}

// ========================================
// 8. INICIAR
// ========================================

cargarCheckout();
