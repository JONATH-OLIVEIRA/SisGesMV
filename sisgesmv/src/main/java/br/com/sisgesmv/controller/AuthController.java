package br.com.sisgesmv.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sisgesmv.enums.TipoUsuario;
import br.com.sisgesmv.exception.SenhaIncorretaException;
import br.com.sisgesmv.exception.UsuarioNaoEncontradoException;
import br.com.sisgesmv.service.UsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String email,
            @RequestParam String senha,
            HttpServletResponse response) {
        
        try {
            if (email == null || email.trim().isEmpty() || senha == null || senha.trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Email e senha são obrigatórios");
            }

            Map<String, String> authInfo = usuarioService.autenticarUsuario(email, senha);
            
            configureAuthCookie(response, authInfo.get("token"));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "redirect", determineRedirectUrl(authInfo.get("role")),
                "token", authInfo.get("token"),
                "userId", authInfo.get("id")
            ));

        } catch (UsuarioNaoEncontradoException | SenhaIncorretaException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            logger.error("Erro durante login", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor");
        }
    }

    private void configureAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);
    }

    private String determineRedirectUrl(String role) {
        return switch (TipoUsuario.valueOf(role)) {
            case ADMIN -> "/admin/dashboard";
            case USER -> "/user/dashboard";
        };
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "success", false,
            "error", message
        ));
    }
}