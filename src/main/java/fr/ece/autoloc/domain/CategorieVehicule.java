package fr.ece.autoloc.domain;

/** Stocke en base sous forme de texte (@Enumerated(EnumType.STRING)) ; le libelle sert a l'affichage. */
public enum CategorieVehicule {
    CITADINE("Citadine"),
    BERLINE("Berline"),
    SUV("SUV"),
    UTILITAIRE("Utilitaire");

    private final String libelle;

    CategorieVehicule(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
