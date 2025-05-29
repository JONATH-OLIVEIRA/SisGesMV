package br.com.sisgesmv.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.sisgesmv.enums.CategoriaProduto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ProdutoDTO {

	private Long id;

	@NotBlank(message = "O nome do produto é obrigatório.")
	@Size(min = 3, max = 100, message = "O nome do produto deve ter entre 3 e 100 caracteres.")
	private String nome;

	@NotBlank(message = "A descrição do produto é obrigatória.")
	@Size(max = 255, message = "A descrição do produto deve ter no máximo 255 caracteres.")
	private String descricao;

	@NotNull(message = "O preço de compra é obrigatório.")
	@Positive(message = "O preço de compra deve ser um valor positivo.")
	private BigDecimal precoCompra;

	@NotNull(message = "O preço de venda é obrigatório.")
	@Positive(message = "O preço de venda deve ser um valor positivo.")
	private BigDecimal precoVenda;

	@NotNull(message = "A quantidade em estoque é obrigatória.")
	@Positive(message = "A quantidade em estoque deve ser um valor positivo.")
	private Integer quantidadeEstoque;

	@NotNull(message = "A categoria do produto é obrigatória.")
	private CategoriaProduto categoria;

	private LocalDate dataCadastro;

	// ✅ Construtor padrão
	public ProdutoDTO() {
		this.dataCadastro = LocalDate.now();
	}

	// ✅ Construtor com todos os atributos
	public ProdutoDTO(Long id, String nome, String descricao, BigDecimal precoCompra, BigDecimal precoVenda,
			Integer quantidadeEstoque, CategoriaProduto categoria, LocalDate dataCadastro) {
		this.id = id;
		this.nome = nome;
		this.descricao = descricao;
		this.precoCompra = precoCompra;
		this.precoVenda = precoVenda;
		this.quantidadeEstoque = quantidadeEstoque;
		this.categoria = categoria;
		this.dataCadastro = dataCadastro != null ? dataCadastro : LocalDate.now();
	}

	// Getters e Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public BigDecimal getPrecoCompra() {
		return precoCompra;
	}

	public void setPrecoCompra(BigDecimal precoCompra) {
		this.precoCompra = precoCompra;
	}

	public BigDecimal getPrecoVenda() {
		return precoVenda;
	}

	public void setPrecoVenda(BigDecimal precoVenda) {
		this.precoVenda = precoVenda;
	}

	public Integer getQuantidadeEstoque() {
		return quantidadeEstoque;
	}

	public void setQuantidadeEstoque(Integer quantidadeEstoque) {
		this.quantidadeEstoque = quantidadeEstoque;
	}

	public CategoriaProduto getCategoria() {
		return categoria;
	}

	public void setCategoria(CategoriaProduto categoria) {
		this.categoria = categoria;
	}

	public LocalDate getDataCadastro() {
		return dataCadastro;
	}

	public void setDataCadastro(LocalDate dataCadastro) {
		this.dataCadastro = dataCadastro;
	}

}