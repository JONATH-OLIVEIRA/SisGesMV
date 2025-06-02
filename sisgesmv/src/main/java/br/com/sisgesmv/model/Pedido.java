package br.com.sisgesmv.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import br.com.sisgesmv.enums.StatusPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "pedidos")
public class Pedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "O CPF do vendedor é obrigatório.")
	@Column(nullable = false, length = 14)
	private String cpfVendedor;

	@Column(nullable = true, length = 14)
	private String cpfGerente;

	@Column(nullable = true, length = 14)
	private String cpfSeparador;

	@NotNull(message = "O valor total do pedido é obrigatório.")
	@Positive(message = "O valor total deve ser positivo.")
	private BigDecimal valorTotal;

	@Enumerated(EnumType.STRING)
	@NotNull(message = "O status do pedido é obrigatório.")
	private StatusPedido status;

	@Column(name = "data_pedido", nullable = false)
	private LocalDate dataPedido = LocalDate.now();

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
	private List<PedidoProduto> pedidoProdutos;

	@NotBlank(message = "O código único de entrega é obrigatório.")
	@Column(nullable = false, unique = true)
	private String codigoEntrega; // 🔹 Código único de entrega do pedido

	public Pedido() {
	}

	public Pedido(Long id, @NotBlank(message = "O CPF do vendedor é obrigatório.") String cpfVendedor,
			String cpfGerente, String cpfSeparador,
			@NotNull(message = "O valor total do pedido é obrigatório.") @Positive(message = "O valor total deve ser positivo.") BigDecimal valorTotal,
			@NotNull(message = "O status do pedido é obrigatório.") StatusPedido status, LocalDate dataPedido,
			List<PedidoProduto> pedidoProdutos,
			@NotBlank(message = "O código único de entrega é obrigatório.") String codigoEntrega) {
		super();
		this.id = id;
		this.cpfVendedor = cpfVendedor;
		this.cpfGerente = cpfGerente;
		this.cpfSeparador = cpfSeparador;
		this.valorTotal = valorTotal;
		this.status = status;
		this.dataPedido = dataPedido;
		this.pedidoProdutos = pedidoProdutos;
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

	public List<PedidoProduto> getPedidoProdutos() {
		return pedidoProdutos;
	}

	public void setPedidoProdutos(List<PedidoProduto> pedidoProdutos) {
		this.pedidoProdutos = pedidoProdutos;
	}

	public String getCodigoEntrega() {
		return codigoEntrega;
	}

	public void setCodigoEntrega(String codigoEntrega) {
		this.codigoEntrega = codigoEntrega;
	}

	@Override
	public String toString() {
		return "Pedido [id=" + id + ", cpfVendedor=" + cpfVendedor + ", cpfGerente=" + cpfGerente + ", cpfSeparador="
				+ cpfSeparador + ", valorTotal=" + valorTotal + ", status=" + status + ", dataPedido=" + dataPedido
				+ ", pedidoProdutos=" + pedidoProdutos + ", codigoEntrega=" + codigoEntrega + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pedido other = (Pedido) obj;
		return Objects.equals(id, other.id);
	}

}