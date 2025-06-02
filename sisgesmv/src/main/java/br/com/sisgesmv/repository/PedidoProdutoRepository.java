package br.com.sisgesmv.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.sisgesmv.model.Pedido;
import br.com.sisgesmv.model.PedidoProduto;

@Repository
public interface PedidoProdutoRepository extends JpaRepository<PedidoProduto, Long>{
	 List<PedidoProduto> findByPedido(Pedido pedido);
}
