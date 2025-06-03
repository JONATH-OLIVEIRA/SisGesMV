document.addEventListener('DOMContentLoaded', function() {
	const token = localStorage.getItem('token');
	if (!token) {
		window.location.href = '/auth/login';
		return;
	}

	// Elementos da página
	const listaProdutos = document.getElementById('listaProdutos');
	const resumoPedido = document.getElementById('resumoPedido');
	const finalizarPedidoBtn = document.getElementById('finalizarPedido');
	const totalPedidoElement = document.querySelector('.total-pedido');
	const confirmacaoModal = new bootstrap.Modal(document.getElementById('confirmacaoModal'));
	const confirmarEnvioBtn = document.getElementById('confirmarEnvio');
	const detalhesConfirmacao = document.getElementById('detalhesConfirmacao');

	// Estado do pedido
	let produtosDisponiveis = [];
	let pedido = {
		produtos: [],
		total: 0
	};

	// Carrega produtos disponíveis
	function carregarProdutos() {
		fetch('/produtos', {
			headers: {
				'Authorization': `Bearer ${token}`,
				'Content-Type': 'application/json'
			}
		})
			.then(response => {
				if (!response.ok) {
					throw new Error('Erro ao carregar produtos');
				}
				return response.json();
			})
			.then(produtos => {
				produtosDisponiveis = produtos;
				renderizarProdutos(produtos);
			})
			.catch(error => {
				console.error('Erro:', error);
				alert('Erro ao carregar produtos. ' + error.message);
			});
	}

	// Renderiza a lista de produtos
	function renderizarProdutos(produtos) {
		listaProdutos.innerHTML = produtos.map(produto => {
			const preco = Number(produto.precoVenda);
			const precoFormatado = isNaN(preco) ? 'N/A' : `R$ ${preco.toFixed(2)}`;
			const estoque = produto.quantidadeEstoque ?? 0;

			return `
                <tr data-id="${produto.id}" class="produto-row">
                    <td>
                        <input type="checkbox" class="form-check-input produto-checkbox" 
                               data-id="${produto.id}" data-preco="${preco}">
                    </td>
                    <td>${produto.nome ?? 'Produto sem nome'}</td>
                    <td>${precoFormatado}</td>
                    <td>${estoque}</td>
                    <td>
                        <input type="number" class="form-control quantidade-input" 
                               min="1" max="${estoque}" value="1" 
                               data-id="${produto.id}" disabled>
                    </td>
                    <td class="subtotal" data-id="${produto.id}">${precoFormatado}</td>
                </tr>
            `;
		}).join('');

		adicionarEventos();
	}

	function adicionarEventos() {
		document.querySelectorAll('.produto-checkbox').forEach(checkbox => {
			checkbox.addEventListener('change', function() {
				const produtoId = parseInt(this.dataset.id);
				const preco = parseFloat(this.dataset.preco);
				const quantidadeInput = document.querySelector(`.quantidade-input[data-id="${produtoId}"]`);

				quantidadeInput.disabled = !this.checked;

				if (this.checked) {
					document.querySelector(`tr[data-id="${produtoId}"]`).classList.add('selected');
					adicionarProdutoAoPedido(produtoId, preco);
				} else {
					document.querySelector(`tr[data-id="${produtoId}"]`).classList.remove('selected');
					removerProdutoDoPedido(produtoId);
				}

				atualizarResumoPedido();
			});
		});

		document.querySelectorAll('.quantidade-input').forEach(input => {
			input.addEventListener('change', function() {
				const produtoId = parseInt(this.dataset.id);
				const quantidade = parseInt(this.value) || 1;
				const produto = produtosDisponiveis.find(p => p.id === produtoId);

				if (!produto) {
					alert('Produto não encontrado');
					return;
				}

				if (quantidade > produto.quantidadeEstoque) {
					alert(`Quantidade solicitada (${quantidade}) maior que estoque disponível (${produto.quantidadeEstoque})`);
					this.value = produto.quantidadeEstoque;
					return;
				}

				atualizarQuantidadeNoPedido(produtoId, quantidade);
				atualizarSubtotal(produtoId, produto.precoVenda);
				atualizarResumoPedido();
			});
		});
	}

	// Adiciona produto ao pedido
	function adicionarProdutoAoPedido(produtoId, precoVenda) {
		const produto = produtosDisponiveis.find(p => p.id === produtoId);
		if (!produto) return;

		const quantidadeInput = document.querySelector(`.quantidade-input[data-id="${produtoId}"]`);
		const quantidade = parseInt(quantidadeInput.value) || 1;

		if (!pedido.produtos.some(p => p.produtoId === produto.id)) {
			pedido.produtos.push({
				produtoId: produto.id,
				nomeProduto: produto.nome,
				precoVenda: precoVenda,
				quantidade: quantidade
			});
		}

		calcularTotalPedido();
	}

	// Remove produto do pedido
	function removerProdutoDoPedido(produtoId) {
		pedido.produtos = pedido.produtos.filter(p => p.produtoId !== produtoId);
		calcularTotalPedido();
	}

	// Atualiza quantidade no pedido
	function atualizarQuantidadeNoPedido(produtoId, quantidade) {
		const produtoPedido = pedido.produtos.find(p => p.produtoId === produtoId);
		if (produtoPedido) {
			produtoPedido.quantidade = quantidade;
			calcularTotalPedido();
		}
	}

	// Atualiza subtotal na tabela
	function atualizarSubtotal(produtoId, precoVenda) {
		const produtoPedido = pedido.produtos.find(p => p.produtoId === produtoId);
		if (produtoPedido) {
			const subtotal = (precoVenda ?? produtoPedido.precoVenda ?? 0) * produtoPedido.quantidade;
			document.querySelector(`.subtotal[data-id="${produtoId}"]`).textContent =
				`R$ ${subtotal.toFixed(2)}`;
		}
	}

	// Calcula o total do pedido
	function calcularTotalPedido() {
		pedido.total = pedido.produtos.reduce((total, produto) => {
			return total + ((produto.precoVenda ?? 0) * produto.quantidade);
		}, 0);

		totalPedidoElement.textContent = `Total: R$ ${pedido.total.toFixed(2)}`;
	}

	// Atualiza o resumo do pedido
	function atualizarResumoPedido() {
		if (pedido.produtos.length === 0) {
			resumoPedido.innerHTML = '<div class="alert alert-info">Nenhum produto selecionado</div>';
			finalizarPedidoBtn.disabled = true;
			return;
		}

		finalizarPedidoBtn.disabled = false;

		resumoPedido.innerHTML = pedido.produtos.map(produto => {
			const subtotal = (produto.precoVenda ?? 0) * produto.quantidade;
			return `
                <li class="list-group-item d-flex justify-content-between align-items-center">
                    <div>
                        <strong>${produto.nomeProduto}</strong>
                        <div class="text-muted small">Quantidade: ${produto.quantidade}</div>
                    </div>
                    <span class="badge bg-primary rounded-pill">
                        R$ ${subtotal.toFixed(2)}
                    </span>
                </li>
            `;
		}).join('');
	}

	// Prepara detalhes para o modal de confirmação
	function prepararConfirmacao() {
		detalhesConfirmacao.innerHTML = `
            <ul class="list-group mb-3">
                ${pedido.produtos.map(produto => `
                    <li class="list-group-item">
                        ${produto.quantidade}x ${produto.nomeProduto} - R$ ${(produto.precoVenda ?? 0).toFixed(2)} cada
                    </li>
                `).join('')}
            </ul>
            <h5 class="text-end">Total: R$ ${pedido.total.toFixed(2)}</h5>
        `;
	}

	// Envia o pedido para o backend
	function enviarPedido() {
		const usuario = JSON.parse(atob(token.split('.')[1]));

		// Primeiro vamos buscar os dados completos do usuário incluindo o CPF
		fetch(`/usuarios/${usuario.id}`, {
			headers: {
				'Authorization': `Bearer ${token}`,
				'Content-Type': 'application/json'
			}
		})
			.then(response => {
				if (!response.ok) {
					throw new Error('Erro ao obter dados do usuário');
				}
				return response.json();
			})
			.then(usuarioCompleto => {
				if (!usuarioCompleto.cpf) {
					throw new Error('CPF do vendedor não encontrado');
				}

				const pedidoDTO = {
					cpfVendedor: usuarioCompleto.cpf,
					pedidoProdutos: pedido.produtos.map(produto => ({
						produtoId: produto.produtoId,
						nomeProduto: produto.nomeProduto,
						precoVenda: produto.precoVenda,
						quantidade: produto.quantidade
					}))
				};

				return fetch('/pedidos', {
					method: 'POST',
					headers: {
						'Authorization': `Bearer ${token}`,
						'Content-Type': 'application/json'
					},
					body: JSON.stringify(pedidoDTO)
				});
			})
			.then(response => {
				if (!response.ok) {
					return response.json().then(err => { throw new Error(err.message || 'Erro ao enviar pedido'); });
				}
				return response.json();
			})
			.then(data => {
				alert('Pedido enviado com sucesso! Número do pedido: ' + data.id);
				window.location.reload();
			})
			.catch(error => {
				console.error('Erro:', error);
				alert('Erro ao enviar pedido: ' + error.message);
			});
	}

	// Event Listeners
	finalizarPedidoBtn.addEventListener('click', function() {
		prepararConfirmacao();
		confirmacaoModal.show();
	});

	confirmarEnvioBtn.addEventListener('click', function() {
		confirmacaoModal.hide();
		enviarPedido();
	});

	// Inicialização
	carregarProdutos();
	atualizarResumoPedido();
});