package com.markovitz.portfolioservice.exception;

public class PortfolioNotFoundException extends RuntimeException {
    public PortfolioNotFoundException(String message) { super(message); }
}
