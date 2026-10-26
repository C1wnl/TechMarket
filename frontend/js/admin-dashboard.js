(async () => {
    const accessMessage = document.querySelector("#admin-access-message");
    const productMessage = document.querySelector("#product-message");
    const ordersMessage = document.querySelector("#admin-orders-message");
    const form = document.querySelector("#product-form");
    async function loadProducts() {
        const button = document.querySelector("#admin-refresh-products");
        button.disabled = true;
        try {
            const products = await TM.request("/products");
            const body = document.querySelector("#admin-products");
            body.replaceChildren();
            products.forEach(product => {
                const row = TM.element("tr");
                [product.id, product.nombre, TM.money(product.precio), product.stock].forEach(value => row.append(TM.element("td", "", value)));
                body.append(row);
            });
            if (!products.length) TM.message(productMessage, "El catálogo está vacío. Agrega tu primer producto.");
        } catch (error) { TM.message(productMessage, error.message, true); }
        finally { button.disabled = false; }
    }
    async function loadOrders() {
        const button = document.querySelector("#admin-refresh-orders");
        button.disabled = true;
        TM.message(ordersMessage, "Cargando pedidos…");
        try {
            TM.renderOrders(document.querySelector("#admin-orders"), await TM.request("/orders"), true);
            TM.message(ordersMessage, "");
        } catch (error) { TM.message(ordersMessage, error.message, true); }
        finally { button.disabled = false; }
    }
    try {
        const user = await TM.requireSession(true);
        if (!user) return;
        document.querySelector("#admin-tools").hidden = false;
        document.querySelector("#admin-data").hidden = false;
        document.querySelector("#admin-name").textContent = user.demo ? "Demostración local" : `Administrador #${user.userId}`;
        if (user.demo) document.querySelector(".admin-role").textContent = "Modo local";
        TM.message(accessMessage, "");
        document.querySelector("#admin-refresh-products").addEventListener("click", async () => {
            TM.message(productMessage, "");
            await loadProducts();
        });
        document.querySelector("#admin-refresh-orders").addEventListener("click", loadOrders);
        form.addEventListener("submit", async event => {
            event.preventDefault();
            const button = form.querySelector("button[type=submit]");
            button.disabled = true;
            TM.message(productMessage, "Guardando producto…");
            try {
                const product = await TM.request("/products", { method: "POST", body: JSON.stringify({
                    nombre: document.querySelector("#product-name").value.trim(),
                    descripcion: document.querySelector("#product-description").value.trim(),
                    precio: Number(document.querySelector("#product-price").value),
                    stock: Number(document.querySelector("#product-stock").value)
                }) });
                form.reset();
                TM.message(productMessage, `Producto #${product.id} guardado. Ya aparece en la tienda.`);
                await loadProducts();
            } catch (error) { TM.message(productMessage, error.message, true); }
            finally { button.disabled = false; }
        });
        await Promise.all([loadProducts(), loadOrders()]);
    } catch (error) { TM.message(accessMessage, error.message, true); }
})();
