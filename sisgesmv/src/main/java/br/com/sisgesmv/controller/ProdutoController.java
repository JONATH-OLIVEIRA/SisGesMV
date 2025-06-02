package br.com.sisgesmv.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sisgesmv.dto.ProdutoDTO;
import br.com.sisgesmv.enums.CategoriaProduto;
import br.com.sisgesmv.service.ProdutoService;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    public ResponseEntity<ProdutoDTO> cadastrarProduto(@RequestBody ProdutoDTO produtoDTO) {
        ProdutoDTO produtoCadastrado = produtoService.cadastrarProduto(produtoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoCadastrado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDTO> buscarProdutoPorId(@PathVariable Long id) {
        ProdutoDTO produtoDTO = produtoService.buscarProdutoPorId(id);
        return ResponseEntity.ok(produtoDTO);
    }

    @GetMapping
    public ResponseEntity<List<ProdutoDTO>> listarTodosProdutos() {
        List<ProdutoDTO> produtos = produtoService.listarTodosProdutos();
        return ResponseEntity.ok(produtos);
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ProdutoDTO>> listarProdutosPorCategoria(
            @PathVariable CategoriaProduto categoria) {
        List<ProdutoDTO> produtos = produtoService.listarProdutosPorCategoria(categoria);
        return ResponseEntity.ok(produtos);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ProdutoDTO>> buscarProdutosPorNome(
            @RequestParam String nome) {
        List<ProdutoDTO> produtos = produtoService.buscarProdutosPorNome(nome);
        return ResponseEntity.ok(produtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoDTO> atualizarProduto(
            @PathVariable Long id, @RequestBody ProdutoDTO produtoDTO) {
        ProdutoDTO produtoAtualizado = produtoService.atualizarProduto(id, produtoDTO);
        return ResponseEntity.ok(produtoAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirProduto(@PathVariable Long id) {
        produtoService.excluirProduto(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/disponiveis")
    public ResponseEntity<List<ProdutoDTO>> listarProdutosDisponiveis() {
        List<ProdutoDTO> produtos = produtoService.listarTodosProdutos().stream()
            .filter(p -> p.getQuantidadeEstoque() > 0)
            .collect(Collectors.toList());
        return ResponseEntity.ok(produtos);
    }
}