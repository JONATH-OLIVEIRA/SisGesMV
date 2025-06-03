// Função para extrair email do token JWT
function getEmailFromToken() {
    const token = localStorage.getItem("token");
    if (!token) return null;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.sub || payload.email;
    } catch (e) {
        console.error("Erro ao decodificar token:", e);
        return null;
    }
}

// Estado global
let emailUsuario = "";
let pedidosStatusChart = null;
let ultimaAtualizacao = null;
let pedidoAtualId = null;

// Função para mostrar notificação de sucesso
function mostrarSucesso(mensagem) {
    Toastify({
        text: mensagem,
        duration: 3000,
        close: true,
        gravity: "top",
        position: "right",
        backgroundColor: "linear-gradient(to right, #00b09b, #96c93d)",
        stopOnFocus: true
    }).showToast();
}

// Função para mostrar erro
function mostrarErro(titulo, mensagem, redirecionarLogin = false) {
    console.error(`${titulo}: ${mensagem}`);
    
    Swal.fire({
        icon: 'error',
        title: titulo,
        text: mensagem,
        confirmButtonText: redirecionarLogin ? "Ir para o login" : "OK"
    }).then(() => {
        if (redirecionarLogin) {
            localStorage.removeItem("token");
            window.location.href = "/auth/login";
        }
    });
}

// Função auxiliar para verificar se elemento existe antes de manipular
function safeSetTextContent(elementId, text) {
    const element = document.getElementById(elementId);
    if (element) element.textContent = text;
}

async function carregarDadosUsuario() {
    try {
        const userEmail = getEmailFromToken();
        if (!userEmail) {
            throw new Error("Sessão inválida");
        }
        emailUsuario = userEmail;
        await carregarDashboardData();
    } catch (error) {
        mostrarErro("Erro ao carregar usuário", error.message, true);
    }
}

async function carregarDashboardData(forcarAtualizacao = false) {
    if (!emailUsuario) {
        console.error("Email do usuário não disponível");
        return;
    }

    try {
        // Adiciona timestamp e forcarAtualizacao como parâmetros
        const url = `/api/user/dashboard/data?timestamp=${new Date().getTime()}&forcarAtualizacao=${forcarAtualizacao}`;
        
        const response = await fetch(url, {
            headers: {
                "Authorization": `Bearer ${localStorage.getItem("token")}`,
                "Content-Type": "application/json"
            },
            cache: 'no-store'
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Erro ${response.status}`);
        }

        const data = await response.json();
        ultimaAtualizacao = new Date();
        atualizarUI(data);

    } catch (error) {
        handleDashboardError(error);
    }
}

function atualizarUI(data) {
    try {
        // Verifica se os dados são válidos
        if (!data || typeof data !== 'object') {
            throw new Error('Dados recebidos são inválidos');
        }

        // Atualiza informações do usuário de forma segura
        safeSetTextContent("nomeUsuario", data.nomeUsuario || "");
        safeSetTextContent("nomeCompleto", data.nomeUsuario || "");
        safeSetTextContent("nomeUsuarioNav", data.nomeUsuario || "");
        safeSetTextContent("cargoUsuario", data.cargoUsuario || data.tipo || "Cargo não informado");

        // Atualiza contadores
        safeSetTextContent("totalPedidos", data.totalPedidos ?? 0);
        safeSetTextContent("pedidosEntregues", data.pedidosEntregues ?? 0);
        safeSetTextContent("pedidosPendentes", data.pedidosPendentes ?? 0);

        // Atualiza gráficos
        if (data.pedidosPorStatus) {
            atualizarGraficoPedidosStatus(data.pedidosPorStatus);
        }

        // Atualiza tabela de pedidos
        if (data.ultimosPedidos && Array.isArray(data.ultimosPedidos)) {
            preencherTabelaPedidos(data.ultimosPedidos);
        } else {
            const tabela = document.getElementById('tabelaPedidos');
            if (tabela) {
                tabela.innerHTML = '<tr><td colspan="6" class="text-center">Nenhum pedido encontrado</td></tr>';
            }
        }

        // Atualiza o horário da última atualização
        if (ultimaAtualizacao) {
            safeSetTextContent('ultimaAtualizacao', `Última atualização: ${ultimaAtualizacao.toLocaleTimeString('pt-BR')}`);
        }

    } catch (error) {
        console.error('Erro ao atualizar UI:', error);
        handleDashboardError(error);
    }
}

function atualizarGraficoPedidosStatus(dadosStatus) {
    const canvas = document.getElementById('pedidosStatusChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');

    // Destrói o gráfico existente
    if (pedidosStatusChart) {
        pedidosStatusChart.destroy();
    }

    // Cores para cada status
    const cores = {
        'PENDENTE': 'rgba(255, 193, 7, 0.7)',
        'APROVADO': 'rgba(23, 162, 184, 0.7)',
        'SEPARACAO': 'rgba(0, 123, 255, 0.7)',
        'PRONTO_PARA_ENTREGA': 'rgba(108, 117, 125, 0.7)',
        'ENTREGUE': 'rgba(40, 167, 69, 0.7)',
        'CANCELADO': 'rgba(220, 53, 69, 0.7)'
    };

    const labels = Object.keys(dadosStatus || {});
    const data = labels.map(label => dadosStatus[label]);
    const backgroundColors = labels.map(label => cores[label] || 'rgba(128, 128, 128, 0.7)');

    if (labels.length > 0) {
        pedidosStatusChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: backgroundColors,
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        position: 'bottom',
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return `${context.label}: ${context.raw} pedidos`;
                            }
                        }
                    }
                },
                animation: {
                    duration: pedidosStatusChart ? 0 : 1000
                }
            }
        });
    } else {
        ctx.font = '16px Arial';
        ctx.fillStyle = '#666';
        ctx.textAlign = 'center';
        ctx.fillText('Nenhum dado disponível', canvas.width / 2, canvas.height / 2);
    }
}

function formatarData(dataPedido) {
    if (!dataPedido) return "Data Inválida";
    
    try {
        // Se a data vier como 'YYYY-MM-DD' do banco
        if (typeof dataPedido === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(dataPedido)) {
            // Adiciona o timezone para evitar a conversão
            const data = new Date(dataPedido + 'T00:00:00-03:00');
            return data.toLocaleDateString('pt-BR', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
            });
        }
        
        // Se já vier como objeto Date ou timestamp
        const data = new Date(dataPedido);
        return data.toLocaleDateString('pt-BR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            timeZone: 'America/Sao_Paulo'
        });
    } catch (error) {
        console.error('Erro ao formatar data:', error);
        return "Data Inválida";
    }
}


function preencherTabelaPedidos(pedidos) {
    const tabela = document.getElementById('tabelaPedidos');
    if (!tabela) return;

    if (!pedidos || !Array.isArray(pedidos) || pedidos.length === 0) {
        tabela.innerHTML = '<tr><td colspan="6" class="text-center">Nenhum pedido encontrado</td></tr>';
        return;
    }

    // Ordena pedidos por data mais recente primeiro
    pedidos.sort((a, b) => new Date(b.dataPedido) - new Date(a.dataPedido));

    tabela.innerHTML = pedidos.map(pedido => {
        const dataFormatada = formatarData(pedido.dataPedido);
        const status = pedido.status || 'PENDENTE';
        
        const cores = {
            'PENDENTE': 'bg-warning',
            'APROVADO': 'bg-info',
            'SEPARACAO': 'bg-primary',
            'PRONTO_PARA_ENTREGA': 'bg-secondary',
            'ENTREGUE': 'bg-success',
            'CANCELADO': 'bg-danger'
        };

        return `
            <tr>
                <td>${pedido.id || 'N/A'}</td>
                <td>${dataFormatada}</td>
                <td>R$ ${pedido.valorTotal?.toFixed(2) || '0,00'}</td>
                <td><span class="badge ${cores[status] || 'bg-secondary'}">${status}</span></td>
                <td>${pedido.codigoEntrega || 'N/A'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="detalhesPedido(${pedido.id || '0'})">
                        <i class="fas fa-eye"></i> Detalhes
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function handleDashboardError(error) {
    console.error("Erro no dashboard:", error);

    // Limpa os gráficos
    const ctx = document.getElementById('pedidosStatusChart')?.getContext('2d');
    if (ctx) {
        ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
        ctx.fillStyle = '#666';
        ctx.textAlign = 'center';
        ctx.fillText("Dados indisponíveis", ctx.canvas.width / 2, ctx.canvas.height / 2);
    }

    const isAuthError = error.message.includes("Sessão expirada") || 
                      error.message.includes("Sessão inválida") || 
                      error.message.includes("401");

    mostrarErro("Erro no Dashboard", error.message, isAuthError);
}

function logout() {
    localStorage.removeItem("token");
    window.location.href = "/auth/login";
}

// Função para forçar atualização completa
function atualizarPedidos() {
    // Mostra feedback visual que está atualizando
    const btnAtualizar = document.getElementById('btnAtualizar');
    if (btnAtualizar) {
        btnAtualizar.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Atualizando';
        btnAtualizar.disabled = true;
    }

    // Força a atualização do gráfico
    if (pedidosStatusChart) {
        pedidosStatusChart.destroy();
        pedidosStatusChart = null;
    }
    
    // Carrega os dados com força de atualização
    carregarDashboardData(true).finally(() => {
        if (btnAtualizar) {
            btnAtualizar.innerHTML = '<i class="fas fa-sync-alt"></i> Atualizar';
            btnAtualizar.disabled = false;
        }
        mostrarSucesso("Dados atualizados com sucesso!");
    });
}

// Função para exibir detalhes do pedido
async function detalhesPedido(id) {
    pedidoAtualId = id;
    try {
        const response = await fetch(`/pedidos/${id}/detalhes?timestamp=${new Date().getTime()}`, {
            headers: {
                "Authorization": `Bearer ${localStorage.getItem("token")}`,
                "Content-Type": "application/json"
            },
            cache: 'no-store'
        });

        if (!response.ok) {
            throw new Error(`Erro ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();
        preencherModalDetalhes(data);

        // Mostra o modal
        const modal = new bootstrap.Modal(document.getElementById('detalhesPedidoModal'));
        modal.show();

    } catch (error) {
        console.error("Erro ao buscar detalhes do pedido:", error);
        mostrarErro("Erro ao carregar detalhes", error.message);
    }
}

function preencherModalDetalhes(pedido) {
    // Preenche informações básicas
    safeSetTextContent('pedidoId', pedido.id || 'N/A');
    safeSetTextContent('pedidoVendedor', `${pedido.vendedor || 'N/A'} (${pedido.cpfVendedor || 'N/A'})`);
    safeSetTextContent('pedidoData', formatarData(pedido.dataPedido));

    // Status com badge colorido
    const statusElement = document.getElementById('pedidoStatus');
    if (statusElement) {
        statusElement.textContent = pedido.status || 'PENDENTE';
        statusElement.className = 'badge ' + getStatusBadgeClass(pedido.status);
    }

    // Preenche itens do pedido
    const itensTbody = document.getElementById('pedidoItens');
    if (itensTbody && pedido.itens && Array.isArray(pedido.itens)) {
        itensTbody.innerHTML = pedido.itens.map(item => `
            <tr>
                <td>${item.produto || 'N/A'}</td>
                <td class="text-end">${item.quantidade || 0}</td>
                <td class="text-end">R$ ${item.precoUnitario?.toFixed(2) || '0,00'}</td>
                <td class="text-end">R$ ${item.subtotal?.toFixed(2) || '0,00'}</td>
            </tr>
        `).join('');
    }

    // Preenche total
    safeSetTextContent('pedidoTotal', `R$ ${pedido.valorTotal?.toFixed(2) || '0,00'}`);
}

function baixarRelatorioPedido() {
    if (!pedidoAtualId) return;
    window.open(`/pedidos/${pedidoAtualId}/relatorio?timestamp=${new Date().getTime()}`, '_blank');
}

function getStatusBadgeClass(status) {
    const classes = {
        'PENDENTE': 'bg-warning',
        'APROVADO': 'bg-info',
        'SEPARACAO': 'bg-primary',
        'PRONTO_PARA_ENTREGA': 'bg-secondary',
        'ENTREGUE': 'bg-success',
        'CANCELADO': 'bg-danger'
    };
    return classes[status] || 'bg-secondary';
}

function atualizarDataAtual() {
    safeSetTextContent('dataAtual', new Date().toLocaleDateString('pt-BR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    }));
}

// Inicialização
document.addEventListener('DOMContentLoaded', () => {
    if (!localStorage.getItem("token")) {
        window.location.href = "/auth/login";
    } else {
        atualizarDataAtual();
        carregarDadosUsuario();
        
        // Adiciona o evento de clique ao botão de atualização
        document.getElementById('btnAtualizar')?.addEventListener('click', atualizarPedidos);
    }
});