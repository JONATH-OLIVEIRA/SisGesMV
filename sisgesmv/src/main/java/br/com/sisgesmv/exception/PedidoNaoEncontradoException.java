package br.com.sisgesmv.exception;

public class PedidoNaoEncontradoException extends RuntimeException {
	public PedidoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}
}
