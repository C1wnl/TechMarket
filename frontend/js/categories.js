(() => {
    const groups = [
        { id: "computo", label: "Cómputo", detail: "Laptops y computadoras", icon: "M3 4h18v13H3z M1 21h22 M8 17v4 M16 17v4", match: /\b(laptop|laptops|macbook|imac|notebook|chromebook|computadora|computadoras|pc|desktop)\b/ },
        { id: "moviles", label: "Móviles y tablets", detail: "Tecnología que va contigo", icon: "M7 2h10v20H7z M10 18h4", match: /\b(iphone|smartphone|smartphones|celular|celulares|telefono|telefonos|tablet|tablets|ipad)\b/ },
        { id: "audio", label: "Audio", detail: "Tu música, a tu manera", icon: "M4 14v-3a8 8 0 0 1 16 0v3 M4 13H2v7h5v-7z M20 13h2v7h-5v-7z", match: /\b(airpods|audifonos|auriculares|bocina|bocinas|altavoz|altavoces|headset|headphones|microfono|microfonos|soundbar)\b/ },
        { id: "gaming", label: "Gaming", detail: "Prepara tu próxima partida", icon: "M6 7h12l4 12h-5l-3-4h-4l-3 4H2z M6 10v4 M4 12h4 M16 11h1 M18 14h1", match: /\b(consola|consolas|playstation|xbox|nintendo|videojuego|videojuegos|gamepad|joystick|dualsense|gaming|gamer)\b/ },
        { id: "accesorios", label: "Accesorios", detail: "Detalles que complementan", icon: "M2 6h20v12H2z M5 10h2 M9 10h2 M13 10h2 M17 10h2 M6 14h12", match: /\b(teclado|teclados|mouse|raton|monitor|monitores|cable|cables|cargador|cargadores|adaptador|hub|funda|soporte|webcam|usb)\b/ },
        { id: "otros", label: "Otros", detail: "Más por descubrir", icon: "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z" }
    ];
    const normalize = value => String(value || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
    // Visual grouping only: Product currently has no persisted category field.
    const categoryFor = product => groups.find(group => group.match?.test(normalize(product.nombre))) || groups[groups.length - 1];
    const grid = document.getElementById("product-grid");
    const list = document.getElementById("category-list");
    const count = document.getElementById("results-count");
    const title = document.getElementById("results-title");
    const clear = document.getElementById("clear-category");
    const retry = document.getElementById("retry-products");
    let products = [];
    let loaded = false;
    const selectedFromUrl = () => {
        const value = new URLSearchParams(location.search).get("categoria");
        return groups.some(group => group.id === value) ? value : null;
    };
    let selected = selectedFromUrl();
    function choose(id) {
        selected = id;
        const url = new URL(location.href);
        if (id) url.searchParams.set("categoria", id);
        else url.searchParams.delete("categoria");
        history.pushState(null, "", url);
        render();
    }
    groups.forEach(group => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "category-tile";
        button.dataset.category = group.id;
        button.setAttribute("aria-controls", "product-grid");
        button.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" stroke-linecap="round" aria-hidden="true"><path d="${group.icon}"/></svg><span class="category-name">${group.label}</span><span class="category-detail">${group.detail}</span><span class="category-count">Cargando...</span>`;
        button.addEventListener("click", () => choose(group.id));
        list.appendChild(button);
    });
    function render() {
        const group = groups.find(group => group.id === selected);
        title.textContent = group ? group.label : "Todos los productos";
        clear.hidden = !selected;
        list.querySelectorAll("button").forEach(button => {
            button.setAttribute("aria-pressed", String(button.dataset.category === selected));
            const total = products.filter(product => categoryFor(product).id === button.dataset.category).length;
            button.querySelector(".category-count").textContent = loaded ? `${total} ${total === 1 ? "producto" : "productos"} →` : "Catálogo no disponible";
        });
        if (!loaded) return;
        const productos = selected ? products.filter(product => categoryFor(product).id === selected) : products;
        count.textContent = `${productos.length} ${productos.length === 1 ? "producto" : "productos"}`;
        grid.replaceChildren();
        if (!productos.length) {
            const message = document.createElement("p");
            message.className = "category-empty";
            message.textContent = selected ? "Por ahora no hay productos en esta categoría. Puedes explorar las demás o ver todo el catálogo." : "Actualmente no hay productos disponibles.";
            grid.appendChild(message);
            return;
        }
        const productGrid = grid;
        productos.forEach(producto => {
            const card = document.createElement("article");
            card.className = "product-card";

            const imageContainer = document.createElement("div");
            imageContainer.className = "product-image";

            const image = document.createElement("img");

            // Utilizar la imagen de Cloudinary o la imagen predeterminada.
            image.src = producto.imagenUrl ||
                "assets/products/MacBookPro.jpg";

            image.alt = producto.nombre || "Producto TechMarket";
            image.loading = "lazy";

            // Si la imagen no carga, mostrar la imagen predeterminada.
            image.onerror = () => {
                image.onerror = null;
                image.src = "assets/products/MacBookPro.jpg";
            };

            imageContainer.appendChild(image);

            const info = document.createElement("div");
            info.className = "product-info";

            const category = document.createElement("p");
            category.className = "product-category";
            category.textContent = categoryFor(producto).label;

            const name = document.createElement("h3");
            name.textContent = producto.nombre;

            const description = document.createElement("p");
            description.className = "product-description";
            description.textContent = producto.descripcion || "";

            const price = document.createElement("p");
            price.className = "product-price";
            price.textContent = new Intl.NumberFormat("es-MX", {
                style: "currency",
                currency: "MXN"
            }).format(Number(producto.precio));

            const stock = document.createElement("p");
            stock.className = "product-stock";
            stock.textContent = producto.stock > 0
                ? `Disponibles: ${producto.stock}`
                : "Agotado";

            const button = document.createElement("a");
            button.className = "product-button";
            button.href = `product-detail.html?id=${encodeURIComponent(producto.id)}`;
            button.textContent = "Ver producto →";

            info.append(category, name, description, price, stock, button);
            card.append(imageContainer, info);

            productGrid.appendChild(card);
        });
    }
    async function load() {
        retry.hidden = true;
        loaded = false;
        grid.setAttribute("aria-busy", "true");
        grid.textContent = "Cargando productos...";
        count.textContent = "Cargando catálogo...";
        list.querySelectorAll(".category-count").forEach(el => el.textContent = "Cargando...");
        list.querySelectorAll("button").forEach(el => el.disabled = true);
        try {
            const result = await API.get("/products");
            if (!Array.isArray(result)) throw new Error("Respuesta de catálogo inválida");
            products = result;
            loaded = true;
            render();
        } catch (error) {
            render();
            grid.textContent = "No fue posible cargar los productos. Comprueba la conexión e intenta nuevamente.";
            count.textContent = "Catálogo no disponible";
            retry.hidden = false;
        } finally {
            grid.setAttribute("aria-busy", "false");
            list.querySelectorAll("button").forEach(el => el.disabled = !loaded);
        }
    }
    clear.addEventListener("click", () => choose(null));
    retry.addEventListener("click", load);
    window.addEventListener("popstate", () => { selected = selectedFromUrl(); render(); });
    load();
})();
