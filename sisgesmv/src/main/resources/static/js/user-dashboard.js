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

async function carregarDadosUsuario() {
	try {
		const userEmail = getEmailFromToken();
		if (!userEmail) {
			throw new Error("Sessão inválida");
		}
		emailUsuario = userEmail;
		await carregarDashboardData();
	} catch (error) {
		console.error("Erro ao carregar usuário:", error);
		mostrarErro("Erro de autenticação", error.message, true);
	}
}

async function carregarDashboardData() {
	if (!emailUsuario) {
		console.error("Email do usuário não disponível");
		return;
	}

	try {
		const response = await fetch(`/api/user/dashboard/data`, {
			headers: {
				"Authorization": `Bearer ${localStorage.getItem("token")}`,
				"Content-Type": "application/json"
			}
		});

		if (!response.ok) {
			const errorData = await response.json().catch(() => ({}));
			throw new Error(errorData.message || `Erro ${response.status}`);
		}

		const data = await response.json();
		atualizarUI(data);

	} catch (error) {
		console.error("Erro no dashboard:", error);
		handleDashboardError(error);
	}
}

function atualizarUI(data) {
	try {
		// Verifica se os dados são válidos
		if (!data || typeof data !== 'object') {
			throw new Error('Dados recebidos são inválidos');
		}

		// Atualiza informações do usuário
		if (data.nomeUsuario) {
			document.getElementById("nomeUsuario").textContent = data.nomeUsuario;
			document.getElementById("nomeCompleto").textContent = data.nomeUsuario;
		} else {
			console.warn('Nome do usuário não encontrado nos dados');
		}

		if (data.cargoUsuario || data.tipo) {
			document.getElementById("cargoUsuario").textContent = data.cargoUsuario || data.tipo || "Cargo não informado";
		}

		// Atualiza contadores com fallback para 0
		document.getElementById("totalPedidos").textContent = data.totalPedidos ?? 0;
		document.getElementById("pedidosEntregues").textContent = data.pedidosEntregues ?? 0;
		document.getElementById("pedidosPendentes").textContent = data.pedidosPendentes ?? 0;

		// Atualiza gráficos apenas se houver dados
		if (data.pedidosPorStatus && Object.keys(data.pedidosPorStatus).length > 0) {
			atualizarGraficoPedidosStatus(data.pedidosPorStatus);
		} else {
			console.warn('Nenhum dado de status de pedidos disponível');
		}

		// Atualiza tabela de pedidos
		if (data.ultimosPedidos && data.ultimosPedidos.length > 0) {
			preencherTabelaPedidos(data.ultimosPedidos);
		} else {
			const tabela = document.getElementById('tabelaPedidos');
			tabela.innerHTML = '<tr><td colspan="6" class="text-center">Nenhum pedido encontrado</td></tr>';
		}

	} catch (error) {
		console.error('Erro ao atualizar UI:', error);
		handleDashboardError(error);
	}
}

function atualizarGraficoPedidosStatus(dadosStatus) {
	const ctx = document.getElementById('pedidosStatusChart').getContext('2d');

	// Verifica se o gráfico existe e é uma instância válida de Chart antes de destruir
	if (window.pedidosStatusChart && typeof window.pedidosStatusChart.destroy === 'function') {
		window.pedidosStatusChart.destroy();
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

	// Prepara os dados para o gráfico
	const labels = Object.keys(dadosStatus || {});
	const data = labels.map(label => dadosStatus[label]);
	const backgroundColors = labels.map(label => cores[label] || 'rgba(128, 128, 128, 0.7)');

	// Cria o gráfico apenas se houver dados
	if (labels.length > 0 && data.length > 0) {
		window.pedidosStatusChart = new Chart(ctx, {
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
				}
			}
		});
	} else {
		// Mostra mensagem quando não há dados
		ctx.font = '16px Arial';
		ctx.fillStyle = '#666';
		ctx.textAlign = 'center';
		ctx.fillText('Nenhum dado disponível', ctx.canvas.width / 2, ctx.canvas.height / 2);
	}
}

function formatarData(dataPedido) {
	if (!dataPedido) return "Data Inválida";

	try {
		const data = new Date(dataPedido);
		return isNaN(data.getTime()) ? "Data Inválida" : data.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
	} catch (error) {
		return "Data Inválida";
	}
}

function preencherTabelaPedidos(pedidos) {
	const tabela = document.getElementById('tabelaPedidos');

	if (!pedidos || pedidos.length === 0) {
		tabela.innerHTML = '<tr><td colspan="6" class="text-center">Nenhum pedido encontrado</td></tr>';
		return;
	}

	tabela.innerHTML = pedidos.map(pedido => {
		const dataFormatada = formatarData(pedido.dataPedido);

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
                <td>${pedido.id}</td>
                <td>${dataFormatada}</td>
                <td>R$ ${pedido.valorTotal?.toFixed(2) || '0,00'}</td>
                <td><span class="badge ${cores[pedido.status] || 'bg-secondary'}">${pedido.status}</span></td>
                <td>${pedido.codigoEntrega || 'N/A'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="detalhesPedido(${pedido.id})">
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
	document.querySelectorAll('canvas').forEach(canvas => {
		const ctx = canvas.getContext('2d');
		ctx.clearRect(0, 0, canvas.width, canvas.height);
		ctx.fillText("Dados indisponíveis", 10, 30);
	});

	// Mostra mensagem de erro
	const isAuthError = error.message.includes("Sessão expirada") || error.message.includes("Sessão inválida");

	Swal.fire({
		icon: 'error',
		title: 'Erro no Dashboard',
		text: error.message,
		confirmButtonText: isAuthError ? "Ir para o login" : "OK"
	}).then(() => {
		if (isAuthError) {
			localStorage.removeItem("token");
			window.location.href = "/login";
		}
	});
}

function logout() {
	localStorage.removeItem("token");
	window.location.href = "/auth/login";
}

// Atualiza o dashboard a cada 30 segundos
let dashboardInterval = setInterval(carregarDashboardData, 30000);

// Atualiza quando a janela ganha foco
window.addEventListener('focus', carregarDashboardData);

// Inicialização
document.addEventListener('DOMContentLoaded', () => {
	if (!localStorage.getItem("token")) {
		window.location.href = "/auht/login";
	} else {
		carregarDadosUsuario();
	}
});

// Função para limpar o intervalo quando a página é fechada
window.addEventListener('beforeunload', () => {
	clearInterval(dashboardInterval);
});

// Variável para armazenar o ID do pedido atual
let pedidoAtualId = null;

// Função para exibir detalhes do pedido
async function detalhesPedido(id) {
	pedidoAtualId = id;
	try {
		const response = await fetch(`/pedidos/${id}/detalhes`, {
			headers: {
				"Authorization": `Bearer ${localStorage.getItem("token")}`,
				"Content-Type": "application/json"
			}
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
		Swal.fire({
			icon: 'error',
			title: 'Erro ao carregar detalhes',
			text: error.message
		});
	}
}

// Função para preencher o modal com os dados do pedido
function preencherModalDetalhes(pedido) {
	// Preenche informações básicas
	document.getElementById('pedidoId').textContent = pedido.id;
	document.getElementById('pedidoVendedor').textContent = `${pedido.vendedor} (${pedido.cpfVendedor})`;
	document.getElementById('pedidoData').textContent = formatarData(pedido.dataPedido);

	// Status com badge colorido
	const statusElement = document.getElementById('pedidoStatus');
	statusElement.textContent = pedido.status;
	statusElement.className = 'badge ' + getStatusBadgeClass(pedido.status);

	// Preenche itens do pedido
	const itensTbody = document.getElementById('pedidoItens');
	itensTbody.innerHTML = pedido.itens.map(item => `
        <tr>
            <td>${item.produto}</td>
            <td>${item.quantidade}</td>
            <td>R$ ${item.precoUnitario.toFixed(2)}</td>
            <td>R$ ${item.subtotal.toFixed(2)}</td>
        </tr>
    `).join('');

	// Preenche total
	document.getElementById('pedidoTotal').textContent = `R$ ${pedido.valorTotal.toFixed(2)}`;
}

// Função para baixar o relatório PDF
function baixarRelatorioPedido() {
	if (!pedidoAtualId) return;

	window.open(`/api/user/pedidos/${pedidoAtualId}/relatorio`, '_blank');
}

// Função auxiliar para classes de badge baseadas no status
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


// Atualiza data atual
document.getElementById('dataAtual').textContent = new Date().toLocaleDateString('pt-BR', {
	weekday: 'long',
	day: 'numeric',
	month: 'long',
	year: 'numeric'
});

// Sidebar toggle para mobile
document.querySelector('[data-bs-toggle="collapse"]').addEventListener('click', function() {
	document.querySelector('.sidebar').classList.toggle('show');
});
// ... (mantenha todas as funções existentes que não foram modificadas) ...

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

// Adicione este evento listener no final do seu arquivo JavaScript
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