package br.com.sisgesmv.adminController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sisgesmv.enums.StatusPedido;
import br.com.sisgesmv.exception.PedidoNaoEncontradoException;
import br.com.sisgesmv.model.Pedido;
import br.com.sisgesmv.model.Usuario;
import br.com.sisgesmv.repository.PedidoProdutoRepository;
import br.com.sisgesmv.repository.PedidoRepository;
import br.com.sisgesmv.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/user/dashboard")
public class UserDashboardController {

    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
   

    @GetMapping("/data")
    public ResponseEntity<?> getDashboardData(Authentication authentication) {
        try {
            // Verifica se há autenticação
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Não autorizado",
                    "message", "Usuário não autenticado"
                ));
            }

            // Obtém o email do usuário autenticado (do token JWT)
            String email = ((UserDetails) authentication.getPrincipal()).getUsername();
            
            // Busca o usuário completo no banco de dados por EMAIL
            Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para o email: " + email));
            
            // Busca os pedidos do usuário usando o CPF do usuário encontrado
            List<Pedido> pedidos = pedidoRepository.findByCpfVendedorOrderByDataPedidoDesc(usuario.getCpf());
            
            // Calcula as estatísticas
            long totalPedidos = pedidos.size();
            long pedidosEntregues = pedidos.stream()
                .filter(p -> p.getStatus() == StatusPedido.ENTREGUE)
                .count();
            long pedidosPendentes = totalPedidos - pedidosEntregues;
            
            // Agrupa pedidos por status para o gráfico
            Map<String, Long> pedidosPorStatus = pedidos.stream()
                .collect(Collectors.groupingBy(
                    p -> p.getStatus().name(),
                    Collectors.counting()
                ));
            
            // Cria a resposta
            Map<String, Object> response = new HashMap<>();
            response.put("nomeUsuario", usuario.getNome());
            response.put("cargoUsuario", usuario.getTipo());
            response.put("totalPedidos", totalPedidos);
            response.put("pedidosEntregues", pedidosEntregues);
            response.put("pedidosPendentes", pedidosPendentes);
            response.put("pedidosPorStatus", pedidosPorStatus);
            response.put("ultimosPedidos", pedidos.stream()
                .limit(10)
                .map(this::toPedidoDTO)
                .collect(Collectors.toList()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Erro ao buscar dados do dashboard",
                    "message", e.getMessage()
                ));
        }
    }

    private PedidoDTO toPedidoDTO(Pedido pedido) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setDataPedido(pedido.getDataPedido());
        dto.setValorTotal(pedido.getValorTotal());
        dto.setStatus(pedido.getStatus().name());
        dto.setCodigoEntrega(pedido.getCodigoEntrega());
        return dto;
    }

    public static class PedidoDTO {
        private Long id;
        private LocalDate dataPedido;
        private BigDecimal valorTotal;
        private String status;
        private String codigoEntrega;

        // Getters e Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public LocalDate getDataPedido() { return dataPedido; }
        public void setDataPedido(LocalDate dataPedido) { this.dataPedido = dataPedido; }
        
        public BigDecimal getValorTotal() { return valorTotal; }
        public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getCodigoEntrega() { return codigoEntrega; }
        public void setCodigoEntrega(String codigoEntrega) { this.codigoEntrega = codigoEntrega; }
    }
   
}