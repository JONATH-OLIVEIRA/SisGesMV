package br.com.sisgesmv.front;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminFront {

    @GetMapping("/")
    public String homePage() {
        return "index"; // Corresponde a src/main/resources/templates/index.html
    }
    
    @GetMapping("/auth/login")
    public String loginPage() {
        return "login"; // Corresponde a src/main/resources/templates/login.html
    }
    
    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard"; // Template em src/main/resources/templates/admin/dashboard.html
    }
    
    @GetMapping("/admin/gerenciar-usuarios")
    public String gerenciarUsuarios() {
        return "admin/gerenciar-usuarios"; 
    }
    @GetMapping("/admin/gerenciar-produtos")
    public String gerenciarProdutos() {
        return "admin/gerenciar-produtos"; 
    }
    @GetMapping("/admin/gerenciar-pedidos")
    public String gerenciarPedidos() {
        return "admin/gerenciar-pedidos"; 
    }
    // 🔹 Novo endpoint para "Meu Perfil"
    @GetMapping("/admin/adminPerfil")
    public String adminPerfil() {
        return "admin/adminPerfil"; // Template em src/main/resources/templates/admin/perfil.html
    }
}