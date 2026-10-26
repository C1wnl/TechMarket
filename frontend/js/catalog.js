const grid = document.querySelector("#product-grid");
const catalogMessage = document.querySelector("#catalog-message");
const cartMessage = document.querySelector("#cart-message");
const cartContainer = document.querySelector("#cart-items");
const orderButton = document.querySelector("#create-order");
let products = [];
let cart = [];
let submitting = false;
let cartKey = "techmarket-cart:guest";

function saveCart() {
    sessionStorage.setItem(cartKey, JSON.stringify(cart.map(({ productId, cantidad }) => ({ productId, cantidad }))));
}

function renderCart() {
    cartContainer.replaceChildren();
    if (!cart.length) cartContainer.append(TM.element("p", "empty-state", "Agrega productos del catálogo para comenzar."));
    let cents = 0;
    cart.forEach(item => {
        const row = TM.element("div", "cart-row");
        const product = products.find(p => p.id === item.productId);
        const name = product?.nombre ?? `Producto #${item.productId} no disponible`;
        const details = TM.element("div", "cart-details");
        details.append(TM.element("strong", "", name));
        if (product) details.append(TM.element("span", "muted", `${TM.money(product.precio)} por unidad · ${product.stock} disponibles`));
        const label = TM.element("label", "quantity-label", "Cantidad");
        const input = TM.element("input");
        Object.assign(input, { type: "number", min: "1", max: String(Math.min(product?.stock ?? 1000, 1000)), step: "1", value: String(item.cantidad), disabled: submitting });
        input.setAttribute("aria-label", `Cantidad de ${name}`);
        input.addEventListener("change", () => {
            if (!Number.isInteger(Number(input.value)) || Number(input.value) < 1 || Number(input.value) > Math.min(product?.stock ?? 1000, 1000)) {
                TM.message(cartMessage, "Revisa la cantidad y las existencias disponibles.", true);
                input.value = String(item.cantidad); return;
            }
            item.cantidad = Number(input.value); saveCart(); renderCart();
        });
        label.append(input);
        const remove = TM.element("button", "text-button", "Quitar");
        remove.type = "button"; remove.disabled = submitting;
        remove.setAttribute("aria-label", `Quitar ${name}`);
        remove.addEventListener("click", () => { cart = cart.filter(p => p !== item); saveCart(); renderCart(); });
        row.append(details, label, remove); cartContainer.append(row);
        if (product) cents += Math.round(Number(product.precio) * 100) * item.cantidad;
    });
    document.querySelector("#cart-total").textContent = `${TM.money(cents / 100)} MXN`;
    orderButton.disabled = submitting || !cart.length || cart.some(item => !products.some(p => p.id === item.productId));
    orderButton.textContent = submitting ? "Verificando pedido…" : "Crear pedido";
}

function addProduct(product) {
    if (submitting) return;
    const existing = cart.find(item => item.productId === product.id);
    if ((existing?.cantidad ?? 0) >= Math.min(product.stock, 1000)) {
        TM.message(cartMessage, "Ya agregaste el máximo de unidades disponibles.", true); return;
    }
    if (existing) existing.cantidad++;
    else {
        if (cart.length >= 50) { TM.message(cartMessage, "Máximo 50 productos por pedido.", true); return; }
        cart.push({ productId: product.id, cantidad: 1 });
    }
    saveCart(); renderCart();
    TM.message(cartMessage, `${product.nombre} se agregó a tu pedido.`);
}

async function loadProducts() {
    const refresh = document.querySelector("#refresh-products");
    refresh.disabled = true;
    TM.message(catalogMessage, "Cargando catálogo…");
    try {
        products = await TM.request("/products", { auth: false });
        grid.replaceChildren();
        products.forEach(product => {
            const card = TM.element("article", "product-card");
            const visual = TM.element("div", "product-image product-placeholder");
            visual.setAttribute("aria-hidden", "true");
            visual.append(TM.element("span", "placeholder-mark", "TM"), TM.element("span", "placeholder-id", `CATÁLOGO / ${String(product.id).padStart(3, "0")}`));
            const info = TM.element("div", "product-info");
            info.append(TM.element("p", "product-category", product.stock ? `${product.stock} DISPONIBLES` : "SIN EXISTENCIAS"));
            info.append(TM.element("h3", "", product.nombre));
            info.append(TM.element("p", "product-description", product.descripcion || "Producto del catálogo TechMarket."));
            info.append(TM.element("p", "product-price", `${TM.money(product.precio)} MXN`));
            const button = TM.element("button", "product-button", product.stock ? "Agregar al pedido →" : "Agotado");
            button.type = "button"; button.disabled = !product.stock;
            button.addEventListener("click", () => addProduct(product));
            info.append(button); card.append(visual, info); grid.append(card);
        });
        TM.message(catalogMessage, products.length ? `${products.length} productos en el catálogo` : "El catálogo está vacío. Un administrador puede agregar el primer producto desde su panel.");
        renderCart();
    } catch (error) {
        products = []; grid.replaceChildren(); renderCart();
        TM.message(catalogMessage, error.message, true);
    } finally { refresh.disabled = false; }
}

document.querySelector("#refresh-products").addEventListener("click", loadProducts);
orderButton.addEventListener("click", async () => {
    if (submitting || !cart.length) return;
    submitting = true; renderCart();
    try {
        const user = await TM.session();
        if (!user) {
            TM.message(cartMessage, "Inicia sesión para crear tu pedido.", true);
            window.location.href = "login.html"; return;
        }
        const order = await TM.request("/orders", { method: "POST", body: JSON.stringify({ items: cart }) });
        cart = []; saveCart();
        TM.message(cartMessage, `Pedido #${order.id} creado. Total confirmado: ${TM.money(order.total)} MXN. Puedes consultarlo en Mi perfil.`);
    } catch (error) { TM.message(cartMessage, error.message, true); }
    finally { submitting = false; renderCart(); }
});

async function initializeCatalog() {
    try {
        const user = await TM.session();
        cartKey = `techmarket-cart:${user?.userId ?? "guest"}`;
        const stored = JSON.parse(sessionStorage.getItem(cartKey) || "[]");
        cart = Array.isArray(stored) ? stored.filter(item => Number.isSafeInteger(item.productId) && item.productId > 0 && Number.isInteger(item.cantidad) && item.cantidad > 0 && item.cantidad <= 1000).slice(0, 50) : [];
    } catch { cart = []; }
    await loadProducts();
}
initializeCatalog();
