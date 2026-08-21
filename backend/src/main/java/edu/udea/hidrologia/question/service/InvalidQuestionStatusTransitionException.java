package edu.udea.hidrologia.question.service;

public class InvalidQuestionStatusTransitionException extends RuntimeException {

    public InvalidQuestionStatusTransitionException() {
        super("La transicion de estado solicitada no esta permitida.");
    }
}
