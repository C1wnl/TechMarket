const registerForm = document.querySelector("#register-form");
const loginForm = document.querySelector("#login-form");


// ============================
// REGISTRO
// ============================

if (registerForm) {

    const registerMessage = document.querySelector("#register-message");

    registerForm.addEventListener("submit", function (event) {

        event.preventDefault();

        const nombre = document.querySelector("#nombre").value;
        const email = document.querySelector("#email").value;
        const password = document.querySelector("#password").value;
        const confirmPassword = document.querySelector("#confirm-password").value;

        if (password !== confirmPassword) {

            alert("Las contraseñas no coinciden.");
            return;

        }

        fetch("http://localhost:8081/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                nombre: nombre,
                email: email,
                password: password
            })
        })
            .then(response => {

                return response.json().then(data => ({
                    status: response.status,
                    data: data
                }));

            })
            .then(result => {

                if (result.status === 200) {

                    registerMessage.textContent = result.data.message;

                } else if (result.status === 409) {

                    registerMessage.textContent = result.data.message;

                }

            });

    });

}


// ============================
// LOGIN
// ============================

if (loginForm) {

    const loginMessage = document.querySelector("#login-message");
    const userInfo = document.querySelector("#user-info");

    loginForm.addEventListener("submit", function (event) {

        event.preventDefault();

        const email = document.querySelector("#email").value;
        const password = document.querySelector("#password").value;

        fetch("http://localhost:8081/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: email,
                password: password
            })
        })
            .then(response => {

                return response.json().then(data => ({
                    status: response.status,
                    data: data
                }));

            })
            .then(result => {

                console.log("Status:", result.status);
                console.log("Respuesta del servidor:", result.data);

                if (result.status === 200) {

                    localStorage.setItem("token", result.data.token);

                    window.location.href = "index.html"

                } else if (result.status === 401) {

                    loginMessage.textContent = result.data.message;

                }

            });

    });

}

// ============================
// LOGOUT
// ============================

const logoutButton = document.querySelector("#logout-button");

if (logoutButton) {

    logoutButton.addEventListener("click", function () {

        localStorage.removeItem("token");

        window.location.href = "login.html";

    });

}

// ============================
// ESTADO DE SESIÓN
// ============================

const token = localStorage.getItem("token");

const loginLink = document.querySelector("#login-link");
const registerLink = document.querySelector("#register-link");
const sessionMessage = document.querySelector("#session-message");

if (token) {

    if (loginLink) {
        loginLink.style.display = "none";
    }

    if (registerLink) {
        registerLink.style.display = "none";
    }

    if (sessionMessage) {
        sessionMessage.style.display = "inline";
    }

    if (logoutButton) {
        logoutButton.style.display = "inline-block";
    }

} else {

    if (sessionMessage) {
        sessionMessage.style.display = "none";
    }

    if (logoutButton) {
        logoutButton.style.display = "none";
    }

}