window.TM = (() => {
    const config = window.TECHMARKET_CONFIG;
    const isLocal = config.mode === "local";
    function baseUrl(path) {
        if (!isLocal) return config.apiBaseUrl.replace(/\/$/, "");
        if (/^\/products(?:\/|\?|$)/.test(path)) return config.productApiUrl.replace(/\/$/, "");
        if (/^\/orders(?:\/|\?|$)/.test(path)) return config.orderApiUrl.replace(/\/$/, "");
        throw new Error("Esta operación requiere la integración con el equipo. En modo local solo se usan Productos y Pedidos.");
    }
    const money = value => new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" }).format(value);
    const token = () => isLocal ? null : localStorage.getItem("token");
    let sessionPromise;

    async function request(path, options = {}) {
        const headers = new Headers(options.headers);
        if (options.body) headers.set("Content-Type", "application/json");
        if (options.auth !== false && token()) headers.set("Authorization", `Bearer ${token()}`);
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), options.method === "POST" ? 60000 : 15000);
        try {
            const response = await fetch(baseUrl(path) + path, { ...options, headers, signal: controller.signal });
            const text = await response.text();
            let data;
            try { data = text ? JSON.parse(text) : null; }
            catch { data = null; }
            if (!response.ok) {
                const message = data?.error || data?.message || (data && Object.values(data).filter(v => typeof v === "string").join(". "));
                const error = new Error(message || `No fue posible completar la solicitud (${response.status}).`);
                error.status = response.status;
                if (response.status === 401) {
                    localStorage.removeItem("token");
                    sessionPromise = null;
                    window.dispatchEvent(new Event("tm-session-ended"));
                }
                throw error;
            }
            return data;
        } catch (error) {
            if (error.name === "AbortError") {
                throw new Error(options.method === "POST"
                    ? "La respuesta tardó demasiado. Revisa si se guardó antes de volver a enviar."
                    : "El servicio tardó demasiado en responder. Intenta actualizar.");
            }
            if (error instanceof TypeError) throw new Error("No se pudo conectar con TechMarket. Comprueba que los servicios estén iniciados.");
            throw error;
        } finally { clearTimeout(timer); }
    }

    function session() {
        if (isLocal) return Promise.resolve({ userId: 1, rol: "ADMIN", demo: true });
        if (!token()) return Promise.resolve(null);
        if (!sessionPromise) sessionPromise = request("/auth/me").catch(error => {
            sessionPromise = null;
            if (error.status === 401) return null;
            throw error;
        });
        return sessionPromise;
    }

    async function requireSession(admin = false) {
        const user = await session();
        if (!user) { window.location.replace("login.html"); return null; }
        if (admin && user.rol !== "ADMIN") { window.location.replace("index.html"); return null; }
        return user;
    }

    function message(element, text, error = false) {
        if (!element) return;
        element.textContent = text;
        element.classList.toggle("is-error", error);
    }

    function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }

    function renderOrders(container, orders, showUser = false) {
        container.replaceChildren();
        if (!orders.length) {
            container.append(element("p", "empty-state", "Todavía no hay pedidos. Explora el catálogo para crear el primero."));
            return;
        }
        orders.forEach(order => {
            const card = element("article", "order-card");
            const heading = element("div", "order-heading");
            heading.append(element("h3", "", `Pedido #${order.id}`), element("span", "status-pill", order.estado));
            card.append(heading);
            if (showUser) card.append(element("p", "muted", `Usuario #${order.userId}`));
            if (order.items?.length) {
                const list = element("ul", "order-lines");
                order.items.forEach(item => list.append(element("li", "", `${item.nombre} · ${item.cantidad} × ${money(item.precioUnitario)} = ${money(item.subtotal)}`)));
                card.append(list);
            } else card.append(element("p", "muted", "Pedido de la versión anterior, sin detalle de productos."));
            card.append(element("p", "order-total", `Total: ${money(order.total)} MXN`));
            container.append(card);
        });
    }

    return { isLocal, request, session, requireSession, token, money, message, element, renderOrders };
})();
