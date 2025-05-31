package br.com.sisgesmv.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import jakarta.persistence.EntityNotFoundException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String jwtSecret;
    private final long jwtExpiration;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          @Value("${jwt.secret}") String jwtSecret, @Value("${jwt.expiration}") long jwtExpiration) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
    }
    
    public boolean isGerenteOrAdmin(String cpf) {
        Usuario funcionario = usuarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new EntityNotFoundException("Funcionário não encontrado"));
        
        return funcionario.getTipo().equals(TipoUsuario.GERENTE) || 
               funcionario.getTipo().equals(TipoUsuario.ADMIN);
    }

    // 🔹 Cadastrar um novo usuário
    @Transactional
    public UsuarioDTO cadastrarUsuario(UsuarioDTO usuarioDTO) {
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

        usuario = usuarioRepository.save(usuario);

        return new UsuarioDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            null, // 🔹 Não retornamos a senha
            usuario.getDtNascimento(),
            usuario.getCpf(),
            usuario.getTipo()
        );
    }

    // 🔹 Autenticar usuário e gerar token JWT
    public Map<String, String> autenticarUsuario(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado!"));

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new SenhaIncorretaException("Credenciais inválidas!");
        }

        String token = gerarTokenJwt(usuario);
        return Map.of("token", token, "role", usuario.getTipo().name(), "id", usuario.getId().toString());
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

    // 🔹 Buscar um usuário por ID
    public UsuarioDTO buscarUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado!"));

        return new UsuarioDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            null, // 🔹 Não retornamos a senha
            usuario.getDtNascimento(),
            usuario.getCpf(),
            usuario.getTipo()
        );
    }

    // 🔹 Atualizar um usuário
    @Transactional
    public UsuarioDTO atualizarUsuario(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado!"));

        usuario.setNome(usuarioDTO.getNome());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setDtNascimento(usuarioDTO.getDtNascimento());
        usuario.setCpf(usuarioDTO.getCpf());
        usuario.setTipo(usuarioDTO.getTipo() != null ? usuarioDTO.getTipo() : usuario.getTipo());

        // 🔹 Só alteramos a senha se for informada no request
        if (usuarioDTO.getSenha() != null && !usuarioDTO.getSenha().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));
        }

        usuarioRepository.save(usuario);

        return new UsuarioDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            null, // 🔹 A senha nunca deve ser retornada diretamente
            usuario.getDtNascimento(),
            usuario.getCpf(),
            usuario.getTipo()
        );
    }

    // 🔹 Excluir usuário
    @Transactional
    public void excluirUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new UsuarioNaoEncontradoException("Usuário não encontrado!");
        }
        usuarioRepository.deleteById(id);
    }

    // 🔹 Listar todos os usuários (ADMIN e USER)
    @Transactional
    public List<UsuarioDTO> listarTodosUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();

        return usuarios.stream().map(usuario -> new UsuarioDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            null, // 🔹 Não retornamos a senha
            usuario.getDtNascimento(),
            usuario.getCpf(),
            usuario.getTipo()
        )).collect(Collectors.toList());
    }

    // 🔹 Listar apenas administradores
    public List<UsuarioDTO> listarAdministradores() {
        List<Usuario> administradores = usuarioRepository.findByTipo(TipoUsuario.ADMIN);
        return administradores.stream().map(admin -> new UsuarioDTO(
            admin.getId(),
            admin.getNome(),
            admin.getEmail(),
            null,
            admin.getDtNascimento(),
            admin.getCpf(),
            admin.getTipo()
        )).collect(Collectors.toList());
    }

    // 🔹 Buscar usuário por email
    public UsuarioDTO buscarPorEmail(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) {
            throw new UsuarioNaoEncontradoException("Usuário com email " + email + " não encontrado!");
        }
        Usuario usuario = usuarioOpt.get();
        return new UsuarioDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            null, // Senha não deve ser retornada
            usuario.getDtNascimento(),
            usuario.getCpf(),
            usuario.getTipo()
        );
    }
}