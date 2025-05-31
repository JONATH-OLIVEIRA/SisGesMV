package br.com.sisgesmv.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import br.com.sisgesmv.enums.StatusPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

	@ManyToMany
	@JoinTable(name = "pedido_produto", joinColumns = @JoinColumn(name = "pedido_id"), inverseJoinColumns = @JoinColumn(name = "produto_id"))
	private List<Produto> produtos;

	@NotBlank(message = "O código único de entrega é obrigatório.")
	@Column(nullable = false, unique = true)
	private String codigoEntrega; // 🔹 Código único de entrega do pedido

	public Pedido() {
	}

	public Pedido(Long id, String cpfVendedor, String cpfGerente, String cpfSeparador, BigDecimal valorTotal,
			StatusPedido status, LocalDate dataPedido, List<Produto> produtos, String codigoEntrega) {
		this.id = id;
		this.cpfVendedor = cpfVendedor;
		this.cpfGerente = cpfGerente;
		this.cpfSeparador = cpfSeparador;
		this.valorTotal = valorTotal;
		this.status = status != null ? status : StatusPedido.PENDENTE;
		this.dataPedido = dataPedido != null ? dataPedido : LocalDate.now();
		this.produtos = produtos;
		this.codigoEntrega = codigoEntrega;
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

	public List<Produto> getProdutos() {
		return produtos;
	}

	public void setProdutos(List<Produto> produtos) {
		this.produtos = produtos;
	}

	@Override
	public String toString() {
		return "Pedido [id=" + id + ", cpfVendedor=" + cpfVendedor + ", cpfGerente=" + cpfGerente + ", cpfSeparador="
				+ cpfSeparador + ", valorTotal=" + valorTotal + ", status=" + status + ", dataPedido=" + dataPedido
				+ ", produtos=" + produtos + ", codigoEntrega=" + codigoEntrega + "]";
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