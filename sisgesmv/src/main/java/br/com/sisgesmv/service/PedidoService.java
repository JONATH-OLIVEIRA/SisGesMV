package br.com.sisgesmv.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sisgesmv.dto.PedidoDTO;
import br.com.sisgesmv.enums.StatusPedido;
import br.com.sisgesmv.exception.PedidoNaoEncontradoException;
import br.com.sisgesmv.model.Pedido;
import br.com.sisgesmv.model.Produto;
import br.com.sisgesmv.repository.PedidoRepository;
import br.com.sisgesmv.repository.ProdutoRepository;


@Service
public class PedidoService {

	private final PedidoRepository pedidoRepository;
	private final ProdutoRepository produtoRepository;
	
	
	private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

	public PedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository) {
		this.pedidoRepository = pedidoRepository;
		this.produtoRepository = produtoRepository;
		
	}

	// 🔹 Criar um novo pedido
	@Transactional
	public PedidoDTO criarPedido(PedidoDTO pedidoDTO) {
		List<Produto> produtos = produtoRepository
				.findAllById(pedidoDTO.getProdutos().stream().map(Produto::getId).collect(Collectors.toList()));

		BigDecimal valorTotal = produtos.stream().map(Produto::getPrecoVenda).reduce(BigDecimal.ZERO, BigDecimal::add);

		Pedido pedido = new Pedido();
		pedido.setCpfVendedor(pedidoDTO.getCpfVendedor());
		pedido.setProdutos(produtos);
		pedido.setValorTotal(valorTotal);
		pedido.setStatus(StatusPedido.PENDENTE);
		pedido.setCodigoEntrega(gerarCodigoEntrega());

		pedido = pedidoRepository.save(pedido);
		return converterParaDTO(pedido);
	}

	// 🔹 Aprovar pedido (movendo para separação)
	

	@Transactional
	public PedidoDTO aprovarPedido(Long id, String cpfGerente) {
	    Pedido pedido = pedidoRepository.findById(id)
	            .orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

	    // Aprovação mantém status APROVADO (não muda para SEPARACAO)
	    pedido.setCpfGerente(cpfGerente);
	    pedido.setStatus(StatusPedido.APROVADO);
	    pedido = pedidoRepository.save(pedido);

	    return converterParaDTO(pedido);
	}

	@Transactional
	public PedidoDTO finalizarSeparacao(Long id, String cpfSeparador) {
	    // Validação rigorosa do CPF
	    cpfSeparador = Objects.requireNonNull(cpfSeparador, "CPF do separador é obrigatório")
	                         .replaceAll("[^0-9]", "");
	    
	    if (cpfSeparador.length() != 11) {
	        throw new IllegalArgumentException("CPF inválido!");
	    }

	    Pedido pedido = pedidoRepository.findById(id)
	            .orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

	    // Verifica status APROVADO antes de separar
	    if (pedido.getStatus() != StatusPedido.APROVADO) {
	        throw new IllegalStateException("Pedido precisa estar APROVADO para separação");
	    }

	    // Atualiza campos
	    pedido.setCpfSeparador(cpfSeparador);
	    pedido.setStatus(StatusPedido.SEPARACAO);
	    
	    // DEBUG: Verifica antes de salvar
	    log.info("Antes de salvar - CPF Separador: {}", pedido.getCpfSeparador());
	    
	    pedido = pedidoRepository.saveAndFlush(pedido);
	    
	    // DEBUG: Verifica após salvar
	    Pedido pedidoVerificado = pedidoRepository.findById(id).orElseThrow();
	    log.info("Após salvar - CPF Separador: {}", pedidoVerificado.getCpfSeparador());
	    
	    return converterParaDTO(pedido);
	}

	@Transactional
	public PedidoDTO marcarProntoParaEntrega(Long id) {
	    Pedido pedido = pedidoRepository.findById(id)
	            .orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

	    // Verifica se está em SEPARACAO
	    if (pedido.getStatus() != StatusPedido.SEPARACAO) {
	        throw new IllegalStateException("Pedido precisa estar em SEPARACAO");
	    }

	    pedido.setStatus(StatusPedido.PRONTO_PARA_ENTREGA);
	    return converterParaDTO(pedidoRepository.save(pedido));
	}
	
	// 🔹 Confirmar entrega com código único
	@Transactional
	public PedidoDTO confirmarEntrega(Long id, String codigoEntrega) {
		// Busca o pedido pelo ID
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

		// Valida se o código de entrega corresponde
		if (!pedido.getCodigoEntrega().trim().equalsIgnoreCase(codigoEntrega.trim())) {
			throw new IllegalArgumentException("Código de entrega não corresponde ao pedido!");
		}

		// Valida se o pedido está no status correto
		if (!pedido.getStatus().equals(StatusPedido.PRONTO_PARA_ENTREGA)) {
			throw new IllegalStateException(
					"Só é possível confirmar entrega de pedidos com status PRONTO_PARA_ENTREGA. Status atual: "
							+ pedido.getStatus());
		}

		// Atualiza o status e a data de entrega
		pedido.setStatus(StatusPedido.ENTREGUE);

		return converterParaDTO(pedidoRepository.save(pedido));
	}

	// 🔹 Cancelar pedido (somente se estiver PENDENTE)
	@Transactional
	public void cancelarPedido(Long id) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

		if (!pedido.getStatus().equals(StatusPedido.PENDENTE)) {
			throw new IllegalStateException("Só é possível cancelar pedidos PENDENTES!");
		}

		pedidoRepository.deleteById(id);
	}

	// 🔹 Excluir pedido (somente administradores)
	@Transactional
	public void excluirPedido(Long id) {
		if (!pedidoRepository.existsById(id)) {
			throw new PedidoNaoEncontradoException("Pedido não encontrado!");
		}
		pedidoRepository.deleteById(id);
	}

	// 🔹 Gerar código único de entrega
	private String gerarCodigoEntrega() {
		return "PED-" + new Random().nextInt(100000);
	}

	// 🔹 Converter entidade para DTO
	private PedidoDTO converterParaDTO(Pedido pedido) {
		return new PedidoDTO(pedido.getId(), pedido.getCpfVendedor(), pedido.getCpfGerente(), pedido.getCpfSeparador(),
				pedido.getValorTotal(), pedido.getStatus(), pedido.getDataPedido(), pedido.getProdutos(),
				pedido.getCodigoEntrega());
	}

	// 🔹 Buscar pedido por ID
	public PedidoDTO buscarPedidoPorId(Long id) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new PedidoNaoEncontradoException("Pedido com ID " + id + " não encontrado!"));

		return converterParaDTO(pedido);
	}

	// 🔹 Listar todos os pedidos
	public List<PedidoDTO> listarTodosPedidos() {
		return pedidoRepository.findAll().stream().map(this::converterParaDTO).collect(Collectors.toList());
	}

	// 🔹 Listar pedidos por status
	public List<PedidoDTO> listarPedidosPorStatus(StatusPedido status) {
		List<Pedido> pedidos = pedidoRepository.findByStatus(status);
		return pedidos.stream().map(this::converterParaDTO).collect(Collectors.toList());
	}
	public byte[] gerarRelatorioSeparacao(Long id) throws IOException {
	    Pedido pedido = pedidoRepository.findById(id)
	            .orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

	    // Criando o conteúdo do documento
	    StringBuilder conteudo = new StringBuilder();
	    conteudo.append("Pedido ID: ").append(pedido.getId()).append("\n");
	    conteudo.append("Código de Entrega: ").append(pedido.getCodigoEntrega()).append("\n\n");
	    conteudo.append("Produtos:\n");

	    for (Produto produto : pedido.getProdutos()) {
	        conteudo.append("🔹 ").append(produto.getNome())
	                .append(" - Qtd: ").append(produto.getQuantidadeEstoque()).append("\n");
	    }

	    // Convertendo para byte array (para PDF ou CSV)
	    return conteudo.toString().getBytes(StandardCharsets.UTF_8);
	}

}