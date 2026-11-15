package fr.ece.autoloc.exception;

/** Les donnees recues sont incoherentes (date de fin avant la date de debut...) : HTTP 400. */
public class RequeteInvalideException extends RuntimeException {

    public RequeteInvalideException(String message) {
        super(message);
    }
}
