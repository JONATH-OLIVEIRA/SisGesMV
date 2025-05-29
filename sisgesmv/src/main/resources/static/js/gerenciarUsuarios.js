document.addEventListener("DOMContentLoaded", () => {
    const token = localStorage.getItem("token");
    if (!token) {
        window.location.href = "/login";
        return;
    }

    const tabela = document.getElementById("tabelaUsuarios");
    const form = document.getElementById("cadastroUsuarioForm");
    const btnCancelar = document.getElementById("btnCancelar");
    const btnSalvar = document.getElementById("btnSalvar");

    let editandoId = null;

    const apiHeaders = {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
    };

    // Função para formatar data (para exibição na tabela)
    const formatarDataExibicao = (dataString) => {
        if (!dataString) return 'N/A';
        try {
            const data = new Date(dataString);
            return isNaN(data) ? dataString : data.toLocaleDateString('pt-BR');
        } catch (e) {
            return dataString;
        }
    };

    // Função para formatar data para o input (formato YYYY-MM-DD)
    const formatarDataInput = (dataString) => {
        if (!dataString) return '';
        try {
            const data = new Date(dataString);
            if (isNaN(data)) return '';
            return data.toISOString().split('T')[0];
        } catch (e) {
            return '';
        }
    };

    // 🔹 Função para carregar usuários
    function carregarUsuarios() {
        fetch("/usuarios/todos", { headers: apiHeaders })
            .then(res => {
                if (!res.ok) {
                    throw new Error("Erro ao carregar usuários");
                }
                return res.json();
            })
            .then(usuarios => {
                tabela.innerHTML = ''; // Limpa a tabela
                
                if (usuarios.length === 0) {
                    tabela.innerHTML = `
                        <tr><td colspan="6" class="text-center">Nenhum usuário cadastrado</td></tr>`;
                    return;
                }

                usuarios.forEach(u => {
                    const row = document.createElement("tr");
                    row.dataset.id = u.id;
                    row.innerHTML = `
                        <td>${u.nome || 'N/A'}</td>
                        <td>${u.email || 'N/A'}</td>
                        <td>${u.cpf || 'N/A'}</td>
                        <td>${formatarDataExibicao(u.dtNascimento)}</td>
                        <td>${u.tipo || 'N/A'}</td>
                        <td>
                            <button class="btn btn-warning btn-sm me-2" onclick="editarUsuario('${u.id}')">Editar</button>
                            <button class="btn btn-danger btn-sm" onclick="excluirUsuario('${u.id}')">Excluir</button>
                        </td>`;
                    tabela.appendChild(row);
                });
            })
            .catch(err => {
                console.error("Erro ao carregar usuários:", err);
                tabela.innerHTML = `
                    <tr><td colspan="6" class="text-center text-danger">Erro ao carregar usuários</td></tr>`;
            });
    }

    // 🔹 Função para resetar formulário
    const resetarForm = () => {
        form.reset();
        editandoId = null;
        btnSalvar.textContent = "Cadastrar";
    };

    // 🔸 Função para salvar usuário (cadastrar/atualizar)
    form.addEventListener("submit", e => {
        e.preventDefault();

        const usuario = {
            nome: form.nome.value.trim(),
            email: form.email.value.trim(),
            cpf: form.cpf.value.trim(),
            dtNascimento: form.dtNascimento.value,
            tipo: form.tipoUsuario.value
        };

        // Só inclui senha se for um novo usuário
        if (!editandoId && form.senha.value) {
            usuario.senha = form.senha.value.trim();
        }

        const url = editandoId ? `/usuarios/${editandoId}` : "/usuarios/cadastrar";
        const metodo = editandoId ? "PUT" : "POST";

        fetch(url, {
            method: metodo,
            headers: apiHeaders,
            body: JSON.stringify(usuario)
        })
            .then(async res => {
                if (!res.ok) {
                    const errorData = await res.json().catch(() => ({}));
                    throw new Error(errorData.message || "Erro ao salvar usuário");
                }
                return res.json();
            })
            .then(() => {
                alert(editandoId ? "Usuário atualizado com sucesso!" : "Usuário cadastrado com sucesso!");
                resetarForm();
                carregarUsuarios();
            })
            .catch(err => {
                console.error("Erro ao salvar usuário:", err);
                alert(err.message || "Erro ao salvar usuário");
            });
    });

    btnCancelar.addEventListener("click", () => {
        resetarForm();
    });

    // 🔸 Função para excluir usuário
    window.excluirUsuario = function(id) {
        if (confirm("Deseja realmente excluir este usuário?")) {
            fetch(`/usuarios/${id}`, {
                method: "DELETE",
                headers: apiHeaders
            })
                .then(async res => {
                    if (!res.ok) {
                        const errorData = await res.json().catch(() => ({}));
                        throw new Error(errorData.message || "Erro ao excluir usuário");
                    }
                    return res.text();
                })
                .then(() => {
                    alert("Usuário excluído com sucesso!");
                    carregarUsuarios();
                })
                .catch(err => {
                    console.error("Erro ao excluir usuário:", err);
                    alert(err.message || "Erro ao excluir usuário");
                });
        }
    };

    // 🔸 Função para editar usuário
    window.editarUsuario = function(id) {
        fetch(`/usuarios/${id}`, {
            method: "GET",
            headers: apiHeaders
        })
            .then(async res => {
                if (!res.ok) {
                    const errorData = await res.json().catch(() => ({}));
                    throw new Error(errorData.message || "Erro ao buscar usuário");
                }
                return res.json();
            })
            .then(usuario => {
                form.nome.value = usuario.nome || '';
                form.email.value = usuario.email || '';
                form.cpf.value = usuario.cpf || '';
                form.dtNascimento.value = formatarDataInput(usuario.dtNascimento);
                form.tipoUsuario.value = usuario.tipo || '';
                form.senha.value = ''; // Limpa o campo de senha

                editandoId = id;
                btnSalvar.textContent = "Atualizar";
                window.scrollTo({ top: 0, behavior: "smooth" });
            })
            .catch(err => {
                console.error("Erro ao carregar usuário:", err);
                alert(err.message || "Erro ao carregar dados do usuário");
            });
    };

    // Inicializa a tabela
    carregarUsuarios();
});