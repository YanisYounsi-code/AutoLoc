package fr.ece.autoloc.web.controller;

import fr.ece.autoloc.exception.RegleMetierException;
import fr.ece.autoloc.exception.RequeteInvalideException;
import fr.ece.autoloc.exception.RessourceIntrouvableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pour l'API REST uniquement (package web.controller) : transforme les exceptions en JSON standard
 * "Problem Details" (RFC 9457), par exemple { "status": 409, "detail": "Ce véhicule est déjà loué..." }.
 * Les controleurs restent ainsi sans try/catch.
 */
@RestControllerAdvice(basePackageClasses = GestionErreursApi.class)
public class GestionErreursApi {

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ProblemDetail introuvable(RessourceIntrouvableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RegleMetierException.class)
    public ProblemDetail regleMetier(RegleMetierException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RequeteInvalideException.class)
    public ProblemDetail requeteInvalide(RequeteInvalideException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** @Valid a echoue : on renvoie la liste des champs en erreur. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException e) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        for (FieldError erreur : e.getBindingResult().getFieldErrors()) {
            erreurs.putIfAbsent(erreur.getField(), erreur.getDefaultMessage());
        }
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                erreurs.values().stream().findFirst().orElse("Données invalides."));
        probleme.setProperty("erreurs", erreurs);
        return probleme;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail formatInvalide(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Format de données invalide (date, nombre ou valeur de liste incorrecte).");
    }
}
