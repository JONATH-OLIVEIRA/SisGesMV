package br.com.sisgesmv.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.sisgesmv.enums.CategoriaProduto;
import br.com.sisgesmv.model.Produto;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // 🔹 Buscar produtos por categoria
    List<Produto> findByCategoria(CategoriaProduto categoria);

    // 🔹 Buscar produtos por nome (caso precise filtrar por nome)
    List<Produto> findByNomeContainingIgnoreCase(String nome);
}