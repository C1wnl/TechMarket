const API_BASE_URL = "http://localhost:8080";

const API = {
    getToken() {
        return localStorage.getItem("token");
    },

    async request(path, options = {}) {
        const token = this.getToken();

        const headers = {
            "Accept": "application/json",
            ...options.headers
        };

        if (token) {
            headers["Authorization"] = `Bearer ${token}`;
        }

        if (options.body && !(options.body instanceof FormData)) {
            headers["Content-Type"] = "application/json";
        }

        let response;

        try {
            response = await fetch(`${API_BASE_URL}${path}`, {
                ...options,
                headers
            });
        } catch (error) {
            throw new Error(
                "No se pudo conectar con TechMarket. Comprueba que los servicios estén funcionando."
            );
        }


        if (response.status === 401) {
            // Si había un token, la sesión puede haber expirado.
            if (token) {
                localStorage.removeItem("token");
            }

            // No redirigir globalmente: cada página protegida
            // debe gestionar su propia autenticación.
            throw new Error(
                "No tienes autorización para realizar esta operación. Inicia sesión si es necesario."
            );
        }


        if (!response.ok) {
            let message = `Error HTTP ${response.status}`;

            try {
                const data = await response.json();
                message = data.message || data.detail || data.error || message;
            } catch {
                // La respuesta no contiene JSON.
            }

            throw new Error(message);
        }

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";

        if (contentType.includes("application/json")) {
            return response.json();
        }

        return response.text();
    },

    get(path) {
        return this.request(path);
    },

    post(path, data, extraHeaders = {}) {
        return this.request(path, {
            method: "POST",
            headers: extraHeaders,
            body: JSON.stringify(data)
        });
    },

    put(path, data) {
        return this.request(path, {
            method: "PUT",
            body: JSON.stringify(data)
        });
    },

    delete(path) {
        return this.request(path, {
            method: "DELETE"
        });
    }
};
