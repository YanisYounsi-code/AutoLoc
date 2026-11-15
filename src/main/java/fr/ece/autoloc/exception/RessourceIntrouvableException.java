package fr.ece.autoloc.exception;

/** L'element demande n'existe pas : renvoye en HTTP 404. */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
