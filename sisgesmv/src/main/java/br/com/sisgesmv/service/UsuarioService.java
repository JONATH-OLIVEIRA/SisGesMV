package br.com.sisgesmv.service;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sisgesmv.dto.UsuarioDTO;
import br.com.sisgesmv.enums.TipoUsuario;
import br.com.sisgesmv.exception.EmailJaCadastradoException;
import br.com.sisgesmv.exception.SenhaIncorretaException;
import br.com.sisgesmv.exception.UsuarioNaoEncontradoException;
import br.com.sisgesmv.model.Usuario;
import br.com.sisgesmv.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String jwtSecret;
    private final long jwtExpiration;

    public UsuarioService(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${jwt.secret}") String jwtSecret,
                        @Value("${jwt.expiration}") long jwtExpiration) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
    }

    @Transactional
    public Usuario cadastrarUsuario(UsuarioDTO usuarioDTO) {
        if (usuarioRepository.existsByCpf(usuarioDTO.getCpf())) {
            throw new EmailJaCadastradoException("CPF já cadastrado!");
        }

        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            throw new EmailJaCadastradoException("E-mail já cadastrado!");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(usuarioDTO.getNome());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));
        usuario.setDtNascimento(usuarioDTO.getDtNascimento());
        usuario.setCpf(usuarioDTO.getCpf());
        usuario.setTipo(usuarioDTO.getTipo() != null ? usuarioDTO.getTipo() : TipoUsuario.USER);

        return usuarioRepository.save(usuario);
    }

    public Map<String, String> autenticarUsuario(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado!"));

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new SenhaIncorretaException("Credenciais inválidas!");
        }

        String token = gerarTokenJwt(usuario);
        return Map.of(
            "token", token,
            "role", usuario.getTipo().name(),
            "id", usuario.getId().toString()
        );
    }

    private String gerarTokenJwt(Usuario usuario) {
        return Jwts.builder()
            .subject(usuario.getEmail())
            .claim("id", usuario.getId())
            .claim("role", usuario.getTipo().name())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
            .compact();
    }

    public UsuarioDTO buscarUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado!"));

        return new UsuarioDTO(
            usuario.getNome(),
            usuario.getEmail(),
            null,
            usuario.getDtNascimento(),
            usuario.getCpf(),
            usuario.getTipo()
        );
    }
}