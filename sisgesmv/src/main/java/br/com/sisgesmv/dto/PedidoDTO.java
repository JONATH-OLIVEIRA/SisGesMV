package br.com.sisgesmv.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.sisgesmv.enums.StatusPedido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PedidoDTO {

	private Long id;

	@NotBlank(message = "O CPF do vendedor é obrigatório.")
	private String cpfVendedor;

	private String cpfGerente;
	private String cpfSeparador;

	@NotNull(message = "O valor total do pedido é obrigatório.")
	@Positive(message = "O valor total deve ser positivo.")
	private BigDecimal valorTotal;

	@NotNull(message = "O status do pedido é obrigatório.")
	private StatusPedido status;

	private LocalDate dataPedido;

	private List<PedidoProdutoDTO> pedidoProdutos; // 🔹 Agora usamos `PedidoProdutoDTO`

	@NotBlank(message = "O código único de entrega é obrigatório.")
	private String codigoEntrega;

	public PedidoDTO() {
		this.dataPedido = LocalDate.now();
	}
	
	

	public PedidoDTO(Long id, String cpfVendedor, String cpfGerente, String cpfSeparador, BigDecimal valorTotal,
			StatusPedido status, LocalDate dataPedido, List<PedidoProdutoDTO> pedidoProdutos, String codigoEntrega) {
		this.id = id;
		this.cpfVendedor = cpfVendedor;
		this.cpfGerente = cpfGerente;
		this.cpfSeparador = cpfSeparador;
		this.valorTotal = valorTotal;
		this.status = status != null ? status : StatusPedido.PENDENTE;
		this.dataPedido = dataPedido != null ? dataPedido : LocalDate.now();
		this.pedidoProdutos = pedidoProdutos; // 🔹 Correção aqui
		this.codigoEntrega = codigoEntrega;
	}

	public List<PedidoProdutoDTO> getPedidoProdutos() {
		return pedidoProdutos != null ? pedidoProdutos : new ArrayList<>();
	}

	public void setPedidoProdutos(List<PedidoProdutoDTO> pedidoProdutos) {
		this.pedidoProdutos = pedidoProdutos;
	}

	public String getCodigoEntrega() {
		return codigoEntrega;
	}

	public void setCodigoEntrega(String codigoEntrega) {
		this.codigoEntrega = codigoEntrega;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCpfVendedor() {
		return cpfVendedor;
	}

	public void setCpfVendedor(String cpfVendedor) {
		this.cpfVendedor = cpfVendedor;
	}

	public String getCpfGerente() {
		return cpfGerente;
	}

	public void setCpfGerente(String cpfGerente) {
		this.cpfGerente = cpfGerente;
	}

	public String getCpfSeparador() {
		return cpfSeparador;
	}

	public void setCpfSeparador(String cpfSeparador) {
		this.cpfSeparador = cpfSeparador;
	}

	public BigDecimal getValorTotal() {
		return valorTotal;
	}

	public void setValorTotal(BigDecimal valorTotal) {
		this.valorTotal = valorTotal;
	}

	public StatusPedido getStatus() {
		return status;
	}

	public void setStatus(StatusPedido status) {
		this.status = status;
	}

	public LocalDate getDataPedido() {
		return dataPedido;
	}

	public void setDataPedido(LocalDate dataPedido) {
		this.dataPedido = dataPedido;
	}
}