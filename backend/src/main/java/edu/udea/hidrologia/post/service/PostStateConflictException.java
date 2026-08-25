package edu.udea.hidrologia.post.service;

public class PostStateConflictException extends RuntimeException {

    public PostStateConflictException(String message) {
        super(message);
    }
}
