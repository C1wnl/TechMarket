(() => {
        const cartCount = document.getElementById("cart-count");

        let cartKey = null;

        function obtenerIdDesdeToken() {
            try {
                const token = API.getToken();

                if (!token) return null;

                const payloadBase64 = token.split(".")[1]
                    .replace(/-/g, "+")
                    .replace(/_/g, "/");

                const payload = JSON.parse(
                    atob(
                        payloadBase64.padEnd(
                            Math.ceil(payloadBase64.length / 4) * 4,
                            "="
                        )
                    )
                );

                return payload.sub ?? null;

            } catch (error) {
                console.error("No se pudo obtener el usuario del token:", error);
                return null;
            }
        }

        function actualizarContador() {
            if (!cartCount || !cartKey) return;

            try {
                const cart = JSON.parse(
                    localStorage.getItem(cartKey) || "[]"
                );

                const total = Array.isArray(cart)
                    ? cart.reduce((sum, item) => {
                        const quantity = Number(
                            item.quantity ?? item.cantidad ?? item.qty ?? 1
                        );

                        return sum + (
                            Number.isFinite(quantity) && quantity > 0
                                ? quantity
                                : 0
                        );
                    }, 0)
                    : 0;

                cartCount.textContent = total;
                cartCount.style.display = total > 0 ? "inline-flex" : "none";

            } catch (error) {
                console.error("Error al leer el carrito:", error);
                cartCount.textContent = "0";
                cartCount.style.display = "none";
            }
        }


        async function inicializarContador() {
            // Los invitados no necesitan consultar /auth/me.
            if (!API.getToken()) {
                cartCount.textContent = "0";
                cartCount.style.display = "none";
                return;
            }

            try {
                const session = await API.get("/auth/me");

                const userId =
                    session.userId ??
                    session.id ??
                    obtenerIdDesdeToken();

                if (userId === null || userId === undefined) {
                    cartCount.textContent = "0";
                    cartCount.style.display = "none";
                    return;
                }

                cartKey = `techmarket_cart_${userId}`;

                actualizarContador();

                window.addEventListener("storage", (event) => {
                    if (event.key === cartKey) {
                        actualizarContador();
                    }
                });

                window.addEventListener("cart-updated", actualizarContador);
                window.addEventListener("pageshow", actualizarContador);
                window.addEventListener("focus", actualizarContador);

            } catch (error) {
                console.error("No se pudo inicializar el contador:", error);

                cartCount.textContent = "0";
                cartCount.style.display = "none";
            }
        }


        inicializarContador();
    })();
