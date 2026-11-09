
const cartContainer = document.querySelector("#cart-container");

const CART_PREFIX = "techmarket_cart_";

let currentUserId = null;
let CART_KEY = null;
let cargaEnCurso = false;

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
        console.error("Error al interpretar el token:", error);
        return null;
    }
}

async function inicializarUsuario() {
    const session = await API.get("/auth/me");

    const userId = session.userId ?? session.id ?? obtenerIdDesdeToken();

    if (userId === null || userId === undefined) {
        throw new Error("No se pudo identificar al usuario autenticado.");
    }

    currentUserId = String(userId);
    CART_KEY = `${CART_PREFIX}${currentUserId}`;
}

// ========================================
// 2. UTILIDADES DEL CARRITO
// ========================================

function obtenerCarrito() {
    if (!CART_KEY) {
        throw new Error("El carrito todavía no está inicializado.");
    }

    try {
        const cart = JSON.parse(
            localStorage.getItem(CART_KEY) || "[]"
        );

        return Array.isArray(cart) ? cart : [];

    } catch (error) {
        console.error("Error al leer el carrito:", error);
        return [];
    }
}

function guardarCarrito(cart) {
    if (!CART_KEY) {
        throw new Error("No se puede guardar un carrito sin usuario.");
    }

    localStorage.setItem(CART_KEY, JSON.stringify(cart));

    window.dispatchEvent(new Event("cart-updated"));
}

function formatearPrecio(precio) {
    return new Intl.NumberFormat("es-MX", {
        style: "currency",
        currency: "MXN"
    }).format(Number(precio) || 0);
}

function crearBoton(texto, clase, accion) {
    const button = document.createElement("button");

    button.type = "button";
    button.textContent = texto;
    button.className = clase;

    button.addEventListener("click", accion);

    return button;
}

// ========================================
// 3. CARGAR CARRITO
// ========================================

async function cargarCarrito() {
    if (!cartContainer || cargaEnCurso) return;

    cargaEnCurso = true;

    try {
        if (!currentUserId) {
            await inicializarUsuario();
        }

        const cart = obtenerCarrito();

        if (cart.length === 0) {
            mostrarCarritoVacio();
            return;
        }

        cartContainer.innerHTML = "<p>Cargando tu carrito...</p>";

        // Consultar los productos actuales.
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

        // Comprobar que las cantidades sean válidas.
        const carritoValido = productos.map(producto => ({
            productId: producto.id,
            quantity: Number.isInteger(producto.quantity) &&
            producto.quantity > 0
                ? producto.quantity
                : 1
        }));

        const huboCambios = carritoValido.some((item, index) =>
            Number(item.quantity) !== Number(cart[index].quantity) ||
            Number(item.productId) !== Number(cart[index].productId)
        );

        if (huboCambios) {
            guardarCarrito(carritoValido);
        }

        renderizarCarrito(productos);

    } catch (error) {
        console.error("Error al cargar el carrito:", error);

        cartContainer.innerHTML = `
            <div class="cart-empty-state">
                <h2>No pudimos cargar tu carrito</h2>
                <p>
                    Comprueba tu conexión e inténtalo nuevamente.
                    Tus productos siguen guardados.
                </p>
                <button
                    type="button"
                    class="product-button"
                    id="retry-cart-button"
                >
                    Reintentar
                </button>
            </div>
        `;

        const retryButton = document.querySelector("#retry-cart-button");

        if (retryButton) {
            retryButton.addEventListener("click", () => {
                cargarCarrito();
            });
        }

    } finally {
        cargaEnCurso = false;
    }
}

// ========================================
// 4. MOSTRAR CARRITO VACÍO
// ========================================

function mostrarCarritoVacio() {
    cartContainer.innerHTML = `
        <div class="cart-empty-state">
            <h2>Tu carrito está vacío</h2>
            <p>
                Explora nuestros productos y encuentra
                lo que necesitas.
            </p>
            <a href="index.html" class="product-button">
                Explorar productos
            </a>
        </div>
    `;
}

// ========================================
// 5. RENDERIZAR PRODUCTOS
// ========================================

function renderizarCarrito(productos) {
    cartContainer.innerHTML = "";

    const itemsContainer = document.createElement("div");
    itemsContainer.className = "cart-items";

    let total = 0;

    productos.forEach(producto => {
        const precio = Number(producto.precio) || 0;
        const stock = Number(producto.stock) || 0;
        const cantidad = producto.quantity;
        const subtotal = precio * cantidad;

        total += subtotal;

        const itemElement = document.createElement("article");
        itemElement.className = "cart-item";

        const information = document.createElement("div");
        information.className = "cart-item-information";

        const name = document.createElement("h3");
        name.textContent = producto.nombre;

        const price = document.createElement("p");
        price.textContent = `Precio: ${formatearPrecio(precio)}`;

        const subtotalElement = document.createElement("p");
        subtotalElement.textContent =
            `Subtotal: ${formatearPrecio(subtotal)}`;

        const stockInformation = document.createElement("p");
        stockInformation.textContent =
            `Existencias disponibles: ${stock}`;

        information.append(
            name,
            price,
            subtotalElement,
            stockInformation
        );

        const controls = document.createElement("div");
        controls.className = "cart-controls";

        const decreaseButton = crearBoton(
            "−",
            "cart-quantity-button",
            () => modificarCantidad(producto.id, -1)
        );

        decreaseButton.disabled = cantidad <= 1;

        const quantity = document.createElement("span");
        quantity.className = "cart-quantity";
        quantity.textContent = cantidad;

        const increaseButton = crearBoton(
            "+",
            "cart-quantity-button",
            () => modificarCantidad(producto.id, 1)
        );

        increaseButton.disabled = cantidad >= stock || stock <= 0;

        const removeButton = crearBoton(
            "Eliminar",
            "cart-remove-button",
            () => eliminarProducto(producto.id)
        );

        controls.append(
            decreaseButton,
            quantity,
            increaseButton,
            removeButton
        );

        itemElement.append(information, controls);
        itemsContainer.appendChild(itemElement);
    });

    // ========================================
    // RESUMEN DE COMPRA
    // ========================================

    const summary = document.createElement("section");
    summary.className = "cart-summary";

    const totalElement = document.createElement("h2");
    totalElement.textContent = `Total: ${formatearPrecio(total)}`;

    const actions = document.createElement("div");
    actions.className = "cart-summary-actions";

    const clearButton = crearBoton(
        "Vaciar carrito",
        "cart-clear-button",
        vaciarCarrito
    );

    const checkoutButton = crearBoton(
        "Continuar con la compra",
        "product-button",
        () => {
            window.location.href = "checkout.html";
        }
    );

    const stockInvalido = productos.some(producto =>
        !Number.isInteger(producto.quantity) ||
        producto.quantity <= 0 ||
        producto.quantity > Number(producto.stock) ||
        Number(producto.stock) <= 0
    );

    checkoutButton.disabled = stockInvalido;

    if (stockInvalido) {
        const warning = document.createElement("p");

        warning.className = "cart-stock-warning";
        warning.textContent =
            "Hay productos sin existencias suficientes. Ajusta las cantidades para continuar.";

        summary.appendChild(warning);
    }

    actions.append(clearButton, checkoutButton);
    summary.append(totalElement, actions);

    cartContainer.append(itemsContainer, summary);
}

// ========================================
// 6. MODIFICAR CANTIDADES
// ========================================

async function modificarCantidad(productId, cambio) {
    try {
        const cart = obtenerCarrito();

        const item = cart.find(
            item => Number(item.productId) === Number(productId)
        );

        if (!item) return;

        const nuevaCantidad = Number(item.quantity) + cambio;

        if (nuevaCantidad <= 0) {
            eliminarProducto(productId);
            return;
        }

        if (cambio > 0) {
            const producto = await API.get(
                `/products/${encodeURIComponent(productId)}`
            );

            if (nuevaCantidad > Number(producto.stock)) {
                alert("No hay más unidades disponibles.");
                return;
            }
        }

        item.quantity = nuevaCantidad;

        guardarCarrito(cart);
        await cargarCarrito();

    } catch (error) {
        console.error("Error al modificar la cantidad:", error);
        alert("No fue posible actualizar la cantidad.");
    }
}

// ========================================
// 7. ELIMINAR PRODUCTO
// ========================================

function eliminarProducto(productId) {
    const cart = obtenerCarrito().filter(
        item => Number(item.productId) !== Number(productId)
    );

    guardarCarrito(cart);
    cargarCarrito();
}

// ========================================
// 8. VACIAR CARRITO
// ========================================

function vaciarCarrito() {
    const confirmar = window.confirm(
        "¿Estás seguro de que deseas eliminar todos los productos del carrito?"
    );

    if (!confirmar) return;

    guardarCarrito([]);
    cargarCarrito();
}

// ========================================
// 9. INICIAR
// ========================================

cargarCarrito();
