document.addEventListener("DOMContentLoaded", async () => {

    // 1. Recuperar el token guardado al iniciar sesión.
    const token = localStorage.getItem("token");

    // Elementos del panel de administración.
    const adminGrid = document.getElementById("admin-grid");
    const adminNote = document.getElementById("admin-note");
    const adminName = document.getElementById("admin-name");

    // Elementos de gestión de usuarios.
    const showUsersButton = document.getElementById("show-users-button");
    const usersPanel = document.getElementById("users-panel");
    const refreshUsersButton = document.getElementById("refresh-users-button");
    const backToAdminButton = document.getElementById("back-to-admin-button");

    const usersCount = document.getElementById("users-count");
    const usersMessage = document.getElementById("users-message");
    const usersTableBody = document.getElementById("users-table-body");

    // 2. Si no hay sesión, regresar al inicio de sesión.
    if (!token) {
        window.location.replace("login.html");
        return;
    }

    try {
        // 3. Consultar al backend quién inició sesión.
        const response = await fetch(
            "http://localhost:8080/auth/me",
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        // 4. Si el token no es válido, eliminarlo.
        if (!response.ok) {
            localStorage.removeItem("token");
            window.location.replace("login.html");
            return;
        }

        // 5. Leer la información enviada por el backend.
        const usuario = await response.json();

        // 6. Permitir el panel únicamente a administradores.
        if (usuario.rol !== "ADMIN") {
            window.location.replace("index.html");
            return;
        }

        // Mostrar el nombre del administrador si el backend lo proporciona.
        if (adminName) {
            adminName.textContent =
                usuario.nombre || usuario.email || "Administrador";
        }

        console.log("Acceso autorizado para ADMIN.");

    } catch (error) {
        // 7. Manejar errores de conexión con el backend.
        console.error("No se pudo verificar el acceso:", error);

        alert(
            "No se pudo verificar tu sesión. " +
            "Comprueba que los servicios estén funcionando."
        );

        window.location.replace("index.html");
        return;
    }



    // 8. Consultar la lista de usuarios a través del API Gateway.
    async function cargarUsuarios() {
        // Evitar solicitudes repetidas mientras se carga la lista.
        refreshUsersButton.disabled = true;
        refreshUsersButton.textContent = "Actualizando...";

        usersMessage.textContent = "Cargando usuarios...";
        usersCount.textContent = "Consultando...";
        usersTableBody.replaceChildren();

        // Permitir que el navegador muestre el estado de carga.
        await new Promise(resolve => requestAnimationFrame(resolve));

        // Registrar cuándo comenzó la actualización.
        const inicioCarga = Date.now();

        try {
            const response = await fetch(
                "http://localhost:8080/users",
                {
                    method: "GET",
                    headers: {
                        "Authorization": `Bearer ${token}`,
                        "Accept": "application/json"
                    }
                }
            );

            // Token vencido o no válido.
            if (response.status === 401) {
                localStorage.removeItem("token");
                window.location.replace("login.html");
                return;
            }

            // El backend rechazó la operación por permisos.
            if (response.status === 403) {
                usersCount.textContent = "Sin autorización";
                usersMessage.textContent =
                    "No tienes permisos para consultar los usuarios.";
                return;
            }

            if (!response.ok) {
                throw new Error(`Error HTTP ${response.status}`);
            }

            const usuarios = await response.json();

            if (!Array.isArray(usuarios)) {
                throw new Error(
                    "El servicio no devolvió una lista de usuarios válida."
                );
            }

            // Actualizar el contador.
            usersCount.textContent =
                `Total de usuarios: ${usuarios.length}`;

            if (usuarios.length === 0) {
                usersMessage.textContent =
                    "Todavía no hay usuarios registrados.";
                return;
            }

            // Crear una fila por cada usuario.
            usuarios.forEach((usuario) => {
                const fila = document.createElement("tr");

                const idCell = document.createElement("td");
                idCell.textContent = usuario.id ?? "—";

                const nombreCell = document.createElement("td");
                nombreCell.textContent = usuario.nombre || "Sin nombre";

                const emailCell = document.createElement("td");
                emailCell.textContent = usuario.email || "Sin correo";

                const rolCell = document.createElement("td");
                const rolBadge = document.createElement("span");

                rolBadge.className = "user-role-badge";
                rolBadge.textContent = usuario.rol || "Sin rol";

                rolCell.appendChild(rolBadge);

                fila.append(
                    idCell,
                    nombreCell,
                    emailCell,
                    rolCell
                );

                usersTableBody.appendChild(fila);
            });

            usersMessage.textContent =
                "Usuarios cargados correctamente.";

        } catch (error) {
            console.error(
                "No se pudo cargar la lista de usuarios:",
                error
            );

            usersCount.textContent = "No disponible";
            usersMessage.textContent =
                "No se pudo cargar la lista. Comprueba que el API Gateway " +
                "y User Service estén funcionando e inténtalo de nuevo.";

        } finally {
            // Mantener visible el estado de carga al menos 300 ms.
            const tiempoTranscurrido = Date.now() - inicioCarga;
            const tiempoRestante = Math.max(0, 500 - tiempoTranscurrido);

            if (tiempoRestante > 0) {
                await new Promise(resolve =>
                    setTimeout(resolve, tiempoRestante)
                );
            }

            // Restaurar el botón al finalizar.
            refreshUsersButton.disabled = false;
            refreshUsersButton.textContent = "Actualizar lista";
        }
    }

    // 9. Abrir la sección de gestión de usuarios.
    showUsersButton.addEventListener("click", async () => {
        adminGrid.hidden = true;

        if (adminNote) {
            adminNote.hidden = true;
        }

        usersPanel.hidden = false;

        await cargarUsuarios();
    });

    // 10. Actualizar la lista de usuarios.
    refreshUsersButton.addEventListener("click", cargarUsuarios);

    // 11. Regresar al panel principal.
    backToAdminButton.addEventListener("click", () => {
        usersPanel.hidden = true;
        adminGrid.hidden = false;

        if (adminNote) {
            adminNote.hidden = false;
        }
    });

});
