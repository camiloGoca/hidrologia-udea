package edu.udea.hidrologia.question.service;

public class UnsupportedQuestionStatusFilterException extends RuntimeException {

    public UnsupportedQuestionStatusFilterException() {
        super("El estado solicitado no esta disponible para esta fase.");
    }
}
