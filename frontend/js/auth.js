if (TM.isLocal) {
    const notice = TM.element("div", "local-mode-notice", "Demostración local · Productos y Pedidos · Usuario de prueba 1");
    document.body.prepend(notice);
    if (document.querySelector("#login-form, #register-form")) window.location.replace("index.html");
}

const loginLink = document.querySelector("#login-link");
const registerLink = document.querySelector("#register-link");
const accountLink = document.querySelector("#account-link");
const logoutButton = document.querySelector("#logout-button");

function renderSession(user) {
    if (loginLink) loginLink.hidden = !!user;
    if (registerLink) registerLink.hidden = !!user;
    if (logoutButton) logoutButton.hidden = !user || TM.isLocal;
    if (accountLink) {
        accountLink.hidden = !user;
        accountLink.textContent = user?.rol === "ADMIN" ? "Panel de administrador" : "Mi perfil";
        accountLink.href = user?.rol === "ADMIN" ? "admin-dashboard.html" : "profile.html";
    }
}

logoutButton?.addEventListener("click", () => {
    localStorage.removeItem("token");
    window.location.href = "login.html";
});
window.addEventListener("tm-session-ended", () => renderSession(null));
TM.session().then(renderSession).catch(() => renderSession(null));

const registerForm = document.querySelector("#register-form");
registerForm?.addEventListener("submit", async event => {
    event.preventDefault();
    const output = document.querySelector("#register-message");
    const password = document.querySelector("#password").value;
    if (password !== document.querySelector("#confirm-password").value) {
        TM.message(output, "Las contraseñas no coinciden.", true); return;
    }
    const button = registerForm.querySelector("button[type=submit]");
    button.disabled = true;
    TM.message(output, "Creando tu cuenta…");
    try {
        const data = await TM.request("/auth/register", { method: "POST", auth: false, body: JSON.stringify({
            nombre: document.querySelector("#nombre").value.trim(),
            email: document.querySelector("#email").value.trim(), password
        }) });
        TM.message(output, `${data.message}. Ya puedes iniciar sesión.`);
        registerForm.reset();
    } catch (error) { TM.message(output, error.message, true); }
    finally { button.disabled = false; }
});

const loginForm = document.querySelector("#login-form");
loginForm?.addEventListener("submit", async event => {
    event.preventDefault();
    const output = document.querySelector("#login-message");
    const button = loginForm.querySelector("button[type=submit]");
    button.disabled = true;
    TM.message(output, "Verificando tu cuenta…");
    try {
        const data = await TM.request("/auth/login", { method: "POST", auth: false, body: JSON.stringify({
            email: document.querySelector("#email").value.trim(),
            password: document.querySelector("#password").value
        }) });
        localStorage.setItem("token", data.token);
        try {
            const guest = JSON.parse(sessionStorage.getItem("techmarket-cart:guest") || "[]");
            const key = `techmarket-cart:${data.userId}`;
            const saved = JSON.parse(sessionStorage.getItem(key) || "[]");
            if (Array.isArray(guest) && Array.isArray(saved)) {
                guest.forEach(item => {
                    if (!Number.isSafeInteger(item.productId) || item.productId < 1 || !Number.isInteger(item.cantidad) || item.cantidad < 1) return;
                    const previous = saved.find(line => line.productId === item.productId);
                    if (previous) previous.cantidad = Math.min(1000, previous.cantidad + item.cantidad);
                    else saved.push({ productId: item.productId, cantidad: Math.min(item.cantidad, 1000) });
                });
                sessionStorage.setItem(key, JSON.stringify(saved.slice(0, 50)));
            }
            sessionStorage.removeItem("techmarket-cart:guest");
        } catch { /* El carrito no impide iniciar sesión. */ }
        window.location.href = "index.html";
    } catch (error) { TM.message(output, error.message, true); }
    finally { button.disabled = false; }
});
