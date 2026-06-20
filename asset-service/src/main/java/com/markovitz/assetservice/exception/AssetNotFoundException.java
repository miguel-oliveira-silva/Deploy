package com.markovitz.assetservice.exception;

/** Lançada quando um ativo não é encontrado pelo ticker */
public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException(String message) {
        super(message);
    }
}
