
const productDetail = document.querySelector("#product-detail");

const CART_PREFIX = "techmarket_cart_";

async function obtenerUsuarioActual() {
    const session = await API.get("/auth/me");

    let userId = session.userId ?? session.id;

    // Si /auth/me no devuelve userId, obtenerlo del JWT.
    if (!userId && typeof API.getToken === "function") {
        const token = API.getToken();

        if (token) {
            try {
                const payload = JSON.parse(
                    atob(
                        token.split(".")[1]
                            .replace(/-/g, "+")
                            .replace(/_/g, "/")
                    )
                );

                userId = payload.sub;
            } catch (error) {
                console.error("No se pudo leer el identificador del usuario:", error);
            }
        }
    }

    if (userId === undefined || userId === null) {
        throw new Error("No se pudo identificar al usuario autenticado.");
    }

    return String(userId);
}

function obtenerClaveCarrito(userId) {
    return `${CART_PREFIX}${userId}`;
}

function obtenerCarrito(userId) {
    try {
        const cart = JSON.parse(
            localStorage.getItem(obtenerClaveCarrito(userId)) || "[]"
        );

        return Array.isArray(cart) ? cart : [];
    } catch (error) {
        console.error("Error al leer el carrito:", error);
        return [];
    }
}

function guardarCarrito(userId, cart) {
    localStorage.setItem(
        obtenerClaveCarrito(userId),
        JSON.stringify(cart)
    );

    window.dispatchEvent(new Event("cart-updated"));
}

async function cargarDetalleProducto() {
    if (!productDetail) return;

    const params = new URLSearchParams(window.location.search);
    const productId = params.get("id");

    if (!productId || !/^\d+$/.test(productId)) {
        productDetail.innerHTML = `
            <p>No se ha seleccionado un producto válido.</p>
            <a href="index.html">Volver a la tienda</a>
        `;
        return;
    }

    try {
        const producto = await API.get(
            `/products/${encodeURIComponent(productId)}`
        );

        productDetail.innerHTML = "";

        const container = document.createElement("article");
        container.className = "product-detail-card";

        const image = document.createElement("img");
        image.className = "product-detail-image";
        image.src = producto.imagenUrl || "assets/products/MacBookPro.jpg";
        image.alt = producto.nombre || "Producto TechMarket";
        image.loading = "lazy";

        image.onerror = () => {
            image.onerror = null;
            image.src = "assets/products/MacBookPro.jpg";
        };

        const info = document.createElement("div");
        info.className = "product-detail-info";

        const category = document.createElement("p");
        category.className = "product-category";
        category.textContent = "TECNOLOGÍA";

        const name = document.createElement("h1");
        name.textContent = producto.nombre;

        const description = document.createElement("p");
        description.className = "product-description";
        description.textContent =
            producto.descripcion || "Sin descripción disponible.";

        const price = document.createElement("p");
        price.className = "product-price";
        price.textContent = new Intl.NumberFormat("es-MX", {
            style: "currency",
            currency: "MXN"
        }).format(Number(producto.precio) || 0);

        const stock = document.createElement("p");
        stock.className = "product-stock";
        stock.textContent = Number(producto.stock) > 0
            ? `Disponibles: ${producto.stock}`
            : "Agotado";

        const addToCartButton = document.createElement("button");
        addToCartButton.type = "button";
        addToCartButton.className = "product-button";
        addToCartButton.textContent = "Añadir al carrito";
        addToCartButton.disabled = Number(producto.stock) <= 0;

        const cartMessage = document.createElement("p");
        cartMessage.className = "product-stock";

        addToCartButton.addEventListener("click", async () => {
            addToCartButton.disabled = true;
            cartMessage.textContent = "Actualizando carrito...";

            try {
                const userId = await obtenerUsuarioActual();
                const cart = obtenerCarrito(userId);

                const existingItem = cart.find(
                    item => Number(item.productId) === Number(producto.id)
                );

                if (existingItem) {
                    if (Number(existingItem.quantity) >= Number(producto.stock)) {
                        cartMessage.textContent =
                            "No hay más unidades disponibles.";
                        return;
                    }

                    existingItem.quantity =
                        Number(existingItem.quantity) + 1;
                } else {
                    cart.push({
                        productId: producto.id,
                        quantity: 1
                    });
                }

                guardarCarrito(userId, cart);

                cartMessage.textContent =
                    "¡Producto añadido a tu carrito!";

            } catch (error) {
                console.error("Error al actualizar el carrito:", error);

                cartMessage.textContent =
                    "Inicia sesión para utilizar tu carrito.";
            } finally {
                addToCartButton.disabled =
                    Number(producto.stock) <= 0;
            }
        });

        const backLink = document.createElement("a");
        backLink.className = "product-button";
        backLink.href = "index.html";
        backLink.textContent = "Volver a la tienda";

        info.append(
            category,
            name,
            description,
            price,
            stock,
            addToCartButton,
            cartMessage,
            backLink
        );

        container.append(image, info);
        productDetail.appendChild(container);

    } catch (error) {
        console.error("Error al cargar el producto:", error);

        productDetail.innerHTML = `
            <p>No fue posible cargar este producto.</p>
            <a href="index.html">Volver a la tienda</a>
        `;
    }
}

cargarDetalleProducto();
