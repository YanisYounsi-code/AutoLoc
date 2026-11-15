package fr.ece.autoloc.domain;

/** Stocke en base sous forme de texte (@Enumerated(EnumType.STRING)) ; le libelle sert a l'affichage. */
public enum StatutVehicule {
    DISPONIBLE("Disponible"),
    LOUE("Loué"),
    MAINTENANCE("En maintenance");

    private final String libelle;

    StatutVehicule(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
