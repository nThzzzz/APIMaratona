package com.APImaratona.Maratona.Exceptions;

// Lancada quando o login falha (usuario/senha errados) ou quando um token
// valido tenta agir sobre uma conta que nao e a dele.
public class AutenticacaoInvalidaException extends RuntimeException {
    public AutenticacaoInvalidaException(String message) {
        super(message);
    }
}
