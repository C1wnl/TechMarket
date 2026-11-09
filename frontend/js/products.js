
const productGrid = document.querySelector("#product-grid");

async function cargarProductos() {
    if (!productGrid) return;

    const productsMessage = document.querySelector("#products-message");

    try {
        const productos = await API.get("/products");

        if (!Array.isArray(productos) || productos.length === 0) {
            productGrid.innerHTML = `
                <p class="products-message">
                    Actualmente no hay productos disponibles.
                </p>
            `;
            return;
        }

        productGrid.innerHTML = "";

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
            category.textContent = "TECNOLOGÍA";

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

    } catch (error) {
        console.error("Error al cargar productos:", error);

        productGrid.innerHTML = `
            <p class="products-message">
                No fue posible cargar los productos.
                Intenta nuevamente más tarde.
            </p>
        `;
    }
}

cargarProductos();
