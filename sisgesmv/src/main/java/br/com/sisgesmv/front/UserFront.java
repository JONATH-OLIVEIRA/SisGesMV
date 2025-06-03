package br.com.sisgesmv.front;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserFront {
	
	@GetMapping("/user/dashboard")
	public String adminDashboard() {
		return "user/dashboard"; // Template em src/main/resources/templates/user/dashboard.html
	}

	@GetMapping("/user/pedidos")
	public String userPedidos() {
		return "user/pedidos"; // Template em src/main/resources/templates/user/pedidos.html
	}
	
	 // 🔹 Novo endpoint para "Meu Perfil"
    @GetMapping("/user/userPerfil")
    public String adminPerfil() {
        return "user/userPerfil"; // Template em src/main/resources/templates/admin/perfil.html
    }

}
