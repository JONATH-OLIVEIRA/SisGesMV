package br.com.sisgesmv.repository;

import br.com.sisgesmv.model.Pedido;
import br.com.sisgesmv.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

	// 🔹 Buscar pedidos por status
	List<Pedido> findByStatus(StatusPedido status);

	// 🔹 Buscar pedidos por CPF do vendedor
	List<Pedido> findByCpfVendedor(String cpfVendedor);

	// 🔹 Buscar pedidos por CPF do gerente
	List<Pedido> findByCpfGerente(String cpfGerente);

	// 🔹 Buscar pedidos por CPF do separador
	List<Pedido> findByCpfSeparador(String cpfSeparador);

	// 🔹 Buscar pedido por código único de entrega
	Optional<Pedido> findByCodigoEntrega(String codigoEntrega);
}