function getUserIdFromToken(token) {
	try {
		const payload = token.split('.')[1];
		const decoded = atob(payload);
		return JSON.parse(decoded).id;
	} catch (e) {
		console.error("Erro ao decodificar token:", e);
		return null;
	}
}

document.addEventListener("DOMContentLoaded", function() {
	const token = localStorage.getItem("token");
	if (!token) {
		window.location.href = "/auth/login";
		return;
	}

	const userId = getUserIdFromToken(token);
	if (!userId) {
		window.location.href = "/auth/login";
		return;
	}

	window.userId = userId;
	window.token = token;

	fetch(`/usuarios/${userId}`, {
		method: "GET",
		headers: {
			"Authorization": `Bearer ${token}`,
			"Content-Type": "application/json"
		}
	})
		.then(handleResponse)
		.then(data => {
			document.getElementById("nome").value = data.nome || "";
			document.getElementById("email").value = data.email || "";
			document.getElementById("cpf").value = data.cpf || "";

			if (data.dtNascimento) {
				const date = new Date(data.dtNascimento);
				document.getElementById("dtNascimento").value = date.toISOString().split('T')[0];
			}

			document.getElementById("tipoUsuario").value = data.tipo || "USER";
		})
		.catch(error => {
			console.error("Erro:", error);
			alert(error.message || "Erro ao carregar perfil.");
			window.location.href = "/auth/login";
		});
});

// ✅ Atualizar perfil
document.getElementById("perfilForm").addEventListener("submit", function(e) {
	e.preventDefault();

	const dadosAtualizados = {
		nome: document.getElementById("nome").value,
		email: document.getElementById("email").value,  // 🔥 Adicionar isso
		cpf: document.getElementById("cpf").value,
		dtNascimento: document.getElementById("dtNascimento").value,
		tipo: document.getElementById("tipoUsuario").value
	};

	const senha = document.getElementById("senha").value;
	if (senha) {
		dadosAtualizados.senha = senha;
	}

	fetch(`/usuarios/${window.userId}`, {
		method: "PUT",
		headers: {
			"Content-Type": "application/json",
			"Authorization": `Bearer ${window.token}`
		},
		body: JSON.stringify(dadosAtualizados)
	})
		.then(handleResponse)
		.then(() => alert("Perfil atualizado com sucesso!"))
		.catch(handleError);
});

// ✅ Excluir conta
document.getElementById("excluirContaBtn").addEventListener("click", function() {
	if (confirm("Tem certeza que deseja excluir sua conta permanentemente?")) {
		fetch(`/usuarios/${window.userId}`, {
			method: "DELETE",
			headers: {
				"Authorization": `Bearer ${window.token}`
			}
		})
			.then(response => {
				if (!response.ok) {
					return response.text().then(text => { throw new Error(text); });
				}
				localStorage.removeItem("token");
				alert("Conta excluída com sucesso!");
				window.location.href = "/auth/login";
			})
			.catch(error => {
				console.error("Erro:", error);
				alert(error.message || "Erro ao excluir conta.");
			});
	}
});

// 🔧 Handler robusto
function handleResponse(response) {
	const contentType = response.headers.get("content-type");
	if (!response.ok) {
		if (contentType && contentType.includes("application/json")) {
			return response.json().then(err => {
				throw new Error(err.message || "Erro desconhecido.");
			});
		} else {
			return response.text().then(text => {
				throw new Error(text || "Erro desconhecido.");
			});
		}
	}
	if (contentType && contentType.includes("application/json")) {
		return response.json();
	}
	return response.text();
}

function handleError(error) {
	console.error("Erro:", error);
	alert(error.message || "Erro ao realizar operação.");
}


// Script para botão voltar
document.getElementById('voltarBtn').addEventListener('click', function() {
	window.location.href = '/admin/dashboard';
});
