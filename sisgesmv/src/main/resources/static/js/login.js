function getUserIdFromToken(token) {
	try {
		const payload = JSON.parse(atob(token.split('.')[1])); // 🔹 Decodifica o payload do JWT
		return payload.id; // 🔹 Retorna o ID do usuário
	} catch (error) {
		console.error("❌ Erro ao decodificar o token:", error);
		return null;
	}
}

document.getElementById('loginForm').addEventListener('submit', async (e) => {
	e.preventDefault();

	try {
		const response = await fetch('/auth/login', {
			method: 'POST',
			credentials: 'include',
			headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
			body: new URLSearchParams({
				email: document.getElementById('email').value,
				senha: document.getElementById('senha').value
			})
		});

		const data = await response.json();

		console.log("Resposta completa do servidor:", data);

		if (response.status === 200) {
			localStorage.setItem("token", data.token);

			const usuarioId = getUserIdFromToken(data.token); // 🔹 Extraímos o ID do token JWT
			if (usuarioId) {
				localStorage.setItem("usuarioId", usuarioId); // 🔹 Agora `usuarioId` está salvo corretamente!
			} else {
				console.error("❌ Erro: ID do usuário não foi encontrado no token.");
			}

			console.log("Token:", data.token);
			console.log("UserID:", usuarioId);
			console.log("Redirecionando para:", data.redirect);

			window.location.href = data.redirect;
		} else {
			console.error("❌ Erro no login:", data);
			window.location.href = '/auth/login?error=' + encodeURIComponent(data.error || 'Erro desconhecido');
		}
	} catch (error) {
		console.error("❌ Erro na requisição:", error);
		window.location.href = '/auth/login?error=' + encodeURIComponent('Erro na conexão com o servidor');
	}
});
// Adicionando evento ao botão de logout
document.getElementById("logoutBtn").addEventListener("click", function() {
	localStorage.removeItem("token"); // Remove o token de autenticação
	window.location.href = "/"; // Redireciona para a página inicial
});

