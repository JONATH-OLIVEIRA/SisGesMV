package br.com.sisgesmv.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import com.itextpdf.layout.element.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.UnitValue;

import br.com.sisgesmv.dto.PedidoDTO;
import br.com.sisgesmv.dto.PedidoProdutoDTO;
import br.com.sisgesmv.enums.StatusPedido;
import br.com.sisgesmv.exception.PedidoNaoEncontradoException;
import br.com.sisgesmv.exception.ProdutoNaoEncontradoException;
import br.com.sisgesmv.model.Pedido;
import br.com.sisgesmv.model.PedidoProduto;
import br.com.sisgesmv.model.Produto;
import br.com.sisgesmv.model.Usuario;
import br.com.sisgesmv.repository.PedidoProdutoRepository;
import br.com.sisgesmv.repository.PedidoRepository;
import br.com.sisgesmv.repository.ProdutoRepository;
import br.com.sisgesmv.repository.UsuarioRepository;

@Service
public class PedidoService {

	private final PedidoRepository pedidoRepository;
	private final ProdutoRepository produtoRepository;
	private final PedidoProdutoRepository pedidoProdutoRepository;
	private final UsuarioRepository usuarioRepository;

	private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

	public PedidoService(UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository,
			ProdutoRepository produtoRepository, PedidoProdutoRepository pedidoProdutoRepository) {
		this.pedidoRepository = pedidoRepository;
		this.produtoRepository = produtoRepository;
		this.pedidoProdutoRepository = pedidoProdutoRepository;
		this.usuarioRepository = usuarioRepository;
	}

	private String gerarCodigoEntrega() {
		return "PED-" + new Random().nextInt(100000);
	}

	// 🔹 Criar um novo pedido com quantidade correta e atualização de estoque
	@Transactional
	public PedidoDTO criarPedido(PedidoDTO pedidoDTO) {
		Pedido pedido = new Pedido();
		pedido.setCpfVendedor(pedidoDTO.getCpfVendedor());
		pedido.setStatus(StatusPedido.PENDENTE);
		pedido.setCodigoEntrega(gerarCodigoEntrega());

		// Processa os itens do pedido
		List<PedidoProduto> pedidoProdutos = pedidoDTO.getPedidoProdutos().stream().map(itemDTO -> {
			Produto produto = produtoRepository.findById(itemDTO.getProdutoId())
					.orElseThrow(() -> new ProdutoNaoEncontradoException("Produto não encontrado!"));

			if (produto.getQuantidadeEstoque() < itemDTO.getQuantidade()) {
				throw new IllegalStateException("Estoque insuficiente para o produto: " + produto.getNome());
			}

			PedidoProduto pedidoProduto = new PedidoProduto();
			pedidoProduto.setProduto(produto);
			pedidoProduto.setQuantidade(itemDTO.getQuantidade());

			// Atualiza estoque
			produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - itemDTO.getQuantidade());
			produtoRepository.save(produto);

			return pedidoProduto;
		}).collect(Collectors.toList());

		// Calcula o total
		BigDecimal valorTotal = pedidoProdutos.stream().map(PedidoProduto::calcularSubtotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		pedido.setValorTotal(valorTotal);

		// Salva o pedido primeiro e usa uma referência final
		Pedido pedidoSalvo = pedidoRepository.save(pedido);

		// Associa e salva os itens do pedido
		pedidoProdutos.forEach(pp -> {
			pp.setPedido(pedidoSalvo);
			pedidoProdutoRepository.save(pp);
		});

		return converterParaDTO(pedidoSalvo);
	}

	// 🔹 Aprovar pedido
	@Transactional
	public PedidoDTO aprovarPedido(Long id, String cpfGerente) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

		pedido.setCpfGerente(cpfGerente);
		pedido.setStatus(StatusPedido.APROVADO);
		pedido = pedidoRepository.save(pedido);

		return converterParaDTO(pedido);
	}

	// 🔹 Finalizar separação e definir CPF separador
	@Transactional
	public PedidoDTO finalizarSeparacao(Long id, String cpfSeparador) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

		if (pedido.getStatus() != StatusPedido.APROVADO) {
			throw new IllegalStateException("Pedido precisa estar APROVADO para separação.");
		}

		pedido.setCpfSeparador(cpfSeparador);
		pedido.setStatus(StatusPedido.SEPARACAO);
		pedido = pedidoRepository.save(pedido);

		return converterParaDTO(pedido);
	}

	// 🔹 Marcar pedido como pronto para entrega
	@Transactional
	public PedidoDTO marcarProntoParaEntrega(Long id) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

		if (pedido.getStatus() != StatusPedido.SEPARACAO) {
			throw new IllegalStateException("Pedido precisa estar em SEPARACAO.");
		}

		pedido.setStatus(StatusPedido.PRONTO_PARA_ENTREGA);
		return converterParaDTO(pedidoRepository.save(pedido));
	}

	// 🔹 Confirmar entrega com código único
	@Transactional
	public PedidoDTO confirmarEntrega(Long id, String codigoEntrega) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

		if (!pedido.getCodigoEntrega().trim().equalsIgnoreCase(codigoEntrega.trim())) {
			throw new IllegalArgumentException("Código de entrega não corresponde ao pedido!");
		}

		if (!pedido.getStatus().equals(StatusPedido.PRONTO_PARA_ENTREGA)) {
			throw new IllegalStateException("Só é possível confirmar entrega de pedidos PRONTO_PARA_ENTREGA.");
		}

		pedido.setStatus(StatusPedido.ENTREGUE);
		return converterParaDTO(pedidoRepository.save(pedido));
	}

	public byte[] gerarRelatorioSeparacaoPDF(Long id) throws IOException {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

		String nomeVendedor = buscarNomePorCpf(pedido.getCpfVendedor());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(baos);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);

		// 📌 Cabeçalho
		document.add(new Paragraph("SISTEMA DE GERENCIAMENTO DE PEDIDOS").setBold().setFontSize(14));
		document.add(new Paragraph("=========================================\n"));

		document.add(new Paragraph("🆔 Pedido ID: " + pedido.getId()));
		document.add(new Paragraph("👨‍💼 Vendedor: " + nomeVendedor + " (" + pedido.getCpfVendedor() + ")"));
		document.add(new Paragraph(
				"📅 Data do Pedido: " + pedido.getDataPedido().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
		document.add(new Paragraph("📌 Status: " + pedido.getStatus() + "\n"));

		document.add(new Paragraph("-----------------------------------------"));
		document.add(new Paragraph("🛒 ITENS DO PEDIDO: (Marque os itens separados)"));
		document.add(new Paragraph("-----------------------------------------\n"));

		// 📌 Criando tabela
		Table table = new Table(UnitValue.createPercentArray(new float[] { 8, 35, 15, 15, 15 })).useAllAvailableWidth();
		table.addHeaderCell(new Cell().add(new Paragraph("✅ Separado?").setBold()));
		table.addHeaderCell(new Cell().add(new Paragraph("Produto").setBold()));
		table.addHeaderCell(new Cell().add(new Paragraph("Quantidade").setBold()));
		table.addHeaderCell(new Cell().add(new Paragraph("Preço Unitário").setBold()));
		table.addHeaderCell(new Cell().add(new Paragraph("Subtotal").setBold()));

		for (PedidoProduto pedidoProduto : pedidoProdutoRepository.findByPedido(pedido)) {
			table.addCell("[  ]"); // ✅ Espaço para separador marcar
			table.addCell(pedidoProduto.getProduto().getNome());
			table.addCell(String.valueOf(pedidoProduto.getQuantidade()));
			table.addCell("R$ " + pedidoProduto.getProduto().getPrecoVenda());
			table.addCell("R$ " + pedidoProduto.calcularSubtotal());
		}

		document.add(table); // 📌 Adicionando tabela ao documento

		document.add(new Paragraph("-----------------------------------------"));
		document.add(new Paragraph("💰 VALOR TOTAL DO PEDIDO: R$ " + pedido.getValorTotal()).setBold());
		document.add(new Paragraph("========================================="));
		document.add(new Paragraph("✅ Este pedido será processado conforme disponibilidade de estoque."));
		document.add(new Paragraph("🚚 Aguarde a confirmação de separação e envio."));
		document.add(new Paragraph("========================================="));
		document.add(new Paragraph("🔹 Separador Responsável: __________________"));
		document.add(new Paragraph("🔹 Data de Separação: ____/____/____"));

		document.close();
		return baos.toByteArray();
	}

	// 🔹 Método para buscar nome do vendedor pelo CPF
	private String buscarNomePorCpf(String cpf) {
		return usuarioRepository.findByCpf(cpf).map(Usuario::getNome).orElse("Nome não encontrado");
	}

	// 🔹 Converter entidade para DTO com lista de produtos
	private PedidoDTO converterParaDTO(Pedido pedido) {
		List<PedidoProdutoDTO> produtosDTO = pedidoProdutoRepository
				.findByPedido(pedido).stream().map(p -> new PedidoProdutoDTO(p.getProduto().getId(),
						p.getProduto().getNome(), p.getProduto().getPrecoVenda(), p.getQuantidade()))
				.collect(Collectors.toList());

		return new PedidoDTO(pedido.getId(), pedido.getCpfVendedor(), pedido.getCpfGerente(), pedido.getCpfSeparador(),
				pedido.getValorTotal(), pedido.getStatus(), pedido.getDataPedido(), produtosDTO,
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

	@Transactional
	public void cancelarPedido(Long id) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

		if (!pedido.getStatus().equals(StatusPedido.PENDENTE)) {
			throw new IllegalStateException("Só é possível cancelar pedidos PENDENTES!");
		}

		pedidoRepository.deleteById(id);
	}

	// 🔹 Editar pedido existente
	@Transactional
	public PedidoDTO editarPedido(Long id, PedidoDTO pedidoDTO) {
		Pedido pedidoExistente = pedidoRepository.findById(id)
				.orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado!"));

		// Verifica se o pedido pode ser editado (apenas pedidos pendentes podem ser
		// editados)
		if (pedidoExistente.getStatus() != StatusPedido.PENDENTE) {
			throw new IllegalStateException("Apenas pedidos com status PENDENTE podem ser editados");
		}

		// Atualiza os dados básicos do pedido
		pedidoExistente.setCpfVendedor(pedidoDTO.getCpfVendedor());

		// Remove os produtos antigos e devolve ao estoque
		List<PedidoProduto> produtosAntigos = pedidoProdutoRepository.findByPedido(pedidoExistente);
		for (PedidoProduto pp : produtosAntigos) {
			Produto produto = pp.getProduto();
			produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + pp.getQuantidade());
			produtoRepository.save(produto);
			pedidoProdutoRepository.delete(pp);
		}

		// Adiciona os novos produtos e atualiza estoque
		List<PedidoProduto> novosProdutos = pedidoDTO.getPedidoProdutos().stream().map(itemDTO -> {
			Produto produto = produtoRepository.findById(itemDTO.getProdutoId())
					.orElseThrow(() -> new ProdutoNaoEncontradoException("Produto não encontrado!"));

			if (produto.getQuantidadeEstoque() < itemDTO.getQuantidade()) {
				throw new IllegalStateException("Estoque insuficiente para o produto: " + produto.getNome());
			}

			PedidoProduto pedidoProduto = new PedidoProduto();
			pedidoProduto.setProduto(produto);
			pedidoProduto.setQuantidade(itemDTO.getQuantidade());
			pedidoProduto.setPedido(pedidoExistente);

			// Atualiza estoque
			produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - itemDTO.getQuantidade());
			produtoRepository.save(produto);

			return pedidoProduto;
		}).collect(Collectors.toList());

		// Calcula o novo valor total
		BigDecimal novoValorTotal = novosProdutos.stream().map(PedidoProduto::calcularSubtotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		pedidoExistente.setValorTotal(novoValorTotal);

		// Salva os novos produtos
		pedidoProdutoRepository.saveAll(novosProdutos);

		// Atualiza o pedido
		Pedido pedidoAtualizado = pedidoRepository.save(pedidoExistente);

		return converterParaDTO(pedidoAtualizado);
	}

}
