package br.com.sisgesmv.dto;

import br.com.sisgesmv.enums.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class UsuarioDTO {

    @NotBlank(message = "Nome do usuário não pode estar vazio")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha não pode estar vazia")
    @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "A senha deve conter pelo menos 1 letra maiúscula, 1 minúscula e 1 número")
    private String senha;

    private LocalDate dtNascimento;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos")
    private String cpf;

    private TipoUsuario tipo; // Define se será ADMIN ou USER

    public UsuarioDTO() {}

    public UsuarioDTO(String nome, String email, String senha, LocalDate dtNascimento, String cpf, TipoUsuario tipo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.dtNascimento = dtNascimento;
        this.cpf = cpf;
        this.tipo = tipo;
    }

    // Getters e Setters

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
}