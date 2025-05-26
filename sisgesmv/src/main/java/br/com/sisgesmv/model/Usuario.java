package br.com.sisgesmv.model;

import java.time.LocalDate;
import java.util.Objects;

import br.com.sisgesmv.enums.TipoUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "administradores")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Nome do usuário não pode ser vazio")
	@Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
	private String nome;

	@NotBlank(message = "E-mail é obrigatório")
	@Email(message = "E-mail inválido")
	@Column(unique = true)
	private String email;

	@NotBlank(message = "Senha não pode estar vazia")
	@Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "A senha deve conter pelo menos 1 letra maiúscula, 1 minúscula e 1 número")
	private String senha;

	@Past(message = "Data de nascimento deve ser no passado")
	private LocalDate dtNascimento;
	
	@NotBlank(message = "CPF é obrigatório")
	@Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos")
	@Column(unique = true)
	private String cpf;

	@Enumerated(EnumType.STRING)
	private TipoUsuario tipo;

	public Usuario() {		
		
	}

	public Usuario(Long id,
			@NotBlank(message = "Nome do usuário não pode ser vazio") @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres") String nome,
			@NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
			@NotBlank(message = "Senha não pode estar vazia") @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres") @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "A senha deve conter pelo menos 1 letra maiúscula, 1 minúscula e 1 número") String senha,
			@Past(message = "Data de nascimento deve ser no passado") LocalDate dtNascimento,
			@NotBlank(message = "CPF é obrigatório") @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos") String cpf,
			TipoUsuario tipo) {
		super();
		this.id = id;
		this.nome = nome;
		this.email = email;
		this.senha = senha;
		this.dtNascimento = dtNascimento;
		this.cpf = cpf;
		this.tipo = tipo;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public LocalDate getDtNascimento() {
		return dtNascimento;
	}

	public void setDtNascimento(LocalDate dtNascimento) {
		this.dtNascimento = dtNascimento;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public TipoUsuario getTipo() {
		return tipo;
	}

	public void setTipo(TipoUsuario tipo) {
		this.tipo = tipo;
	}

	@Override
	public String toString() {
		return "Usuario [id=" + id + ", nome=" + nome + ", email=" + email + ", senha=" + senha + ", dtNascimento="
				+ dtNascimento + ", cpf=" + cpf + ", tipo=" + tipo + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Usuario other = (Usuario) obj;
		return Objects.equals(id, other.id);
	}
	
}