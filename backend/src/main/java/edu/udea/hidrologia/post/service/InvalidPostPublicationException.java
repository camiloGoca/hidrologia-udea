package edu.udea.hidrologia.post.service;

public class InvalidPostPublicationException extends RuntimeException {

    public InvalidPostPublicationException(String message) {
        super(message);
    }
}
