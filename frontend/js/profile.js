
document.addEventListener("DOMContentLoaded", async () => {

    // Recuperar el token de la sesión actual.
    const token = localStorage.getItem("token");

    // Si no hay token, solicitar inicio de sesión.
    if (!token) {
        window.location.replace("login.html");
        return;
    }

    try {
        // Consultar la información de la sesión al backend.
        const response = await fetch(
            "http://localhost:8080/auth/me",
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        // Si el token no es válido, eliminarlo.
        if (!response.ok) {
            localStorage.removeItem("token");
            window.location.replace("login.html");
            return;
        }

        // Convertir la respuesta del backend en un objeto JavaScript.
        const usuario = await response.json();

        // Obtener el identificador y el rol reales.
        const userId = usuario.userId;
        const rol = usuario.rol ?? "USER";

        // Mostrar el identificador de usuario.
        const userIdElement =
            document.querySelector("#profile-user-id");

        if (userIdElement) {
            userIdElement.textContent =
                userId ?? "No disponible";
        }

        // Mostrar el rol en la sección de información.
        const userRoleElement =
            document.querySelector("#profile-user-role");

        if (userRoleElement) {
            userRoleElement.textContent = rol;
        }

        // Mostrar una etiqueta amigable del rol.
        const profileRoleElement =
            document.querySelector("#profile-role");

        if (profileRoleElement) {
            profileRoleElement.textContent =
                rol === "ADMIN"
                    ? "Administrador"
                    : "Usuario";
        }

        // Configurar el avatar provisional.
        const avatar =
            document.querySelector("#profile-avatar");

        if (avatar) {
            avatar.textContent =
                rol === "ADMIN" ? "A" : "U";
        }

        // Mostrar el tipo de cuenta.
        const displayName =
            document.querySelector("#profile-display-name");

        if (displayName) {
            displayName.textContent =
                rol === "ADMIN"
                    ? "Cuenta administrativa"
                    : "Mi cuenta";
        }

        console.log("Perfil cargado correctamente:", usuario);

    } catch (error) {
        console.error(
            "No se pudo cargar el perfil:",
            error
        );

        alert(
            "No fue posible conectar con TechMarket. " +
            "Comprueba que los servicios estén funcionando."
        );
    }

});
