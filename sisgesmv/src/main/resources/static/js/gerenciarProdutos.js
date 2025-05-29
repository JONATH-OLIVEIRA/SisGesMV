document.addEventListener("DOMContentLoaded", function() {
    const token = localStorage.getItem("token");
    let produtoEditandoId = null; // 🔸 Controla se está editando algum produto

    if (!token) {
        window.location.href = "/login";
        return;
    }

    carregarProdutos();

    const form = document.getElementById("cadastroProdutoForm");
    const botao = form.querySelector("button[type='submit']");

    form.addEventListener("submit", function(e) {
        e.preventDefault();

        const produto = {
            nome: document.getElementById("nome").value,
            descricao: document.getElementById("descricao").value,
            precoCompra: parseFloat(document.getElementById("precoCompra").value),
            precoVenda: parseFloat(document.getElementById("precoVenda").value),
            quantidadeEstoque: parseInt(document.getElementById("quantidadeEstoque").value),
            categoria: document.getElementById("categoria").value
        };

        if (produtoEditandoId) {
            // 🔸 Atualizar Produto
            fetch(`/produtos/${produtoEditandoId}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify(produto)
            })
            .then(response => {
                if (response.ok) {
                    alert("Produto atualizado com sucesso!");
                    resetarFormulario();
                    carregarProdutos();
                } else {
                    alert("Erro ao atualizar produto.");
                }
            })
            .catch(error => {
                console.error("Erro ao atualizar produto:", error);
                alert("Erro ao atualizar produto.");
            });
        } else {
            // 🔸 Cadastrar Produto
            fetch("/produtos", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify(produto)
            })
            .then(response => {
                if (response.ok) {
                    alert("Produto cadastrado com sucesso!");
                    resetarFormulario();
                    carregarProdutos();
                } else {
                    alert("Erro ao cadastrar produto.");
                }
            })
            .catch(error => {
                console.error("Erro ao cadastrar produto:", error);
                alert("Erro ao cadastrar produto.");
            });
        }
    });

    // 🔹 Função para carregar produtos
    function carregarProdutos() {
        fetch("/produtos", {
            method: "GET",
            headers: { "Authorization": `Bearer ${token}` }
        })
        .then(response => response.json())
        .then(produtos => {
            const tabelaProdutos = document.getElementById("tabelaProdutos");
            tabelaProdutos.innerHTML = "";

            produtos.forEach(produto => {
                const row = `
                    <tr>
                        <td>${produto.nome}</td>
                        <td>${produto.descricao}</td>
                        <td>R$ ${produto.precoVenda.toFixed(2)}</td>
                        <td>${produto.quantidadeEstoque}</td>
                        <td>${produto.categoria}</td>
                        <td>
                            <button class="btn btn-warning btn-sm" onclick="editarProduto(${produto.id})">Editar</button>
                            <button class="btn btn-danger btn-sm" onclick="excluirProduto(${produto.id})">Excluir</button>
                        </td>
                    </tr>
                `;
                tabelaProdutos.innerHTML += row;
            });
        })
        .catch(error => console.error("Erro ao carregar produtos:", error));
    }

    // 🔹 Função para resetar formulário
    function resetarFormulario() {
        form.reset();
        botao.textContent = "Cadastrar Produto";
        produtoEditandoId = null;
    }

    // 🔸 Função para excluir produto
    window.excluirProduto = function(id) {
        if (confirm("Tem certeza que deseja excluir este produto?")) {
            fetch(`/produtos/${id}`, {
                method: "DELETE",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            })
            .then(response => {
                if (response.ok) {
                    alert("Produto excluído com sucesso!");
                    carregarProdutos();
                } else {
                    alert("Erro ao excluir produto.");
                }
            })
            .catch(error => {
                console.error("Erro ao excluir produto:", error);
                alert("Erro ao excluir produto.");
            });
        }
    };

    // 🔸 Função para editar produto
    window.editarProduto = function(id) {
        fetch(`/produtos/${id}`, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        })
        .then(response => response.json())
        .then(produto => {
            document.getElementById("nome").value = produto.nome;
            document.getElementById("descricao").value = produto.descricao;
            document.getElementById("precoCompra").value = produto.precoCompra;
            document.getElementById("precoVenda").value = produto.precoVenda;
            document.getElementById("quantidadeEstoque").value = produto.quantidadeEstoque;
            document.getElementById("categoria").value = produto.categoria;

            botao.textContent = "Atualizar Produto";
            produtoEditandoId = id;
        })
        .catch(error => {
            console.error("Erro ao buscar produto:", error);
            alert("Erro ao carregar dados do produto.");
        });
    };
});
