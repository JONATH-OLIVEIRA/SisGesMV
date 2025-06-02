package br.com.sisgesmv.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sisgesmv.dto.PedidoDTO;
import br.com.sisgesmv.enums.StatusPedido;
import br.com.sisgesmv.exception.PedidoNaoEncontradoException;
import br.com.sisgesmv.service.PedidoService;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

	private final PedidoService pedidoService;
	private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

	public PedidoController(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	@PostMapping
	public ResponseEntity<PedidoDTO> criarPedido(@RequestBody PedidoDTO pedidoDTO) {
		log.info("Criando novo pedido para o vendedor: {}", pedidoDTO.getCpfVendedor());
		PedidoDTO pedidoCriado = pedidoService.criarPedido(pedidoDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(pedidoCriado);
	}

	@GetMapping("/{id}")
	public ResponseEntity<PedidoDTO> buscarPedidoPorId(@PathVariable Long id) {
		log.info("Buscando pedido por ID: {}", id);
		PedidoDTO pedidoDTO = pedidoService.buscarPedidoPorId(id);
		return ResponseEntity.ok(pedidoDTO);
	}

	@GetMapping
	public ResponseEntity<List<PedidoDTO>> listarTodosPedidos() {
		log.info("Listando todos os pedidos");
		List<PedidoDTO> pedidos = pedidoService.listarTodosPedidos();
		return ResponseEntity.ok(pedidos);
	}

	@GetMapping("/status/{status}")
	public ResponseEntity<List<PedidoDTO>> listarPedidosPorStatus(@PathVariable StatusPedido status) {
		log.info("Listando pedidos com status: {}", status);
		List<PedidoDTO> pedidos = pedidoService.listarPedidosPorStatus(status);
		return ResponseEntity.ok(pedidos);
	}

	@PutMapping("/{id}/aprovar")
	public ResponseEntity<PedidoDTO> aprovarPedido(@PathVariable Long id, @RequestBody String cpfGerente) {
		log.info("Aprovando pedido ID: {} pelo gerente: {}", id, cpfGerente);
		PedidoDTO pedidoAprovado = pedidoService.aprovarPedido(id, cpfGerente);
		return ResponseEntity.ok(pedidoAprovado);
	}

	@PutMapping("/{id}/separar")
	public ResponseEntity<PedidoDTO> finalizarSeparacao(@PathVariable Long id, @RequestBody String cpfSeparador) {
		log.info("Finalizando separação do pedido ID: {} pelo separador: {}", id, cpfSeparador);
		PedidoDTO pedidoSeparado = pedidoService.finalizarSeparacao(id, cpfSeparador);
		return ResponseEntity.ok(pedidoSeparado);
	}

	@PutMapping("/{id}/pronto")
	public ResponseEntity<PedidoDTO> marcarProntoParaEntrega(@PathVariable Long id) {
		log.info("Marcando pedido ID: {} como pronto para entrega", id);
		PedidoDTO pedido = pedidoService.marcarProntoParaEntrega(id);
		return ResponseEntity.ok(pedido);
	}

	@PutMapping("/{id}/entregar")
	public ResponseEntity<PedidoDTO> confirmarEntrega(@PathVariable Long id, @RequestBody String codigoEntrega) {
		log.info("Confirmando entrega do pedido ID: {}", id);
		PedidoDTO pedidoEntregue = pedidoService.confirmarEntrega(id, codigoEntrega);
		return ResponseEntity.ok(pedidoEntregue);
	}

	@DeleteMapping("/{id}/cancelar")
	public ResponseEntity<Void> cancelarPedido(@PathVariable Long id) {
		log.info("Cancelando pedido ID: {}", id);
		pedidoService.cancelarPedido(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/relatorio")
	public ResponseEntity<byte[]> baixarRelatorioSeparacao(@PathVariable Long id) throws IOException {
		log.info("Gerando relatório de separação para o pedido ID: {}", id);

		// 🔹 Agora chamamos `gerarRelatorioSeparacaoPDF()` para gerar o relatório em
		// PDF
		byte[] documento = pedidoService.gerarRelatorioSeparacaoPDF(id);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pedido_" + id + ".pdf")
				.contentType(MediaType.APPLICATION_PDF) // 🔹 Definimos que o retorno é um PDF
				.body(documento);
	}

	@GetMapping("/{id}/completo")
	public ResponseEntity<PedidoDTO> buscarPedidoCompleto(@PathVariable Long id) {
		PedidoDTO pedidoDTO = pedidoService.buscarPedidoPorId(id);
		return ResponseEntity.ok(pedidoDTO);
	}

	@PutMapping("/{id}")
	public ResponseEntity<PedidoDTO> atualizarPedido(@PathVariable Long id, @RequestBody PedidoDTO pedidoDTO) {
		try {
			PedidoDTO pedidoAtualizado = pedidoService.editarPedido(id, pedidoDTO);
			return ResponseEntity.ok(pedidoAtualizado);
		} catch (PedidoNaoEncontradoException e) {
			return ResponseEntity.notFound().build();
		}
	}

}