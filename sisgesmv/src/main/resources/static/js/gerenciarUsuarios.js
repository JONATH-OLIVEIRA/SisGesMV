document.addEventListener("DOMContentLoaded", function () {
    const token = localStorage.getItem("token");

    if (!token) {
        window.location.href = "/login";
        return;
    }

    function formatarData(dataString) {
        if (!dataString) return 'N/A';
        try {
            const data = new Date(dataString);
            return data.toLocaleDateString('pt-BR');
        } catch (e) {
            console.error("Erro ao formatar data:", e);
            return dataString;
        }
    }

    function carregarUsuarios() {
        fetch("/usuarios/todos", {
            method: "GET",
            headers: { 
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            }
        })
        .then(response => {
            if (!response.ok) throw new Error("Erro ao carregar usuários");
            return response.json();
        })
        .then(usuarios => {
            const tabela = document.getElementById("tabelaUsuarios");
            tabela.innerHTML = '';

            if (!usuarios || usuarios.length === 0) {
                tabela.innerHTML = `
                    <tr>
                        <td colspan="6" class="text-center">Nenhum usuário cadastrado</td>
                    </tr>
                `;
                return;
            }

            usuarios.forEach(usuario => {
                const row = document.createElement('tr');
                row.setAttribute('data-id', usuario.id);
                row.innerHTML = `
                    <td>${usuario.nome || 'N/A'}</td>
                    <td>${usuario.email || 'N/A'}</td>
                    <td>${usuario.cpf || 'N/A'}</td>
                    <td>${formatarData(usuario.dtNascimento)}</td>
                    <td>${usuario.tipo || 'N/A'}</td>
                    <td>
                        <button class="btn btn-warning btn-sm me-2 btn-editar">Editar</button>
                        <button class="btn btn-danger btn-sm btn-excluir">Excluir</button>
                    </td>
                `;
                tabela.appendChild(row);
            });
        })
        .catch(error => {
            console.error("Erro:", error);
            document.getElementById("tabelaUsuarios").innerHTML = `
                <tr>
                    <td colspan="6" class="text-center text-danger">Erro ao carregar usuários</td>
                </tr>
            `;
        });
    }

    document.getElementById("cadastroUsuarioForm").addEventListener("submit", function(e) {
        e.preventDefault();

        const novoUsuario = {
            nome: document.getElementById("nome").value,
            email: document.getElementById("email").value,
            cpf: document.getElementById("cpf").value,
            dtNascimento: document.getElementById("dtNascimento").value,
            senha: document.getElementById("senha").value,
            tipo: document.getElementById("tipoUsuario").value
        };

        fetch("/usuarios/cadastrar", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify(novoUsuario)
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(err => {
                    throw new Error(err.message || "Erro ao cadastrar usuário");
                });
            }
            return response.json();
        })
        .then(() => {
            alert("Usuário cadastrado com sucesso!");
            document.getElementById("cadastroUsuarioForm").reset();
            carregarUsuarios();
        })
        .catch(error => {
            console.error("Erro:", error);
            alert(error.message);
        });
    });

    document.getElementById("tabelaUsuarios").addEventListener("click", function(e) {
        const row = e.target.closest('tr');
        if (!row) return;

        const id = row.dataset.id;

        if (!id || isNaN(id)) {
            console.error("ID inválido:", id);
            alert("Erro: ID do usuário inválido");
            return;
        }

        // Exclusão
        if (e.target.classList.contains('btn-excluir')) {
            if (confirm("Tem certeza que deseja excluir este usuário?")) {
                fetch(`/usuarios/${id}`, {
                    method: "DELETE",
                    headers: { 
                        "Authorization": `Bearer ${token}`
                    }
                })
                .then(response => {
                    if (!response.ok) throw new Error("Erro ao excluir usuário");
                    return response.text();
                })
                .then(() => {
                    alert("Usuário excluído com sucesso!");
                    carregarUsuarios();
                })
                .catch(error => {
                    console.error("Erro:", error);
                    alert(error.message || "Erro ao excluir usuário");
                });
            }
        }

        // Edição
        if (e.target.classList.contains('btn-editar')) {
            const nome = prompt("Digite o novo nome:", row.children[0].textContent);
            const email = prompt("Digite o novo email:", row.children[1].textContent);
            const cpf = prompt("Digite o novo CPF (somente números):", row.children[2].textContent);
            const dtNascimento = prompt("Digite a nova data de nascimento (AAAA-MM-DD):", new Date(row.children[3].textContent.split('/').reverse().join('-')).toISOString().split('T')[0]);
            const tipo = prompt("Digite o tipo (USER ou ADMIN):", row.children[4].textContent);

            if (!nome || !email || !cpf || !dtNascimento || !tipo) {
                alert("Todos os campos são obrigatórios.");
                return;
            }

            const usuarioAtualizado = {
                nome,
                email,
                cpf,
                dtNascimento,
                tipo
            };

            fetch(`/usuarios/${id}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify(usuarioAtualizado)
            })
            .then(response => {
                if (!response.ok) throw new Error("Erro ao atualizar usuário");
                return response.json();
            })
            .then(() => {
                alert("Usuário atualizado com sucesso!");
                carregarUsuarios();
            })
            .catch(error => {
                console.error("Erro:", error);
                alert(error.message || "Erro ao atualizar usuário");
            });
        }
    });

    carregarUsuarios();
});
