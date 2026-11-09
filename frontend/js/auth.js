const registerForm = document.querySelector("#register-form");
const loginForm = document.querySelector("#login-form");

// ============================
// REGISTRO
// ============================

if (registerForm) {
    const registerMessage = document.querySelector("#register-message");

    registerForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const nombre = document.querySelector("#nombre").value.trim();
        const email = document.querySelector("#email").value.trim();
        const password = document.querySelector("#password").value;
        const confirmPassword = document.querySelector("#confirm-password").value;

        if (password !== confirmPassword) {
            registerMessage.textContent = "Las contraseñas no coinciden.";
            return;
        }

        try {
            const result = await API.post("/auth/register", {
                nombre,
                email,
                password
            });

            registerMessage.textContent =
                result?.message || "Registro exitoso. Ya puedes iniciar sesión.";

        } catch (error) {
            registerMessage.textContent = error.message;
        }
    });
}

// ============================
// LOGIN
// ============================

if (loginForm) {
    const loginMessage = document.querySelector("#login-message");

    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const email = document.querySelector("#email").value.trim();
        const password = document.querySelector("#password").value;

        try {
            const result = await API.post("/auth/login", {
                email,
                password
            });

            if (!result?.token) {
                throw new Error("El servidor no devolvió un token válido.");
            }

            localStorage.setItem("token", result.token);
            window.location.href = "index.html";

        } catch (error) {
            loginMessage.textContent = error.message;
        }
    });
}

// ============================
// LOGOUT
// ============================

const logoutButton = document.querySelector("#logout-button");

if (logoutButton) {
    logoutButton.addEventListener("click", () => {
        localStorage.removeItem("token");
        window.location.href = "login.html";
    });
}

// ============================
// ESTADO DE SESIÓN Y ROL
// ============================

const loginLink = document.querySelector("#login-link");
const registerLink = document.querySelector("#register-link");
const accountLink = document.querySelector("#account-link");

function mostrarInvitado() {
    if (loginLink) loginLink.style.display = "inline";
    if (registerLink) registerLink.style.display = "inline-block";
    if (accountLink) accountLink.style.display = "none";
    if (logoutButton) logoutButton.style.display = "none";
}

function mostrarUsuario(rol) {
    if (loginLink) loginLink.style.display = "none";
    if (registerLink) registerLink.style.display = "none";

    if (accountLink) {
        if (rol === "ADMIN") {
            accountLink.textContent = "Panel de administrador";
            accountLink.href = "admin-dashboard.html";
        } else {
            accountLink.textContent = "Mi perfil";
            accountLink.href = "profile.html";
        }

        accountLink.style.display = "inline-block";
    }

    if (logoutButton) logoutButton.style.display = "inline-block";
}

async function verificarSesion() {
    if (!API.getToken()) {
        mostrarInvitado();
        return;
    }

    try {
        const usuario = await API.get("/auth/me");
        mostrarUsuario(usuario.rol);

    } catch (error) {
        mostrarInvitado();
        console.error("No se pudo verificar la sesión:", error);
    }
}

verificarSesion();


