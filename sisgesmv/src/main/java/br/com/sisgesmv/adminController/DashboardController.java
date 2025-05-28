package br.com.sisgesmv.adminController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sisgesmv.dto.UsuarioDTO;
import br.com.sisgesmv.service.UsuarioService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @Autowired
    private UsuarioService usuarioService;

    @Value("${jwt.secret}") // Injete o secret do application.properties
    private String jwtSecret;

    @GetMapping("/usuario-logado")
    public ResponseEntity<UsuarioDTO> obterDadosUsuario(@RequestHeader("Authorization") String token) {
        String email = extrairEmailDoToken(token);
        UsuarioDTO usuario = usuarioService.buscarPorEmail(email);
        return ResponseEntity.ok(usuario);
    }

    private String extrairEmailDoToken(String token) {
        // Remove o prefixo "Bearer " se existir
        String jwtToken = token.replace("Bearer ", "");
        
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
        
        return claims.getSubject(); // Retorna o email do usuário
    }
}