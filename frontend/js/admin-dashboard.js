document.addEventListener("DOMContentLoaded", async () => {

    // 1. Recuperar el token guardado al iniciar sesión.
    const token = localStorage.getItem("token");

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

        console.log("Acceso autorizado para ADMIN.");

    } catch (error) {
        // 7. Manejar errores de conexión con el backend.
        console.error(
            "No se pudo verificar el acceso:",
            error
        );

        alert(
            "No se pudo verificar tu sesión. " +
            "Comprueba que los servicios estén funcionando."
        );

        window.location.replace("index.html");
    }

});
