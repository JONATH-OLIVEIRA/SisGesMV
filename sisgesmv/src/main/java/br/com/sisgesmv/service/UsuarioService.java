package br.com.sisgesmv.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
		return Map.of("token", token, "role", usuario.getTipo().name(), "id", usuario.getId().toString());
	}

	private String gerarTokenJwt(Usuario usuario) {
		return Jwts.builder().subject(usuario.getEmail()).claim("id", usuario.getId())
				.claim("role", usuario.getTipo().name()).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + jwtExpiration))
				.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret))).compact();
	}

	public UsuarioDTO buscarUsuarioPorId(Long id) {
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado!"));

		return new UsuarioDTO(usuario.getNome(), usuario.getEmail(), usuario.getSenha(), usuario.getDtNascimento(),
				usuario.getCpf(), usuario.getTipo());
	}

	// ✅ Método para atualizar dados do administrador
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
	        usuario.setSenha(passwordEncoder.encode(usuarioDTO.getSenha())); // 🔑 Senha criptografada
	    }

	    usuarioRepository.save(usuario);

	    return new UsuarioDTO(
	        usuario.getNome(), 
	        usuario.getEmail(), 
	        null, // 🔹 A senha nunca deve ser retornada diretamente
	        usuario.getDtNascimento(), 
	        usuario.getCpf(), 
	        usuario.getTipo()
	    );
	}

	// ✅ Método para excluir um administrador
	@Transactional
	public void excluirUsuario(Long id) {
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado!"));

		usuarioRepository.delete(usuario);
	}

	// ✅ Método para listar todos os administradores
	public List<UsuarioDTO> listarAdministradores() {
		List<Usuario> administradores = usuarioRepository.findAll().stream()
				.filter(usuario -> usuario.getTipo() == TipoUsuario.ADMIN) // Filtra apenas administradores
				.collect(Collectors.toList());

		return administradores.stream().map(admin -> new UsuarioDTO(admin.getNome(), admin.getEmail(), null,
				admin.getDtNascimento(), admin.getCpf(), admin.getTipo())).collect(Collectors.toList());
	}

	// ✅ Método para buscar um usuario por email
	public UsuarioDTO buscarPorEmail(String email) {
		Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow(
				() -> new UsuarioNaoEncontradoException("Usuário com email " + email + " não encontrado!"));

		// Retorna um DTO para ocultar a senha
		return new UsuarioDTO(usuario.getNome(), usuario.getEmail(), null, // Senha não deve ser retornada
				usuario.getDtNascimento(), usuario.getCpf(), usuario.getTipo());
	}
}
