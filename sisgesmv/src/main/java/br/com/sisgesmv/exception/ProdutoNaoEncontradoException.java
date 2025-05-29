package br.com.sisgesmv.exception;

public class ProdutoNaoEncontradoException extends RuntimeException {
	public ProdutoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}

}

