package br.com.sisgesmv.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.sisgesmv.enums.CategoriaProduto;
import br.com.sisgesmv.model.Produto;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // 🔹 Buscar produtos por categoria
    List<Produto> findByCategoria(CategoriaProduto categoria);

    // 🔹 Buscar produtos por nome (caso precise filtrar por nome)
    List<Produto> findByNomeContainingIgnoreCase(String nome);

    // 🔹 Produtos mais vendidos por vendedor (CPF) usando **native query**
    @Query(value = """
            SELECT 
                p.id, 
                p.nome, 
                SUM(pp.quantidade) as quantidadeVendida
            FROM produtos p
            JOIN pedido_produto pp ON p.id = pp.produto_id
            JOIN pedidos ped ON pp.pedido_id = ped.id
            WHERE ped.cpf_vendedor = :cpfVendedor
            AND ped.data_pedido >= :dataInicio
            GROUP BY p.id, p.nome
            ORDER BY quantidadeVendida DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> findTop5MaisVendidosPorVendedor(
        @Param("cpfVendedor") String cpfVendedor,
        @Param("dataInicio") LocalDate dataInicio);

    // 🔹 Produtos mais vendidos por vendedor usando **JPQL**
    @Query("""
            SELECT p, SUM(pp.quantidade) as quantidadeVendida
            FROM Produto p
            JOIN PedidoProduto pp ON pp.produto = p
            JOIN Pedido ped ON pp.pedido = ped
            WHERE ped.cpfVendedor = :cpfVendedor
            AND ped.dataPedido >= :dataInicio
            GROUP BY p
            ORDER BY quantidadeVendida DESC
            """)
    List<Object[]> findTop5MaisVendidosPorVendedorJpql(@Param("cpfVendedor") String cpfVendedor,
                                                       @Param("dataInicio") LocalDate dataInicio);
}