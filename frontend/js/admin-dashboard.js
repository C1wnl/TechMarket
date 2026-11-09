document.addEventListener("DOMContentLoaded", async () => {

    // ========================================
    // 1. ELEMENTOS DEL PANEL
    // ========================================

    const $ = (id) => document.getElementById(id);

    const adminGrid = $("admin-grid");
    const adminNote = $("admin-note");
    const adminName = $("admin-name");

    // Usuarios
    const showUsersButton = $("show-users-button");
    const usersPanel = $("users-panel");
    const refreshUsersButton = $("refresh-users-button");
    const backToAdminButton = $("back-to-admin-button");
    const usersCount = $("users-count");
    const usersMessage = $("users-message");
    const usersTableBody = $("users-table-body");

    // Productos
    const showProductsButton = $("show-products-button");
    const productsPanel = $("products-panel");
    const refreshProductsButton = $("refresh-products-button");
    const backFromProductsButton = $("back-from-products-button");
    const productsCount = $("products-count");
    const productsMessage = $("products-message");
    const productsTableBody = $("products-table-body");

    // Pedidos
    const showOrdersButton = $("show-orders-button");
    const ordersPanel = $("orders-panel");
    const refreshOrdersButton = $("refresh-orders-button");
    const backFromOrdersButton = $("back-from-orders-button");
    const ordersCount = $("orders-count");
    const ordersMessage = $("orders-message");
    const ordersTableBody = $("orders-table-body");

    const orderDetailPanel = $("order-detail-panel");
    const orderDetailTitle = $("order-detail-title");
    const orderDetailCustomer = $("order-detail-customer");
    const orderItemsTableBody = $("order-items-table-body");
    const orderDetailTotal = $("order-detail-total");
    const closeOrderDetailButton = $("close-order-detail-button");

    // Pagos
    const showPaymentsButton = $("show-payments-button");
    const paymentsPanel = $("payments-panel");
    const refreshPaymentsButton = $("refresh-payments-button");
    const backFromPaymentsButton = $("back-from-payments-button");
    const paymentsCount = $("payments-count");
    const paymentsMessage = $("payments-message");
    const paymentsTableBody = $("payments-table-body");

    // Formulario de productos
    const createProductForm = $("create-product-form");
    const productNameInput = $("product-name");
    const productDescriptionInput = $("product-description");
    const productPriceInput = $("product-price");
    const productStockInput = $("product-stock");

    const productImageInput = $("product-image");
    const productImagePreviewContainer = $("product-image-preview-container");
    const productImagePreview = $("product-image-preview");
    const removeProductImageButton = $("remove-product-image-button");

    const createProductButton = $("create-product-button");
    const createProductMessage = $("create-product-message");

    const productFormTag = $("product-form-tag");
    const productFormTitle = $("product-form-title");
    const productFormDescription = $("product-form-description");
    const cancelEditButton = $("cancel-edit-button");

    const MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    const ALLOWED_IMAGE_TYPES = [
        "image/jpeg",
        "image/png",
        "image/webp"
    ];

    let previewObjectUrl = null;
    let productosActuales = [];
    let pedidosActuales = [];
    let pagosActuales = [];
    let productoEnEdicion = null;
    let eliminarImagenActual = false;

    const panels = [
        usersPanel,
        productsPanel,
        ordersPanel,
        paymentsPanel
    ];


    // ========================================
    // 2. FUNCIONES GENERALES
    // ========================================

    function formatPrice(price) {
        return new Intl.NumberFormat("es-MX", {
            style: "currency",
            currency: "MXN"
        }).format(Number(price) || 0);
    }

    function formatDate(date) {
        if (!date) return "Fecha no disponible";

        const parsedDate = new Date(date);

        if (Number.isNaN(parsedDate.getTime())) {
            return "Fecha no disponible";
        }

        return new Intl.DateTimeFormat("es-MX", {
            dateStyle: "medium",
            timeStyle: "short"
        }).format(parsedDate);
    }

    function mostrarMensaje(elemento, mensaje, color = "#64748b") {
        elemento.style.color = color;
        elemento.textContent = mensaje;
    }

    function crearCelda(valor) {
        const cell = document.createElement("td");
        cell.textContent = valor ?? "—";
        return cell;
    }

    function volverAlPanelPrincipal() {
        panels.forEach(panel => panel.hidden = true);

        orderDetailPanel.hidden = true;
        adminGrid.hidden = false;
        adminNote.hidden = false;
    }

    function abrirPanel(panelSeleccionado) {
        adminGrid.hidden = true;
        adminNote.hidden = true;

        panels.forEach(panel => {
            panel.hidden = panel !== panelSeleccionado;
        });

        orderDetailPanel.hidden = true;
    }

    function crearBadge(estado) {
        const badge = document.createElement("span");
        badge.className = "user-role-badge";
        badge.textContent = estado || "Sin estado";
        return badge;
    }


    // ========================================
    // 3. VERIFICAR SESIÓN Y ROL
    // ========================================

    if (!API.getToken()) {
        window.location.replace("login.html");
        return;
    }

    try {
        const session = await API.get("/auth/me");

        if (session.rol !== "ADMIN") {
            window.location.replace("index.html");
            return;
        }

        try {
            const user = await API.get("/users/me");
            adminName.textContent =
                user.nombre || user.email || "Administrador";
        } catch (error) {
            console.warn("No se pudo obtener el perfil:", error);
            adminName.textContent = "Administrador";
        }

        console.log("Acceso autorizado para ADMIN.");

    } catch (error) {
        console.error("No se pudo verificar el acceso:", error);

        alert(
            "No se pudo verificar tu sesión. " +
            "Comprueba que los servicios estén funcionando."
        );

        window.location.replace("login.html");
        return;
    }


    // ========================================
    // 4. VISTA PREVIA DE IMAGEN
    // ========================================

    function limpiarVistaPrevia() {
        if (previewObjectUrl) {
            URL.revokeObjectURL(previewObjectUrl);
            previewObjectUrl = null;
        }

        productImagePreview.removeAttribute("src");
        productImagePreviewContainer.hidden = true;
    }

    function mostrarVistaPrevia(url) {
        limpiarVistaPrevia();

        if (!url) return;

        productImagePreview.src = url;
        productImagePreviewContainer.hidden = false;
    }

    function mostrarVistaPreviaArchivo(archivo) {
        if (previewObjectUrl) {
            URL.revokeObjectURL(previewObjectUrl);
        }

        previewObjectUrl = URL.createObjectURL(archivo);
        productImagePreview.src = previewObjectUrl;
        productImagePreviewContainer.hidden = false;
    }

    productImageInput.addEventListener("change", () => {
        const archivo = productImageInput.files[0];

        if (!archivo) return;

        if (!ALLOWED_IMAGE_TYPES.includes(archivo.type)) {
            productImageInput.value = "";
            limpiarVistaPrevia();

            mostrarMensaje(
                createProductMessage,
                "Formato no permitido. Selecciona JPG, PNG o WEBP.",
                "#dc2626"
            );
            return;
        }

        if (archivo.size > MAX_IMAGE_SIZE) {
            productImageInput.value = "";
            limpiarVistaPrevia();

            mostrarMensaje(
                createProductMessage,
                "La imagen supera el límite permitido de 5 MB.",
                "#dc2626"
            );
            return;
        }

        eliminarImagenActual = false;
        mostrarVistaPreviaArchivo(archivo);
        createProductMessage.textContent = "";
    });


    // ========================================
    // 5. QUITAR IMAGEN
    // ========================================

    removeProductImageButton.addEventListener("click", () => {
        productImageInput.value = "";
        limpiarVistaPrevia();

        if (productoEnEdicion) {
            eliminarImagenActual = true;
        }

        mostrarMensaje(
            createProductMessage,
            "Se quitó la imagen del formulario."
        );
    });


    // ========================================
    // 6. CARGAR USUARIOS
    // ========================================

    async function cargarUsuarios() {
        refreshUsersButton.disabled = true;
        refreshUsersButton.textContent = "Actualizando...";

        usersMessage.textContent = "Cargando usuarios...";
        usersCount.textContent = "Consultando...";
        usersTableBody.replaceChildren();

        try {
            const usuarios = await API.get("/users");

            if (!Array.isArray(usuarios)) {
                throw new Error(
                    "User Service no devolvió una lista válida."
                );
            }

            usersCount.textContent =
                `Total de usuarios: ${usuarios.length}`;

            if (usuarios.length === 0) {
                usersMessage.textContent =
                    "Todavía no hay usuarios registrados.";
                return;
            }

            usuarios.forEach(usuario => {
                const fila = document.createElement("tr");

                const rolCell = document.createElement("td");
                rolCell.appendChild(crearBadge(usuario.rol));

                fila.append(
                    crearCelda(usuario.id),
                    crearCelda(usuario.nombre || "Sin nombre"),
                    crearCelda(usuario.email || "Sin correo"),
                    rolCell
                );

                usersTableBody.appendChild(fila);
            });

            usersMessage.textContent =
                "Usuarios cargados correctamente.";

        } catch (error) {
            console.error("No se pudo cargar la lista de usuarios:", error);

            usersCount.textContent = "No disponible";
            usersMessage.textContent =
                `No se pudo cargar la lista de usuarios. ${error.message}`;

        } finally {
            refreshUsersButton.disabled = false;
            refreshUsersButton.textContent = "Actualizar lista";
        }
    }


    // ========================================
    // 7. CARGAR PRODUCTOS
    // ========================================

    async function cargarProductos() {
        refreshProductsButton.disabled = true;
        refreshProductsButton.textContent = "Actualizando...";

        productsMessage.textContent = "Cargando productos...";
        productsCount.textContent = "Consultando...";
        productsTableBody.replaceChildren();

        try {
            const productos = await API.get("/products");

            if (!Array.isArray(productos)) {
                throw new Error(
                    "Product Service no devolvió una lista válida."
                );
            }

            productosActuales = productos;

            productsCount.textContent =
                `Total de productos: ${productos.length}`;

            if (productos.length === 0) {
                productsMessage.textContent =
                    "Todavía no hay productos registrados.";
                return;
            }

            productos.forEach(producto => {
                const fila = document.createElement("tr");

                const accionesCell = document.createElement("td");
                const accionesContainer = document.createElement("div");

                accionesContainer.style.display = "flex";
                accionesContainer.style.gap = "8px";
                accionesContainer.style.flexWrap = "wrap";

                const editarButton = document.createElement("button");
                editarButton.type = "button";
                editarButton.className = "admin-card-button";
                editarButton.textContent = "Editar";
                editarButton.dataset.action = "edit";
                editarButton.dataset.id = producto.id;

                const eliminarButton = document.createElement("button");
                eliminarButton.type = "button";
                eliminarButton.className = "logout-button";
                eliminarButton.textContent = "Eliminar";
                eliminarButton.dataset.action = "delete";
                eliminarButton.dataset.id = producto.id;

                accionesContainer.append(editarButton, eliminarButton);
                accionesCell.appendChild(accionesContainer);

                fila.append(
                    crearCelda(producto.id),
                    crearCelda(producto.nombre || "Sin nombre"),
                    crearCelda(producto.descripcion || "Sin descripción"),
                    crearCelda(formatPrice(producto.precio)),
                    crearCelda(producto.stock ?? 0),
                    accionesCell
                );

                productsTableBody.appendChild(fila);
            });

            productsMessage.textContent =
                "Catálogo cargado correctamente.";

        } catch (error) {
            console.error("No se pudo cargar el catálogo:", error);

            productosActuales = [];
            productsCount.textContent = "No disponible";
            productsMessage.textContent =
                `No se pudieron cargar los productos. ${error.message}`;

        } finally {
            refreshProductsButton.disabled = false;
            refreshProductsButton.textContent = "Actualizar catálogo";
        }
    }


    // ========================================
    // 8. CARGAR PEDIDOS ADMINISTRATIVOS
    // ========================================

    async function cargarPedidos() {
        refreshOrdersButton.disabled = true;
        refreshOrdersButton.textContent = "Actualizando...";

        ordersMessage.textContent = "Consultando pedidos...";
        ordersCount.textContent = "Consultando...";
        ordersTableBody.replaceChildren();
        orderDetailPanel.hidden = true;

        try {
            const pedidos = await API.get("/orders/admin");

            if (!Array.isArray(pedidos)) {
                throw new Error(
                    "Order Service no devolvió una lista válida."
                );
            }

            pedidosActuales = pedidos;

            ordersCount.textContent =
                `Total de pedidos: ${pedidos.length}`;

            if (pedidos.length === 0) {
                ordersMessage.textContent =
                    "Todavía no hay pedidos registrados.";
                return;
            }

            pedidos.forEach(pedido => {
                const fila = document.createElement("tr");

                const estadoCell = document.createElement("td");
                estadoCell.appendChild(crearBadge(pedido.estado));

                const detalleCell = document.createElement("td");
                const detalleButton = document.createElement("button");

                detalleButton.type = "button";
                detalleButton.className = "admin-card-button";
                detalleButton.textContent = "Ver detalle";

                detalleButton.addEventListener("click", () => {
                    mostrarDetallePedido(pedido);
                });

                detalleCell.appendChild(detalleButton);

                fila.append(
                    crearCelda(
                        pedido.numeroPedido != null
                            ? `#${pedido.numeroPedido}`
                            : "—"
                    ),
                    crearCelda(
                        pedido.nombreCliente || "Cliente no disponible"
                    ),
                    crearCelda(
                        pedido.emailCliente || "Correo no disponible"
                    ),
                    crearCelda(formatDate(pedido.fechaCreacion)),
                    estadoCell,
                    crearCelda(formatPrice(pedido.total)),
                    detalleCell
                );

                ordersTableBody.appendChild(fila);
            });

            ordersMessage.textContent =
                "Pedidos cargados correctamente.";

        } catch (error) {
            console.error("No se pudieron cargar los pedidos:", error);

            pedidosActuales = [];
            ordersCount.textContent = "No disponible";
            ordersMessage.textContent =
                `No se pudieron cargar los pedidos. ${error.message}`;

        } finally {
            refreshOrdersButton.disabled = false;
            refreshOrdersButton.textContent = "Actualizar pedidos";
        }
    }


    // ========================================
    // 9. MOSTRAR DETALLE DE UN PEDIDO
    // ========================================

    function mostrarDetallePedido(pedido) {
        orderItemsTableBody.replaceChildren();

        orderDetailTitle.textContent =
            `Pedido #${pedido.numeroPedido}`;

        orderDetailCustomer.textContent =
            `${pedido.nombreCliente || "Cliente no disponible"} · ` +
            `${pedido.emailCliente || "Correo no disponible"}`;

        const items = Array.isArray(pedido.items)
            ? pedido.items
            : [];

        if (items.length === 0) {
            const fila = document.createElement("tr");
            const celda = document.createElement("td");

            celda.colSpan = 4;
            celda.textContent =
                "Este pedido no contiene productos registrados.";

            fila.appendChild(celda);
            orderItemsTableBody.appendChild(fila);

        } else {
            items.forEach(item => {
                const fila = document.createElement("tr");

                fila.append(
                    crearCelda(item.nombre || "Producto sin nombre"),
                    crearCelda(formatPrice(item.precioUnitario)),
                    crearCelda(item.cantidad ?? 0),
                    crearCelda(formatPrice(item.subtotal))
                );

                orderItemsTableBody.appendChild(fila);
            });
        }

        orderDetailTotal.textContent = formatPrice(pedido.total);
        orderDetailPanel.hidden = false;

        orderDetailPanel.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }

    closeOrderDetailButton.addEventListener("click", () => {
        orderDetailPanel.hidden = true;
        orderItemsTableBody.replaceChildren();
    });


    // ========================================
    // 10. CARGAR PAGOS
    // ========================================

    async function cargarPagos() {
        refreshPaymentsButton.disabled = true;
        refreshPaymentsButton.textContent = "Actualizando...";

        paymentsMessage.textContent = "Consultando pagos...";
        paymentsCount.textContent = "Consultando...";
        paymentsTableBody.replaceChildren();

        try {
            const pagos = await API.get("/payments");

            if (!Array.isArray(pagos)) {
                throw new Error(
                    "Payment Service no devolvió una lista válida."
                );
            }

            pagosActuales = pagos;

            const pendientes = pagos.filter(
                pago => pago.estado === "PENDIENTE"
            ).length;

            paymentsCount.textContent =
                `Total de pagos: ${pagos.length} · Pendientes: ${pendientes}`;

            if (pagos.length === 0) {
                paymentsMessage.textContent =
                    "Todavía no hay pagos registrados.";
                return;
            }

            pagos.forEach(pago => {
                const fila = document.createElement("tr");

                const estadoCell = document.createElement("td");
                estadoCell.appendChild(crearBadge(pago.estado));

                const accionesCell = document.createElement("td");
                const accionesContainer = document.createElement("div");

                accionesContainer.style.display = "flex";
                accionesContainer.style.gap = "8px";
                accionesContainer.style.flexWrap = "wrap";

                if (pago.estado === "PENDIENTE") {
                    const aprobarButton = document.createElement("button");

                    aprobarButton.type = "button";
                    aprobarButton.className = "admin-card-button";
                    aprobarButton.textContent = "Aprobar";
                    aprobarButton.dataset.paymentAction = "APROBADO";
                    aprobarButton.dataset.id = pago.id;

                    const rechazarButton = document.createElement("button");

                    rechazarButton.type = "button";
                    rechazarButton.className = "logout-button";
                    rechazarButton.textContent = "Rechazar";
                    rechazarButton.dataset.paymentAction = "RECHAZADO";
                    rechazarButton.dataset.id = pago.id;

                    accionesContainer.append(
                        aprobarButton,
                        rechazarButton
                    );

                } else {
                    const finalizado = document.createElement("span");
                    finalizado.textContent = "Sin acciones disponibles";
                    accionesContainer.appendChild(finalizado);
                }

                accionesCell.appendChild(accionesContainer);

                fila.append(
                    crearCelda(pago.id),
                    crearCelda(
                        pago.numeroPedido != null
                            ? `#${pago.numeroPedido}`
                            : "—"
                    ),
                    crearCelda(formatPrice(pago.monto)),
                    crearCelda(pago.metodoPago || "No especificado"),
                    estadoCell,
                    crearCelda(formatDate(pago.fechaCreacion)),
                    crearCelda(formatDate(pago.fechaActualizacion)),
                    accionesCell
                );

                paymentsTableBody.appendChild(fila);
            });

            paymentsMessage.textContent =
                "Pagos cargados correctamente. Los pagos pendientes pueden aprobarse o rechazarse.";

        } catch (error) {
            console.error("No se pudieron cargar los pagos:", error);

            pagosActuales = [];
            paymentsCount.textContent = "No disponible";
            paymentsMessage.textContent =
                `No se pudieron cargar los pagos. ${error.message}`;

        } finally {
            refreshPaymentsButton.disabled = false;
            refreshPaymentsButton.textContent = "Actualizar pagos";
        }
    }


    // ========================================
    // 11. APROBAR O RECHAZAR UN PAGO
    // ========================================

    paymentsTableBody.addEventListener("click", async event => {
        const button = event.target.closest("button[data-payment-action]");

        if (!button) return;

        const paymentId = Number(button.dataset.id);
        const nuevoEstado = button.dataset.paymentAction;

        const pago = pagosActuales.find(
            item => Number(item.id) === paymentId
        );

        if (!pago) {
            mostrarMensaje(
                paymentsMessage,
                "No se encontró el pago seleccionado. Actualiza la lista.",
                "#dc2626"
            );
            return;
        }

        if (pago.estado !== "PENDIENTE") {
            mostrarMensaje(
                paymentsMessage,
                "Este pago ya no está pendiente y no puede modificarse.",
                "#dc2626"
            );

            await cargarPagos();
            return;
        }

        if (!["APROBADO", "RECHAZADO"].includes(nuevoEstado)) {
            return;
        }

        const accion = nuevoEstado === "APROBADO"
            ? "aprobar"
            : "rechazar";

        const confirmar = window.confirm(
            `¿Deseas ${accion} el pago #${paymentId} ` +
            `correspondiente al pedido #${pago.numeroPedido}?\n\n` +
            `Monto: ${formatPrice(pago.monto)}\n\n` +
            "Esta acción iniciará el flujo de actualización del pedido."
        );

        if (!confirmar) return;

        const botones = paymentsTableBody.querySelectorAll(
            `button[data-id="${paymentId}"]`
        );

        botones.forEach(item => item.disabled = true);
        button.textContent = "Procesando...";

        mostrarMensaje(
            paymentsMessage,
            `Enviando solicitud para ${accion} el pago #${paymentId}...`
        );

        try {
            await API.put(
                `/payments/${paymentId}/status`,
                { estado: nuevoEstado }
            );

            mostrarMensaje(
                paymentsMessage,
                `Pago #${paymentId} actualizado a ${nuevoEstado}. ` +
                "Kafka notificará el resultado a Order Service. " +
                "Actualiza los pedidos para consultar el estado final.",
                "#16a34a"
            );

            await cargarPagos();

        } catch (error) {
            console.error("No se pudo actualizar el pago:", error);

            mostrarMensaje(
                paymentsMessage,
                `No se pudo actualizar el pago. ${error.message}`,
                "#dc2626"
            );

            await cargarPagos();
        }
    });


    // ========================================
    // 12. SUBIR IMAGEN A CLOUDINARY
    // ========================================

    async function subirImagenCloudinary(archivo) {
        const formData = new FormData();
        formData.append("imagen", archivo);

        const respuesta = await API.request(
            "/products/images",
            {
                method: "POST",
                body: formData
            }
        );

        if (!respuesta || !respuesta.imagenUrl) {
            throw new Error(
                "Cloudinary no devolvió una URL válida."
            );
        }

        return respuesta.imagenUrl;
    }


    // ========================================
    // 13. RESTABLECER FORMULARIO
    // ========================================

    function restablecerFormulario() {
        productoEnEdicion = null;
        eliminarImagenActual = false;

        createProductForm.reset();
        productImageInput.value = "";
        limpiarVistaPrevia();

        productFormTag.textContent = "NUEVO REGISTRO";
        productFormTitle.textContent = "Agregar producto";

        productFormDescription.textContent =
            "Completa los datos para incorporar un producto al catálogo.";

        createProductButton.textContent = "Registrar producto";
        cancelEditButton.hidden = true;

        createProductMessage.textContent = "";
        createProductMessage.style.color = "#64748b";
    }


    // ========================================
    // 14. PREPARAR EDICIÓN DE PRODUCTO
    // ========================================

    function editarProducto(producto) {
        productoEnEdicion = producto;
        eliminarImagenActual = false;

        productNameInput.value = producto.nombre || "";
        productDescriptionInput.value = producto.descripcion || "";
        productPriceInput.value = producto.precio ?? "";
        productStockInput.value = producto.stock ?? 0;

        productImageInput.value = "";

        if (producto.imagenUrl) {
            mostrarVistaPrevia(producto.imagenUrl);
        } else {
            limpiarVistaPrevia();
        }

        productFormTag.textContent = "EDITAR REGISTRO";
        productFormTitle.textContent = "Modificar producto";

        productFormDescription.textContent =
            `Estás modificando el producto #${producto.id}.`;

        createProductButton.textContent = "Guardar cambios";
        cancelEditButton.hidden = false;

        mostrarMensaje(
            createProductMessage,
            "Modifica los campos necesarios y guarda los cambios."
        );

        createProductForm.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }


    // ========================================
    // 15. VALIDAR DATOS DEL PRODUCTO
    // ========================================

    function validarDatosProducto() {
        const nombre = productNameInput.value.trim();
        const descripcion = productDescriptionInput.value.trim();
        const precioTexto = productPriceInput.value.trim();
        const stockTexto = productStockInput.value.trim();

        const precio = Number(precioTexto);
        const stock = Number(stockTexto);

        if (!nombre || nombre.length > 100) {
            throw new Error(
                "El nombre es obligatorio y no debe superar 100 caracteres."
            );
        }

        if (descripcion.length > 500) {
            throw new Error(
                "La descripción no debe superar 500 caracteres."
            );
        }

        if (
            !Number.isFinite(precio) ||
            precio <= 0 ||
            !/^\d+(\.\d{1,2})?$/.test(precioTexto)
        ) {
            throw new Error(
                "Introduce un precio mayor que cero y con máximo dos decimales."
            );
        }

        if (!Number.isInteger(stock) || stock < 0) {
            throw new Error(
                "El stock debe ser un número entero igual o mayor que cero."
            );
        }

        return { nombre, descripcion, precio, stock };
    }


    // ========================================
    // 16. REGISTRAR O ACTUALIZAR PRODUCTO
    // ========================================

    createProductForm.addEventListener("submit", async event => {
        event.preventDefault();

        createProductMessage.textContent = "";
        createProductMessage.style.color = "#dc2626";

        if (!createProductForm.reportValidity()) return;

        let datos;

        try {
            datos = validarDatosProducto();
        } catch (error) {
            createProductMessage.textContent = error.message;
            return;
        }

        const archivo = productImageInput.files[0];

        if (archivo) {
            if (!ALLOWED_IMAGE_TYPES.includes(archivo.type)) {
                createProductMessage.textContent =
                    "La imagen debe ser JPG, PNG o WEBP.";
                return;
            }

            if (archivo.size > MAX_IMAGE_SIZE) {
                createProductMessage.textContent =
                    "La imagen no puede superar los 5 MB.";
                return;
            }
        }

        createProductButton.disabled = true;
        cancelEditButton.disabled = true;

        const estabaEditando = productoEnEdicion !== null;

        try {
            let imagenUrl = estabaEditando
                ? productoEnEdicion.imagenUrl || null
                : null;

            if (archivo) {
                createProductButton.textContent = "Subiendo imagen...";
                mostrarMensaje(
                    createProductMessage,
                    "Subiendo imagen a Cloudinary..."
                );

                imagenUrl = await subirImagenCloudinary(archivo);

            } else if (estabaEditando && eliminarImagenActual) {
                imagenUrl = null;
            }

            const producto = { ...datos, imagenUrl };

            if (estabaEditando) {
                createProductButton.textContent = "Guardando cambios...";

                await API.put(
                    `/products/${productoEnEdicion.id}`,
                    producto
                );

            } else {
                createProductButton.textContent = "Registrando producto...";
                await API.post("/products", producto);
            }

            restablecerFormulario();

            mostrarMensaje(
                createProductMessage,
                estabaEditando
                    ? "Producto actualizado correctamente."
                    : "Producto registrado correctamente.",
                "#16a34a"
            );

            await cargarProductos();

        } catch (error) {
            console.error("No se pudo guardar el producto:", error);

            mostrarMensaje(
                createProductMessage,
                `No se pudo guardar el producto. ${error.message}`,
                "#dc2626"
            );

        } finally {
            createProductButton.disabled = false;
            cancelEditButton.disabled = false;

            createProductButton.textContent =
                productoEnEdicion
                    ? "Guardar cambios"
                    : "Registrar producto";
        }
    });


    // ========================================
    // 17. CANCELAR EDICIÓN
    // ========================================

    cancelEditButton.addEventListener("click", () => {
        restablecerFormulario();

        mostrarMensaje(
            createProductMessage,
            "Edición cancelada."
        );
    });


    // ========================================
    // 18. EDITAR O ELIMINAR PRODUCTOS
    // ========================================

    productsTableBody.addEventListener("click", async event => {
        const button = event.target.closest("button[data-action]");

        if (!button) return;

        const productId = Number(button.dataset.id);
        const action = button.dataset.action;

        const producto = productosActuales.find(
            item => Number(item.id) === productId
        );

        if (!producto) {
            productsMessage.textContent =
                "No se encontró el producto seleccionado. Actualiza el catálogo.";
            return;
        }

        if (action === "edit") {
            editarProducto(producto);
            return;
        }

        if (action === "delete") {
            const confirmar = window.confirm(
                `¿Estás seguro de eliminar "${producto.nombre}"?\n\n` +
                "Esta operación no se puede deshacer."
            );

            if (!confirmar) return;

            button.disabled = true;
            button.textContent = "Eliminando...";

            try {
                await API.delete(`/products/${productId}`);

                if (
                    productoEnEdicion &&
                    Number(productoEnEdicion.id) === productId
                ) {
                    restablecerFormulario();
                }

                mostrarMensaje(
                    productsMessage,
                    "Producto eliminado correctamente.",
                    "#16a34a"
                );

                await cargarProductos();

            } catch (error) {
                console.error("No se pudo eliminar el producto:", error);

                mostrarMensaje(
                    productsMessage,
                    `No se pudo eliminar el producto. ${error.message}`,
                    "#dc2626"
                );

                button.disabled = false;
                button.textContent = "Eliminar";
            }
        }
    });


    // ========================================
    // 19. ABRIR Y ACTUALIZAR USUARIOS
    // ========================================

    showUsersButton.addEventListener("click", async () => {
        abrirPanel(usersPanel);
        await cargarUsuarios();
    });

    refreshUsersButton.addEventListener("click", cargarUsuarios);

    backToAdminButton.addEventListener(
        "click",
        volverAlPanelPrincipal
    );


    // ========================================
    // 20. ABRIR Y ACTUALIZAR PRODUCTOS
    // ========================================

    showProductsButton.addEventListener("click", async () => {
        abrirPanel(productsPanel);
        await cargarProductos();
    });

    refreshProductsButton.addEventListener(
        "click",
        cargarProductos
    );

    backFromProductsButton.addEventListener("click", () => {
        restablecerFormulario();
        volverAlPanelPrincipal();
    });


    // ========================================
    // 21. ABRIR Y ACTUALIZAR PEDIDOS
    // ========================================

    showOrdersButton.addEventListener("click", async () => {
        abrirPanel(ordersPanel);
        await cargarPedidos();
    });

    refreshOrdersButton.addEventListener(
        "click",
        cargarPedidos
    );

    backFromOrdersButton.addEventListener("click", () => {
        orderDetailPanel.hidden = true;
        volverAlPanelPrincipal();
    });


    // ========================================
    // 22. ABRIR Y ACTUALIZAR PAGOS
    // ========================================

    showPaymentsButton.addEventListener("click", async () => {
        abrirPanel(paymentsPanel);
        await cargarPagos();
    });

    refreshPaymentsButton.addEventListener(
        "click",
        cargarPagos
    );

    backFromPaymentsButton.addEventListener(
        "click",
        volverAlPanelPrincipal
    );

});
