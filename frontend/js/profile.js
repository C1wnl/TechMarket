
document.addEventListener("DOMContentLoaded", async () => {

    // ========================================
    // 1. UTILIDADES
    // ========================================

    const formatPrice = (amount) => {
        return new Intl.NumberFormat("es-MX", {
            style: "currency",
            currency: "MXN"
        }).format(Number(amount) || 0);
    };

    const escapeHTML = (value) => {
        return String(value ?? "").replace(/[&<>"']/g, (character) => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#039;"
        })[character]);
    };

    const showText = (selector, value) => {
        const element = document.querySelector(selector);

        if (element) {
            element.textContent = value ?? "No disponible";
        }
    };

    const formatDate = (date) => {
        if (!date) return "Fecha no disponible";

        const parsedDate = new Date(date);

        if (Number.isNaN(parsedDate.getTime())) {
            return "Fecha no disponible";
        }

        return parsedDate.toLocaleString("es-MX", {
            dateStyle: "medium",
            timeStyle: "short"
        });
    };

    const formatStatus = (status) => {
        const statuses = {
            "CONFIRMADO": "Confirmado",
            "PENDIENTE_PAGO": "Pendiente de pago",
            "PENDIENTE": "Pendiente",
            "CANCELADO": "Cancelado",
            "RECHAZADO": "Rechazado",
            "EN_PROCESO": "En proceso"
        };

        return statuses[status] || status || "Sin estado";
    };

    // ========================================
    // 2. MOSTRAR HISTORIAL DE PEDIDOS
    // ========================================

    const renderOrders = (orders) => {
        const container = document.querySelector(
            "#profile-orders-container"
        );

        if (!container) return;

        if (!Array.isArray(orders) || orders.length === 0) {
            container.innerHTML = `
                <div class="profile-empty-state">
                    <div class="profile-empty-icon">□</div>

                    <h3>Aún no tienes pedidos</h3>

                    <p>
                        Cuando realices una compra, podrás consultar
                        aquí su información y estado.
                    </p>

                    <a href="index.html" class="profile-store-link">
                        Explorar productos
                    </a>
                </div>
            `;

            return;
        }

        container.innerHTML = orders.map((order) => {

            const items = Array.isArray(order.items)
                ? order.items
                : [];

            const itemsHTML = items.map((item) => {

                const name = escapeHTML(
                    item.nombre ??
                    item.nombreProducto ??
                    "Producto"
                );

                const quantity = Number(item.cantidad) || 0;

                const unitPrice = formatPrice(
                    item.precioUnitario
                );

                const subtotal = formatPrice(
                    item.subtotal
                );

                return `
                    <li class="profile-order-item">

                        <div>
                            <strong>${name}</strong>

                            <span>
                                ${unitPrice} por unidad
                            </span>
                        </div>

                        <div>
                            <span>
                                x${quantity}
                            </span>

                            <strong>
                                ${subtotal}
                            </strong>
                        </div>

                    </li>
                `;
            }).join("");

            const orderNumber = escapeHTML(
                order.numeroPedido ?? "No disponible"
            );

            const status = escapeHTML(
                formatStatus(order.estado)
            );

            const date = escapeHTML(
                formatDate(order.fechaCreacion)
            );

            const total = formatPrice(order.total);

            return `
                <article class="profile-order-card">

                    <div class="profile-panel-heading">

                        <div>
                            <p class="profile-section-label">
                                PEDIDO
                            </p>

                            <h3>
                                #${orderNumber}
                            </h3>
                        </div>

                        <span class="profile-role">
                            ${status}
                        </span>

                    </div>

                    <p class="profile-order-date">
                        <strong>Fecha:</strong>
                        ${date}
                    </p>

                    <div class="profile-order-products">

                        <strong>Productos:</strong>

                        <ul>
                            ${itemsHTML || `
                                <li>Sin artículos registrados</li>
                            `}
                        </ul>

                    </div>

                    <div class="profile-order-total">
                        <strong>Total del pedido:</strong>

                        <span>
                            ${total}
                        </span>
                    </div>

                </article>
            `;
        }).join("");
    };

    // ========================================
    // 3. CARGAR INFORMACIÓN PERSONAL
    // ========================================

    const loadProfile = async () => {

        const user = await API.get("/users/me");

        showText("#profile-user-name", user.nombre);
        showText("#profile-user-email", user.email);

        showText("#profile-display-name", user.nombre);
        showText("#profile-email", user.email);

        const avatar = document.querySelector("#profile-avatar");

        if (avatar) {
            avatar.textContent =
                user.nombre?.trim()?.charAt(0)?.toUpperCase() || "U";
        }
    };

    // ========================================
    // 4. CARGAR HISTORIAL
    // ========================================

    const loadOrders = async () => {

        const container = document.querySelector(
            "#profile-orders-container"
        );

        try {
            const orders = await API.get("/orders");

            renderOrders(orders);

        } catch (error) {

            console.error(
                "Error al cargar los pedidos:",
                error
            );

            if (container) {
                container.textContent =
                    "No fue posible cargar tus pedidos. Inténtalo nuevamente.";
            }
        }
    };

    // ========================================
    // 5. VERIFICAR SESIÓN Y CARGAR PERFIL
    // ========================================

    try {

        const session = await API.get("/auth/me");

        const role = session.rol ?? "USER";

        const friendlyRole = role === "ADMIN"
            ? "Administrador"
            : "Usuario";

        showText("#profile-user-role", friendlyRole);
        showText("#profile-role", friendlyRole);

        await loadProfile();

        await loadOrders();

        console.log("Perfil cargado correctamente.");

    } catch (error) {

        console.error(
            "No se pudo cargar el perfil:",
            error
        );

        showText(
            "#profile-display-name",
            "No se pudo cargar el perfil"
        );

        showText(
            "#profile-email",
            "Verifica tu sesión e inténtalo nuevamente"
        );

        showText("#profile-user-name", "No disponible");
        showText("#profile-user-email", "No disponible");

        const ordersContainer = document.querySelector(
            "#profile-orders-container"
        );

        if (ordersContainer) {
            ordersContainer.textContent =
                "No fue posible cargar la información de tu cuenta.";
        }
    }

});
