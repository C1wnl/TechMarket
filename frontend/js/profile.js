(async () => {
    const output = document.querySelector("#orders-message");
    const refresh = document.querySelector("#refresh-orders");
    async function loadOrders() {
        refresh.disabled = true;
        TM.message(output, "Cargando pedidos…");
        try {
            const orders = await TM.request("/orders");
            const user = await TM.session();
            TM.renderOrders(document.querySelector("#orders-list"), orders.filter(order => order.userId === user?.userId));
            TM.message(output, "");
        } catch (error) { TM.message(output, error.message, true); }
        finally { refresh.disabled = false; }
    }
    try {
        const user = await TM.requireSession();
        if (!user) return;
        document.querySelector("#profile-user-id").textContent = user.userId;
        document.querySelector("#profile-user-role").textContent = user.rol;
        document.querySelector("#profile-role").textContent = user.rol === "ADMIN" ? "Administrador" : "Usuario";
        document.querySelector("#profile-avatar").textContent = user.rol === "ADMIN" ? "A" : "U";
        document.querySelector("#profile-display-name").textContent = user.rol === "ADMIN" ? "Cuenta administrativa" : "Mi cuenta";
        document.querySelector(".profile-session-status").textContent = user.demo ? "Demostración local" : "Sesión verificada";
        if (user.demo) {
            document.querySelector("#profile-user-role").textContent = "DEMO LOCAL";
            document.querySelector("#profile-role").textContent = "Usuario de prueba";
            document.querySelector("#profile-display-name").textContent = "Demostración local";
            document.querySelector("#profile-avatar").textContent = "1";
            document.querySelector(".profile-lock").textContent = "Modo local";
            document.querySelector(".profile-description").textContent = "Los pedidos de esta prueba se asocian al usuario de demostración 1. No necesitas iniciar Auth ni User.";
        }
        refresh.addEventListener("click", loadOrders);
        await loadOrders();
    } catch (error) { TM.message(output, error.message, true); }
})();
