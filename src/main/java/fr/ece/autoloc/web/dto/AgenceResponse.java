package fr.ece.autoloc.web.dto;

import fr.ece.autoloc.domain.Agence;

public record AgenceResponse(Long id, String nom, String ville, String adresse, String telephone) {

    public static AgenceResponse from(Agence a) {
        return new AgenceResponse(a.getIdAgence(), a.getNom(), a.getVille(), a.getAdresse(), a.getTelephone());
    }
}
