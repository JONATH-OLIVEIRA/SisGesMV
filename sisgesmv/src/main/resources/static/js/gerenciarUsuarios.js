document.addEventListener("DOMContentLoaded", function () {
    const token = localStorage.getItem("token");

    if (!token) {
        window.location.href = "/login";
        return;
    }

    // Função para formatar data
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

    // Função para carregar usuários
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
	console.log("Usuários carregados:", usuarios);
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

    // Cadastro de novo usuário
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
                "Accept": "application/json",
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
        .then(data => {
            alert("Usuário cadastrado com sucesso!");
            document.getElementById("cadastroUsuarioForm").reset();
            carregarUsuarios(); // Atualiza a lista
        })
        .catch(error => {
            console.error("Erro:", error);
            alert(error.message);
        });
    });

    // Event delegation para ações (excluir/editar)
    document.getElementById("tabelaUsuarios").addEventListener("click", function(e) {
        const row = e.target.closest('tr');
        if (!row) return;
        
        const id = row.dataset.id;
        console.log("ID do usuário para ação:", id); // Para depuração
        
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
                        "Authorization": `Bearer ${token}`,
                        "Content-Type": "application/json"
                    }
                })
                .then(response => {
                    if (!response.ok) throw new Error("Erro ao excluir usuário");
                   return response.text();
                })
                .then(data => {
                    alert(data.message || "Usuário excluído com sucesso!");
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
            alert("Funcionalidade de edição será implementada aqui");
            // window.location.href = `/editar-usuario?id=${id}`;
        }
    });

    // Carrega os usuários ao iniciar
    carregarUsuarios();
});