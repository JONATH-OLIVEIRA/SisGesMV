package br.com.sisgesmv.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PedidoProdutoDTO {
	@NotNull(message = "O ID do produto é obrigatório.")
	private Long produtoId; // 🔹 Agora temos apenas o ID do produto

	@NotNull(message = "O nome do produto é obrigatório.")
	private String nomeProduto; // 🔹 Agora armazenamos o nome do produto

	@NotNull(message = "O preço do produto é obrigatório.")
	@Positive(message = "O preço deve ser positivo.")
	private BigDecimal precoUnitario; // 🔹 Preço unitário do produto

	@NotNull
	@Positive(message = "A quantidade deve ser positiva.")
	private Integer quantidade;
	
	public PedidoProdutoDTO(Long produtoId, String nomeProduto, BigDecimal precoUnitario, Integer quantidade) {
	    this.produtoId = produtoId;
	    this.nomeProduto = nomeProduto;
	    this.precoUnitario = precoUnitario;
	    this.quantidade = quantidade;
	}

	// 🔹 Agora calculamos o subtotal diretamente
	public BigDecimal calcularSubtotal() {
		return precoUnitario.multiply(new BigDecimal(quantidade));
	}

	public Long getProdutoId() {
		return produtoId;
	}

	public void setProdutoId(Long produtoId) {
		this.produtoId = produtoId;
	}

	public String getNomeProduto() {
		return nomeProduto;
	}

	public void setNomeProduto(String nomeProduto) {
		this.nomeProduto = nomeProduto;
	}

	public BigDecimal getPrecoUnitario() {
		return precoUnitario;
	}

	public void setPrecoUnitario(BigDecimal precoUnitario) {
		this.precoUnitario = precoUnitario;
	}

	public Integer getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(Integer quantidade) {
		this.quantidade = quantidade;
	}
}