document.addEventListener("DOMContentLoaded", async () => {

    const container = document.querySelector(
        "#notifications-container"
    );

    const countElement = document.querySelector(
        "#notifications-count"
    );

    const formatDate = (date) => {
        if (!date) return "Fecha no disponible";

        const parsedDate = new Date(date);

        if (Number.isNaN(parsedDate.getTime())) {
            return "Fecha no disponible";
        }

        return parsedDate.toLocaleString("es-MX", {
            dateStyle: "medium",
            timeStyle: "short"
        });
    };

    const escapeHTML = (value) => {
        return String(value ?? "").replace(/[&<>"']/g, (character) => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#039;"
        })[character]);
    };

    const renderNotifications = (notifications) => {

        if (!Array.isArray(notifications) ||
            notifications.length === 0) {

            countElement.textContent = "0 notificaciones";

            container.innerHTML = `
<div class="profile-empty-state">
    <div class="profile-empty-icon">□</div>

<h3>No tienes notificaciones</h3>

<p>
    Aquí aparecerán las actualizaciones
    relacionadas con tus pedidos y compras.
</p>

<a href="index.html" class="profile-store-link">
    Volver a la tienda
</a>
</div>
`;

            return;
        }

        const unreadCount = notifications.filter(
            notification => notification.estado !== "LEIDA"
        ).length;

        countElement.textContent =
            `${unreadCount} pendiente(s)`;

        container.innerHTML = notifications.map(notification => {

            const isRead = notification.estado === "LEIDA";

            return `
<article class="notification-card ${isRead ? "notification-read" : "notification-unread"}">

<div class="notification-card-header">

    <div>
    <p class="profile-section-label">
    ${escapeHTML(notification.tipo ?? "NOTIFICACIÓN")}
</p>

<span class="notification-status">
                                ${escapeHTML(notification.estado ?? "PENDIENTE")}
                            </span>
</div>

<span class="notification-date">
                            ${escapeHTML(formatDate(notification.fechaCreacion))}
                        </span>

</div>

<p class="notification-message">
    ${escapeHTML(notification.mensaje)}
</p>

${
    !isRead
        ? `
                                <button
                                    class="notification-read-button"
                                    type="button"
                                    data-notification-id="${escapeHTML(notification.id)}">
                                    Marcar como leída
                                </button>
                            `
        : `
                                <span class="notification-read-label">
                                    <span></span>
                                    Leída
                                </span>
                            `
}

</article>
`;
        }).join("");
    };

    try {

        // 1. Verificar la sesión y recuperar el ID internamente.
        const session = await API.get("/auth/me");

        if (!session.userId) {
            throw new Error(
                "No se pudo identificar al usuario autenticado."
            );
        }

        // 2. Consultar exclusivamente las notificaciones del usuario.
        const notifications = await API.get(
            `/notifications/user/${encodeURIComponent(session.userId)}`
        );

        renderNotifications(notifications);

        // 3. Escuchar las acciones de marcar como leída.
        container.addEventListener("click", async (event) => {

            const button = event.target.closest(
                ".notification-read-button"
            );

            if (!button) return;

            const notificationId = button.dataset.notificationId;

            if (!notificationId) return;

            button.disabled = true;
            button.textContent = "Actualizando...";

            try {

                await API.put(
                    `/notifications/${encodeURIComponent(notificationId)}/status`,
{
    estado: "LEIDA"
}
);

// Actualizar la interfaz después de confirmar el backend.
const updatedNotifications = await API.get(
    `/notifications/user/${encodeURIComponent(session.userId)}`
);

renderNotifications(updatedNotifications);

} catch (error) {

    console.error(
        "No se pudo actualizar la notificación:",
        error
    );

    button.disabled = false;
    button.textContent = "Marcar como leída";

    alert(
        "No fue posible actualizar la notificación: " +
        error.message
    );
}

});

console.log("Notificaciones cargadas correctamente.");

} catch (error) {

    console.error(
        "Error al cargar las notificaciones:",
        error
    );

    if (container) {
        container.textContent =
            "No fue posible cargar tus notificaciones. " +
            error.message;
    }

    if (countElement) {
        countElement.textContent = "No disponible";
    }
}

});