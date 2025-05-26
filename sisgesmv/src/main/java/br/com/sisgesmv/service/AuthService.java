package br.com.sisgesmv.service;

import java.util.List;
import java.util.Objects;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import br.com.sisgesmv.model.Usuario;
import br.com.sisgesmv.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;


@Service
public class AuthService implements UserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UsuarioRepository usuarioRepository, 
                      PasswordEncoder passwordEncoder,
                      JwtTokenService jwtTokenService) {
        
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "UsuarioRepository não pode ser nulo");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "PasswordEncoder não pode ser nulo");
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService, "JwtTokenService não pode ser nulo");
    }

    public String login(String email, String senha) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(senha)) {
            throw new IllegalArgumentException("Email e senha são obrigatórios");
        }

        Usuario usuario = usuarioRepository.findByEmail(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new JwtException("Credenciais inválidas");
        }

        return jwtTokenService.gerarToken(usuario.getEmail(), usuario.getTipo().name());
    }

    public boolean validarToken(String token) {
        return jwtTokenService.validarToken(token);
    }

    public String extrairEmail(String token) {
        return jwtTokenService.extrairEmail(token);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (!StringUtils.hasText(email)) {
            throw new UsernameNotFoundException("Email não pode ser vazio");
        }

        Usuario usuario = usuarioRepository.findByEmail(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        return new User(
                usuario.getEmail(),
                usuario.getSenha(),
                List.of(new SimpleGrantedAuthority(ROLE_PREFIX + usuario.getTipo()))
        );
    }
}