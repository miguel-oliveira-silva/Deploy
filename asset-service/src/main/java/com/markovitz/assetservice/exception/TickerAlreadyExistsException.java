package com.markovitz.assetservice.exception;

/** Lançada quando se tenta cadastrar um ticker que já existe */
public class TickerAlreadyExistsException extends RuntimeException {
    public TickerAlreadyExistsException(String message) {
        super(message);
    }
}
