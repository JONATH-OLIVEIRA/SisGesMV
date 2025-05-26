package br.com.sisgesmv.exception;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException(String mensagem) {
        super(mensagem);
    }
}
