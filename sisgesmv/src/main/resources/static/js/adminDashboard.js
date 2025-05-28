document.addEventListener("DOMContentLoaded", function () {
    const token = localStorage.getItem("token");

    if (!token) {
        window.location.href = "/auth/login"; // Redireciona para login se não estiver autenticado
        return;
    }

    fetch("/api/usuario-logado", {
        method: "GET",
        headers: { "Authorization": `Bearer ${token}` }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Erro ao obter dados do usuário.");
        }
        return response.json();
    })
    .then(data => {
        const nomeUsuarioElem = document.getElementById("nome");
        if (nomeUsuarioElem) {
            nomeUsuarioElem.innerText = data.nome;
        }
    })
    .catch(error => {
        console.error("Erro ao buscar dados do usuário:", error);
        alert("Não foi possível obter as informações do usuário. Faça login novamente.");
        localStorage.removeItem("token");
        window.location.href = "/auth/login";
    });

    // 🔸 Evento no botão de logout
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", function() {
            localStorage.removeItem("token");
            window.location.href = "/";
        });
    }

    // 🔸 Evento no "Admin Dashboard"
    const adminDashboardLink = document.querySelector(".navbar-brand");
    if (adminDashboardLink) {
        adminDashboardLink.addEventListener("click", function(event) {
            event.preventDefault(); // Impede comportamento padrão
            window.location.href = "/admin/dashboard"; // Redireciona corretamente
        });
    }
});
