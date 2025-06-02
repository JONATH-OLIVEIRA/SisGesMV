package br.com.sisgesmv.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sisgesmv.dto.PedidoDTO;
import br.com.sisgesmv.enums.StatusPedido;
import br.com.sisgesmv.service.PedidoService;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

	private final PedidoService pedidoService;

	private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

	public PedidoController(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	// 🔹 Criar um novo pedido
	@PostMapping
	public ResponseEntity<PedidoDTO> criarPedido(@RequestBody PedidoDTO pedidoDTO) {
		PedidoDTO pedidoCriado = pedidoService.criarPedido(pedidoDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(pedidoCriado);
	}

	// 🔹 Buscar pedido por ID
	@GetMapping("/{id}")
	public ResponseEntity<PedidoDTO> buscarPedidoPorId(@PathVariable Long id) {
		PedidoDTO pedidoDTO = pedidoService.buscarPedidoPorId(id);
		return ResponseEntity.ok(pedidoDTO);
	}

	// 🔹 Listar todos os pedidos
	@GetMapping
	public ResponseEntity<List<PedidoDTO>> listarTodosPedidos() {
		List<PedidoDTO> pedidos = pedidoService.listarTodosPedidos();
		return pedidos.isEmpty() ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.ok(pedidos);
	}

	// 🔹 Listar pedidos por status
	@GetMapping("/status/{status}")
	public ResponseEntity<List<PedidoDTO>> listarPedidosPorStatus(@PathVariable StatusPedido status) {
		List<PedidoDTO> pedidos = pedidoService.listarPedidosPorStatus(status);
		return pedidos.isEmpty() ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.ok(pedidos);
	}

	// 🔹 Aprovar pedido (movendo para separação)
	@PutMapping("/{id}/aprovar")
	public ResponseEntity<PedidoDTO> aprovarPedido(@PathVariable Long id, @RequestBody String cpfGerente) {
		PedidoDTO pedidoAprovado = pedidoService.aprovarPedido(id, cpfGerente);
		return ResponseEntity.ok(pedidoAprovado);
	}

	// 🔹 Finalizar separação do pedido
	@PutMapping("/{id}/separar")
	public ResponseEntity<?> finalizarSeparacao(@PathVariable Long id, @RequestBody String cpfSeparador) {

		log.info("Iniciando separação para pedido {} com CPF {}", id, cpfSeparador);

		try {
			PedidoDTO pedidoSeparado = pedidoService.finalizarSeparacao(id, cpfSeparador);
			log.info("Separação concluída para pedido {}", id);
			return ResponseEntity.ok(pedidoSeparado);
		} catch (Exception e) {
			log.error("Erro na separação do pedido", e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("/{id}/pronto")
	public ResponseEntity<PedidoDTO> marcarProntoParaEntrega(@PathVariable Long id) {
		log.info("Marcando pedido {} como pronto para entrega", id);
		PedidoDTO pedido = pedidoService.marcarProntoParaEntrega(id);
		return ResponseEntity.ok(pedido);
	}

	// 🔹 Confirmar entrega com código único
	@PutMapping("/{id}/entregar")
	public ResponseEntity<PedidoDTO> confirmarEntrega(@PathVariable Long id, @RequestBody String codigoEntrega) {

		PedidoDTO pedidoEntregue = pedidoService.confirmarEntrega(id, codigoEntrega);
		return ResponseEntity.ok(pedidoEntregue);
	}

	// 🔹 Cancelar pedido (somente se estiver PENDENTE)
	@DeleteMapping("/{id}/cancelar")
	public ResponseEntity<String> cancelarPedido(@PathVariable Long id) {
		pedidoService.cancelarPedido(id);
		return ResponseEntity.ok("Pedido cancelado com sucesso!");
	}

	// 🔹 Excluir pedido (somente administradores)
	@DeleteMapping("/{id}")
	public ResponseEntity<String> excluirPedido(@PathVariable Long id) {
		pedidoService.excluirPedido(id);
		return ResponseEntity.ok("Pedido excluído com sucesso!");
	}

	@GetMapping("/{id}/relatorio")
	public ResponseEntity<byte[]> baixarRelatorioSeparacao(@PathVariable Long id) throws IOException {
		byte[] documento = pedidoService.gerarRelatorioSeparacao(id);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pedido_" + id + ".txt")
				.body(documento);
	}

}