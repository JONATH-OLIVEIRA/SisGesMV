document.addEventListener("DOMContentLoaded", function () {
    carregarPedidos();
});

// 🔹 Buscar pedidos do backend e exibir na tabela
function carregarPedidos() {
    fetch("/pedidos", {
        method: "GET",
        headers: { 
            "Authorization": `Bearer ${localStorage.getItem("token")}`,
            "Content-Type": "application/json"
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Erro ao carregar pedidos");
        }
        return response.json();
    })
    .then(pedidos => {
        const tabelaPedidos = document.getElementById("tabelaPedidos");
        tabelaPedidos.innerHTML = "";

        pedidos.forEach(pedido => {
            const row = `
                <tr>
                    <td>${pedido.id}</td>
                    <td>${pedido.cpfVendedor}</td>
                    <td>R$ ${pedido.valorTotal.toFixed(2)}</td>
                    <td><span class="badge bg-${getBadgeClass(pedido.status)}">${pedido.status}</span></td>
                    <td>${pedido.codigoEntrega || 'N/A'}</td>
                    <td>
                        ${getAcoesPedido(pedido)}
                    </td>
                </tr>
            `;
            tabelaPedidos.innerHTML += row;
        });
    })
    .catch(error => console.error("Erro ao carregar pedidos:", error));
}

// 🔹 Retorna a classe de cor para cada status
function getBadgeClass(status) {
    switch (status) {
        case "PENDENTE": return "warning";
        case "APROVADO": return "primary";
        case "SEPARACAO": return "info";
        case "PRONTO_PARA_ENTREGA": return "secondary";
        case "ENTREGUE": return "success";
        default: return "dark";
    }
}

// 🔹 Gera botões de ação com base no status do pedido
function getAcoesPedido(pedido) {
    let acoes = "";
    if (pedido.status === "PENDENTE") {
        acoes += `<button class="btn btn-sm btn-primary" onclick="aprovarPedido(${pedido.id})">Aprovar</button> `;
        acoes += `<button class="btn btn-sm btn-warning" onclick="editarPedido(${pedido.id})">Editar</button> `;
        acoes += `<button class="btn btn-sm btn-danger" onclick="cancelarPedido(${pedido.id})">Cancelar</button>`;
    }
    if (pedido.status === "APROVADO") {
        acoes += `<button class="btn btn-sm btn-info" onclick="separarPedido(${pedido.id})">Separar</button>`;
    }
    if (pedido.status === "SEPARACAO") {
        acoes += `<button class="btn btn-sm btn-secondary" onclick="marcarProntoParaEntrega(${pedido.id})">Pronto para Entrega</button>`;
    }
    if (pedido.status === "PRONTO_PARA_ENTREGA") {
        acoes += `<button class="btn btn-sm btn-dark" onclick="confirmarEntrega(${pedido.id})">Confirmar Entrega</button>`;
    }
    return acoes;
}

// 🔹 Aprovar pedido (movendo para separação)
function aprovarPedido(id) {
    const cpfGerente = solicitarCPF("Digite seu CPF para aprovar este pedido:");
    if (!cpfGerente) return;

    fetch(`/pedidos/${id}/aprovar`, {
        method: "PUT",
        headers: { 
            "Authorization": `Bearer ${localStorage.getItem("token")}`,
            "Content-Type": "application/json" 
        },
        body: JSON.stringify(cpfGerente)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Falha ao aprovar pedido");
        }
        return response.json();
    })
    .then(pedidoAtualizado => {
        alert(`Pedido ${pedidoAtualizado.id} aprovado com sucesso! Status atualizado para ${pedidoAtualizado.status}`);
        carregarPedidos();
    })
    .catch(error => alert(error.message));
}

// 🔹 Separar pedido (finalizar separação)
function separarPedido(id) {
    const cpfSeparador = solicitarCPF("Digite seu CPF (de gerente/admin) para confirmar a separação:");
    if (!cpfSeparador) return;

    // Sanitiza CPF
    const cpfSanitizado = cpfSeparador.replace(/\D/g, '');
    
    fetch(`/pedidos/${id}/separar`, {
        method: "PUT",
        headers: { 
            "Authorization": `Bearer ${localStorage.getItem("token")}`,
            "Content-Type": "text/plain"  // Envia como texto puro
        },
        body: cpfSanitizado
    })
    .then(async response => {
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || "Falha ao separar pedido");
        }
        return response.json();
    })
    .then(pedidoAtualizado => {
        if (!pedidoAtualizado.cpfSeparador) {
            throw new Error("CPF do separador não foi registrado!");
        }
        console.log("Pedido atualizado:", pedidoAtualizado);
        alert(`✅ Pedido em separação! CPF registrado: ${pedidoAtualizado.cpfSeparador}`);
        carregarPedidos();
    })
    .catch(error => {
        console.error("Erro detalhado:", error);
        alert(`❌ Falha: ${error.message}`);
    });
}

function marcarProntoParaEntrega(id) {
    if (!confirm("Confirmar que o pedido está pronto para entrega?")) return;

    fetch(`/pedidos/${id}/pronto`, {
        method: "PUT",
        headers: { 
            "Authorization": `Bearer ${localStorage.getItem("token")}`,
            "Content-Type": "application/json" 
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Falha ao marcar como pronto para entrega");
        }
        return response.json();
    })
    .then(pedidoAtualizado => {
        alert(`Pedido ${pedidoAtualizado.id} pronto para entrega!`);
        carregarPedidos();
    })
    .catch(error => alert(error.message));
}

// 🔹 Confirmar entrega com código único
function confirmarEntrega(id) {
    console.log(`[confirmarEntrega] Iniciando confirmação para pedido ID: ${id}`);
    
    // Pega o código exato do DOM (se disponível)
    const codigoEsperado = document.querySelector(`tr[data-id="${id}"] .codigo-entrega`)?.textContent.trim();
    const codigoEntrega = prompt(`Digite o código de entrega${codigoEsperado ? ` (${codigoEsperado})` : ''}:`);
    
    if (!codigoEntrega) {
        console.warn('[confirmarEntrega] Usuário cancelou ou código vazio');
        return alert("Operação cancelada!");
    }

    console.log(`[confirmarEntrega] Código inserido: "${codigoEntrega}"`);

    // Sanitização mais cuidadosa (remove TODAS as aspas)
    const codigoSanitizado = codigoEntrega.replace(/['"]/g, '').trim().toUpperCase();
    console.log(`[confirmarEntrega] Código sanitizado: "${codigoSanitizado}"`);

    fetch(`/pedidos/${id}/entregar`, {
        method: "PUT",
        headers: { 
            "Authorization": `Bearer ${localStorage.getItem("token")}`,
            "Content-Type": "text/plain"  // 🔥 Agora é texto puro, não JSON
        },
        body: codigoSanitizado  // 🔥 Envia só o texto, sem aspas adicionais
    })
    .then(async response => {
        console.log(`[confirmarEntrega] Resposta recebida - Status: ${response.status}`);
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            console.error('[confirmarEntrega] Erro detalhado:', errorData);
            throw new Error(errorData?.message || `Erro ${response.status}: ${response.statusText}`);
        }
        return response.json();
    })
    .then(pedidoAtualizado => {
        console.log('[confirmarEntrega] Pedido atualizado:', pedidoAtualizado);
        alert(`✅ Pedido ${pedidoAtualizado.id} entregue com sucesso!`);
        carregarPedidos();
    })
    .catch(error => {
        console.error('[confirmarEntrega] Erro completo:', error);
        
        let mensagemErro = "Erro ao confirmar entrega";
        if (error.message.includes("Código de entrega não corresponde")) {
            mensagemErro = "❌ Código incorreto! Verifique o código e tente novamente.";
        } else if (error.message.includes("PRONTO_PARA_ENTREGA")) {
            mensagemErro = "⚠️ O pedido precisa estar 'Pronto para Entrega' antes de confirmar";
        }
        
        alert(mensagemErro);
    });
}

// 🔹 Cancelar pedido
function cancelarPedido(id) {
    if (!confirm("Tem certeza que deseja cancelar este pedido?")) return;

    fetch(`/pedidos/${id}/cancelar`, {
        method: "DELETE",
        headers: { 
            "Authorization": `Bearer ${localStorage.getItem("token")}`,
            "Content-Type": "application/json" 
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Falha ao cancelar pedido");
        }
        return response.text();
    })
    .then(() => {
        alert("Pedido cancelado com sucesso!");
        carregarPedidos();
    })
    .catch(error => alert(error.message));
}

// 🔹 Editar pedido (abrindo modal de edição)
function editarPedido(id) {
    fetch(`/pedidos/${id}`, {
        headers: { 
            "Authorization": `Bearer ${localStorage.getItem("token")}`,
            "Content-Type": "application/json" 
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Erro ao carregar pedido");
        }
        return response.json();
    })
    .then(pedido => {
        document.getElementById("modalPedidoId").value = pedido.id;
        document.getElementById("modalCpfVendedor").value = pedido.cpfVendedor;
        document.getElementById("modalProdutos").value = pedido.produtos.map(p => p.id).join(", ");
        new bootstrap.Modal(document.getElementById("modalEditarPedido")).show();
    })
    .catch(error => alert(error.message));
}

// 🔹 Salvar edição de pedido
function salvarEdicaoPedido() {
    const id = document.getElementById("modalPedidoId").value;
    const cpfVendedor = document.getElementById("modalCpfVendedor").value;
    const produtosIds = document.getElementById("modalProdutos").value.split(",").map(p => parseInt(p.trim()));

    fetch(`/pedidos/${id}`, {
        method: "PUT",
        headers: { 
            "Authorization": `Bearer ${localStorage.getItem("token")}`,
            "Content-Type": "application/json" 
        },
        body: JSON.stringify({ cpfVendedor, produtos: produtosIds })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Erro ao atualizar pedido");
        }
        return response.json();
    })
    .then(() => {
        alert("Pedido atualizado com sucesso!");
        carregarPedidos();
        bootstrap.Modal.getInstance(document.getElementById("modalEditarPedido")).hide();
    })
    .catch(error => alert(error.message));
}

// 🔹 Validação e solicitação de CPF
function solicitarCPF(mensagem) {
    let cpf = prompt(mensagem);
    if (!cpf) return null;
    
    cpf = cpf.trim().replace(/\D/g, ""); // Remove caracteres não numéricos
    
    if (cpf.length !== 11 && cpf.length !== 14) {
        alert("CPF inválido! Deve conter 11 dígitos.");
        return null;
    }
    return cpf;
}

// 🔹 Filtro de pedidos por status
function filtrarPedidos() {
    const statusSelecionado = document.getElementById("filtroStatus").value;
    
    const pedidos = document.querySelectorAll("#tabelaPedidos tr");
    pedidos.forEach(pedido => {
        const status = pedido.querySelector("td:nth-child(4) span").textContent.trim();
        pedido.style.display = (statusSelecionado === "TODOS" || statusSelecionado === status) ? "" : "none";
    });
}