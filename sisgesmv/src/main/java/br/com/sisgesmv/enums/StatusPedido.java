package br.com.sisgesmv.enums;

public enum StatusPedido {
    PENDENTE,            // Pedido aguardando aprovação
    APROVADO,            // Pedido aprovado pelo gerente
    SEPARACAO,           // Pedido em separação para envio
    PRONTO_PARA_ENTREGA, // Pedido pronto para ser entregue ao cliente
    ENTREGUE             // Pedido entregue ao cliente final
}
