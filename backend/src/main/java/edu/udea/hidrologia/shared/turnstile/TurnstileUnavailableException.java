package edu.udea.hidrologia.shared.turnstile;

public class TurnstileUnavailableException extends RuntimeException {

    public TurnstileUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public TurnstileUnavailableException(String message) {
        super(message);
    }
}
