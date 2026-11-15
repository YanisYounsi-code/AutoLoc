package fr.ece.autoloc.exception;

/**
 * Une regle metier interdit l'operation (vehicule deja reserve, contrat annule...) :
 * renvoye en HTTP 409 Conflict.
 */
public class RegleMetierException extends RuntimeException {

    public RegleMetierException(String message) {
        super(message);
    }
}
