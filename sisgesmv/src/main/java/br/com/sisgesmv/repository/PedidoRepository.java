package br.com.sisgesmv.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.sisgesmv.enums.StatusPedido;
import br.com.sisgesmv.model.Pedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // 🔹 Buscar pedidos por status
    List<Pedido> findByStatus(StatusPedido status);

    // 🔹 Buscar pedidos por CPF do vendedor
    List<Pedido> findByCpfVendedor(String cpfVendedor);

    // 🔹 Buscar pedidos por CPF do vendedor e status
    List<Pedido> findByCpfVendedorAndStatus(String cpfVendedor, StatusPedido status);

    // 🔹 Buscar pedidos por CPF do gerente
    List<Pedido> findByCpfGerente(String cpfGerente);

    // 🔹 Buscar pedidos por CPF do separador
    List<Pedido> findByCpfSeparador(String cpfSeparador);

    // 🔹 Buscar pedido por código único de entrega
    Optional<Pedido> findByCodigoEntrega(String codigoEntrega);
    
    // 🔹 Conta pedidos por CPF do vendedor
    Long countByCpfVendedor(String cpfVendedor);

    List<Pedido> findByCpfVendedorOrderByDataPedidoDesc(String cpfVendedor);
    // 🔹 Conta pedidos por CPF do vendedor e status
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.cpfVendedor = :cpfVendedor AND p.status = :status")
    Long countByCpfVendedorAndStatus(@Param("cpfVendedor") String cpfVendedor, 
                                    @Param("status") StatusPedido status);

    // 🔹 Conta pedidos por CPF do vendedor e lista de status
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.cpfVendedor = :cpfVendedor AND p.status IN :statusList")
    Long countByCpfVendedorAndStatusIn(@Param("cpfVendedor") String cpfVendedor, 
                                     @Param("statusList") List<StatusPedido> statusList);

    // 🔹 Busca últimos 5 pedidos do vendedor ordenados por data
    @Query("SELECT p FROM Pedido p WHERE p.cpfVendedor = :cpfVendedor ORDER BY p.dataPedido DESC")
    List<Pedido> findTop5ByCpfVendedorOrderByDataPedidoDesc(@Param("cpfVendedor") String cpfVendedor);

    // 🔹 Pedidos agrupados por mês (últimos N meses) para o vendedor
    @Query(value = """
        SELECT 
            TO_CHAR(p.data_pedido, 'Mon YYYY') as mes,
            COUNT(p.id) as total
        FROM pedidos p
        WHERE p.cpf_vendedor = :cpfVendedor
        AND p.data_pedido >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL ':months months')
        GROUP BY mes
        ORDER BY MIN(p.data_pedido)
        """, nativeQuery = true)
    List<Object[]> getPedidosPorMes(@Param("cpfVendedor") String cpfVendedor, 
                                  @Param("months") int months);
    
    // 🔹 Valor total de vendas por vendedor
    @Query("SELECT COALESCE(SUM(p.valorTotal), 0) FROM Pedido p WHERE p.cpfVendedor = :cpfVendedor")
    BigDecimal sumValorTotalByCpfVendedor(@Param("cpfVendedor") String cpfVendedor);
    
    // 🔹 Valor total de vendas por vendedor em um período
    @Query("SELECT COALESCE(SUM(p.valorTotal), 0) FROM Pedido p WHERE p.cpfVendedor = :cpfVendedor " +
           "AND p.dataPedido BETWEEN :dataInicio AND :dataFim")
    BigDecimal sumValorTotalByCpfVendedorAndPeriodo(@Param("cpfVendedor") String cpfVendedor,
                                                  @Param("dataInicio") LocalDate dataInicio,
                                                  @Param("dataFim") LocalDate dataFim);
}