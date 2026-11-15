package fr.ece.autoloc.domain;

/** Stocke en base sous forme de texte (@Enumerated(EnumType.STRING)) ; le libelle sert a l'affichage. */
public enum StatutReservation {
    EN_ATTENTE("En attente"),
    CONFIRMEE("Confirmée"),
    ANNULEE("Annulée"),
    TERMINEE("Terminée");

    private final String libelle;

    StatutReservation(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
