package br.com.sisgesmv.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sisgesmv.dto.ProdutoDTO;
import br.com.sisgesmv.enums.CategoriaProduto;
import br.com.sisgesmv.exception.ProdutoNaoEncontradoException;
import br.com.sisgesmv.model.Produto;
import br.com.sisgesmv.repository.ProdutoRepository;

@Service
public class ProdutoService {

	private final ProdutoRepository produtoRepository;

	public ProdutoService(ProdutoRepository produtoRepository) {
		this.produtoRepository = produtoRepository;
	}

	// 🔹 Cadastrar um novo produto
	@Transactional
	public ProdutoDTO cadastrarProduto(ProdutoDTO produtoDTO) {
		Produto produto = new Produto();
		produto.setNome(produtoDTO.getNome());
		produto.setDescricao(produtoDTO.getDescricao());
		produto.setPrecoCompra(produtoDTO.getPrecoCompra());
		produto.setPrecoVenda(produtoDTO.getPrecoVenda());
		produto.setQuantidadeEstoque(produtoDTO.getQuantidadeEstoque());
		produto.setCategoria(produtoDTO.getCategoria());

		produto = produtoRepository.save(produto);

		return converterParaDTO(produto);
	}

	// 🔹 Buscar um produto por ID
	public ProdutoDTO buscarProdutoPorId(Long id) {
		Produto produto = produtoRepository.findById(id)
				.orElseThrow(() -> new ProdutoNaoEncontradoException("Produto com ID " + id + " não encontrado!"));

		return converterParaDTO(produto);
	}

	// 🔹 Listar todos os produtos
	public List<ProdutoDTO> listarTodosProdutos() {
		return produtoRepository.findAll().stream().map(this::converterParaDTO).collect(Collectors.toList());
	}

	// 🔹 Listar produtos por categoria
	public List<ProdutoDTO> listarProdutosPorCategoria(CategoriaProduto categoria) {
		return produtoRepository.findByCategoria(categoria).stream().map(this::converterParaDTO)
				.collect(Collectors.toList());
	}

	// 🔹 Buscar produtos por nome (pesquisa parcial)
	public List<ProdutoDTO> buscarProdutosPorNome(String nome) {
		return produtoRepository.findByNomeContainingIgnoreCase(nome).stream().map(this::converterParaDTO)
				.collect(Collectors.toList());
	}

	// 🔹 Atualizar produto
	@Transactional
	public ProdutoDTO atualizarProduto(Long id, ProdutoDTO produtoDTO) {
		Produto produto = produtoRepository.findById(id)
				.orElseThrow(() -> new ProdutoNaoEncontradoException("Produto com ID " + id + " não encontrado!"));

		produto.setNome(produtoDTO.getNome());
		produto.setDescricao(produtoDTO.getDescricao());
		produto.setPrecoCompra(produtoDTO.getPrecoCompra());
		produto.setPrecoVenda(produtoDTO.getPrecoVenda());
		produto.setQuantidadeEstoque(produtoDTO.getQuantidadeEstoque());
		produto.setCategoria(produtoDTO.getCategoria());

		produtoRepository.save(produto);

		return converterParaDTO(produto);
	}

	// 🔹 Excluir um produto
	@Transactional
	public void excluirProduto(Long id) {
		if (!produtoRepository.existsById(id)) {
			throw new ProdutoNaoEncontradoException("Produto com ID " + id + " não encontrado!");
		}
		produtoRepository.deleteById(id);
	}

	// 🔹 Converter entidade para DTO
	private ProdutoDTO converterParaDTO(Produto produto) {
		return new ProdutoDTO(produto.getId(), produto.getNome(), produto.getDescricao(), produto.getPrecoCompra(),
				produto.getPrecoVenda(), produto.getQuantidadeEstoque(), produto.getCategoria(),
				produto.getDataCadastro());
	}
}