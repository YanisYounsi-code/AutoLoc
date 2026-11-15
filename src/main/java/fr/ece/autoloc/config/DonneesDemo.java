package fr.ece.autoloc.config;

import fr.ece.autoloc.domain.*;
import fr.ece.autoloc.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Remplit une base VIDE avec des donnees realistes au premier demarrage (CommandLineRunner) :
 * 4 agences, 4 employes, 8 equipements, 48 vehicules, 150 clients et plus d'un millier de reservations.
 * Ce volume rend les mesures de performance significatives. Desactivable avec autoloc.demo.enabled=false.
 *
 * Les cascades du Workshop 2 sont utilisees ici : sauvegarder un vehicule enregistre ses maintenances
 * (PERSIST), sauvegarder une reservation enregistre son contrat (ALL) qui enregistre ses paiements (ALL).
 */
@Component
@ConditionalOnProperty(name = "autoloc.demo.enabled", havingValue = "true")
public class DonneesDemo implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DonneesDemo.class);

    private record Modele(String marque, String modele, CategorieVehicule categorie, int tarif) {
    }

    private static final List<Modele> MODELES = List.of(
            new Modele("Peugeot", "208", CategorieVehicule.CITADINE, 39),
            new Modele("Renault", "Clio", CategorieVehicule.CITADINE, 38),
            new Modele("Toyota", "Yaris", CategorieVehicule.CITADINE, 41),
            new Modele("Fiat", "500", CategorieVehicule.CITADINE, 35),
            new Modele("Volkswagen", "Polo", CategorieVehicule.CITADINE, 42),
            new Modele("Peugeot", "508", CategorieVehicule.BERLINE, 69),
            new Modele("Skoda", "Octavia", CategorieVehicule.BERLINE, 62),
            new Modele("Toyota", "Corolla", CategorieVehicule.BERLINE, 59),
            new Modele("BMW", "Série 3", CategorieVehicule.BERLINE, 89),
            new Modele("Tesla", "Model 3", CategorieVehicule.BERLINE, 95),
            new Modele("Peugeot", "3008", CategorieVehicule.SUV, 75),
            new Modele("Renault", "Austral", CategorieVehicule.SUV, 72),
            new Modele("Dacia", "Duster", CategorieVehicule.SUV, 55),
            new Modele("Toyota", "RAV4", CategorieVehicule.SUV, 85),
            new Modele("Kia", "Sportage", CategorieVehicule.SUV, 78),
            new Modele("Renault", "Trafic", CategorieVehicule.UTILITAIRE, 79),
            new Modele("Citroën", "Jumpy", CategorieVehicule.UTILITAIRE, 74),
            new Modele("Ford", "Transit", CategorieVehicule.UTILITAIRE, 82),
            new Modele("Renault", "Master", CategorieVehicule.UTILITAIRE, 89));

    private static final List<String> PRENOMS = List.of("Camille", "Lea", "Hugo", "Louis", "Emma", "Jules",
            "Ines", "Adam", "Chloe", "Nathan", "Sarah", "Yanis", "Manon", "Rayan", "Lina", "Tom", "Jade", "Noah",
            "Zoe", "Ethan", "Alice", "Mathis", "Nora", "Samuel", "Ambre");
    private static final List<String> NOMS = List.of("Martin", "Bernard", "Dubois", "Thomas", "Robert",
            "Richard", "Petit", "Durand", "Leroy", "Moreau", "Simon", "Laurent", "Lefebvre", "Michel", "Garcia",
            "David", "Bertrand", "Roux", "Vincent", "Fournier", "Morel", "Girard", "Andre", "Mercier", "Benali");

    private final AgenceRepository agenceRepository;
    private final EquipementRepository equipementRepository;
    private final VehiculeRepository vehiculeRepository;
    private final EmployeRepository employeRepository;
    private final ClientRepository clientRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;
    private final String emailUser;

    public DonneesDemo(AgenceRepository agenceRepository, EquipementRepository equipementRepository,
                       VehiculeRepository vehiculeRepository, EmployeRepository employeRepository,
                       ClientRepository clientRepository, ReservationRepository reservationRepository,
                       Clock clock, @Value("${autoloc.securite.user.identifiant}") String emailUser) {
        this.agenceRepository = agenceRepository;
        this.equipementRepository = equipementRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.employeRepository = employeRepository;
        this.clientRepository = clientRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
        this.emailUser = emailUser;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (vehiculeRepository.count() > 0) {
            return; // base deja remplie : on ne touche a rien
        }
        LOG.info("Base vide : insertion des données de démonstration...");
        Random hasard = new Random(42); // graine fixe : memes donnees a chaque installation
        LocalDate aujourdhui = LocalDate.now(clock);

        List<Agence> agences = creerAgences();
        List<Equipement> equipements = creerEquipements();
        creerPersonnel(agences);
        List<Client> clients = creerClients(hasard, aujourdhui);
        List<Vehicule> vehicules = creerVehicules(agences, equipements, hasard, aujourdhui);
        int nbReservations = creerReservations(vehicules, clients, hasard, aujourdhui);

        LOG.info("Données de démonstration prêtes : {} véhicules, {} clients, {} réservations.",
                vehicules.size(), clients.size(), nbReservations);
        LOG.info("Le compte {} loue au nom du client Camille Martin.", emailUser);
    }

    private List<Agence> creerAgences() {
        return agenceRepository.saveAll(List.of(
                agence("AutoLoc Paris Gare de Lyon", "Paris", "20 boulevard Diderot, 75012 Paris", "01 00 00 00 01"),
                agence("AutoLoc Lyon Part-Dieu", "Lyon", "5 place Charles Béraudier, 69003 Lyon", "04 00 00 00 02"),
                agence("AutoLoc Marseille Saint-Charles", "Marseille", "Square Narvik, 13001 Marseille", "04 00 00 00 03"),
                agence("AutoLoc Bordeaux Saint-Jean", "Bordeaux", "Rue Charles Domercq, 33800 Bordeaux", "05 00 00 00 04")));
    }

    private List<Equipement> creerEquipements() {
        List<Equipement> liste = new ArrayList<>();
        for (String libelle : List.of("GPS", "Climatisation", "Bluetooth", "Caméra de recul", "Siège bébé",
                "Régulateur de vitesse", "Toit ouvrant", "Attelage")) {
            Equipement e = new Equipement();
            e.setLibelle(libelle);
            liste.add(e);
        }
        return equipementRepository.saveAll(liste);
    }

    private void creerPersonnel(List<Agence> agences) {
        employeRepository.saveAll(List.of(
                employe("Sophie", "Bernard", RoleEmploye.MANAGER, agences.get(0)),
                employe("Lucas", "Petit", RoleEmploye.AGENT, agences.get(1)),
                employe("Amina", "Haddad", RoleEmploye.AGENT, agences.get(2)),
                employe("Paul", "Lambert", RoleEmploye.AGENT, agences.get(3))));
    }

    private List<Client> creerClients(Random hasard, LocalDate aujourdhui) {
        List<Client> clients = new ArrayList<>();
        // Le client du compte USER : meme e-mail que l'identifiant de connexion
        Client demo = client("Camille", "Martin", emailUser, "P000000001", aujourdhui.minusMonths(8));
        clients.add(demo);
        for (int i = 1; i < 150; i++) {
            String prenom = PRENOMS.get(hasard.nextInt(PRENOMS.size()));
            String nom = NOMS.get(hasard.nextInt(NOMS.size()));
            String email = (prenom + "." + nom + i + "@exemple.test").toLowerCase();
            clients.add(client(prenom, nom, email, String.format("P%09d", 100 + i),
                    aujourdhui.minusDays(30 + hasard.nextInt(700))));
        }
        return clientRepository.saveAll(clients);
    }

    private List<Vehicule> creerVehicules(List<Agence> agences, List<Equipement> equipements, Random hasard,
                                          LocalDate aujourdhui) {
        List<Vehicule> vehicules = new ArrayList<>();
        for (int i = 0; i < 48; i++) {
            Modele m = MODELES.get(i % MODELES.size());
            Vehicule v = new Vehicule();
            v.setImmatriculation(immatriculation(i));
            v.setMarque(m.marque());
            v.setModele(m.modele());
            v.setCategorie(m.categorie());
            v.setTarifJournalier(BigDecimal.valueOf(m.tarif()).setScale(2, RoundingMode.HALF_UP));
            v.setStatut(StatutVehicule.DISPONIBLE);
            v.setAgence(agences.get(i % agences.size()));
            // GPS, climatisation, Bluetooth partout ; le reste au hasard
            v.getEquipements().addAll(equipements.subList(0, 3));
            for (Equipement e : equipements.subList(3, equipements.size())) {
                if (hasard.nextInt(100) < 30) {
                    v.getEquipements().add(e);
                }
            }
            vehicules.add(v);
        }
        ajouterMaintenances(vehicules, aujourdhui);
        // saveAll enregistre aussi les maintenances : cascade PERSIST de Vehicule.maintenances
        return vehiculeRepository.saveAll(vehicules);
    }

    /** 3 vehicules immobilises aujourd'hui + un historique de maintenances anciennes. */
    private static void ajouterMaintenances(List<Vehicule> vehicules, LocalDate aujourdhui) {
        String[] motifs = {"Révision des 30 000 km", "Remplacement des plaquettes de frein", "Pneus à changer"};
        for (int i = 0; i < 3; i++) {
            Vehicule v = vehicules.get(5 + i * 13);
            v.setStatut(StatutVehicule.MAINTENANCE);
            v.ajouterMaintenance(maintenance(aujourdhui.minusDays(i + 1), null, motifs[i]));
        }
        for (int i = 0; i < 12; i++) {
            LocalDate debut = aujourdhui.minusDays(240 + i * 3L);
            vehicules.get(i * 4).ajouterMaintenance(maintenance(debut, debut.plusDays(2), "Entretien périodique"));
        }
    }

    /**
     * Pour chaque vehicule, une suite de locations sans chevauchement sur 6 mois passes et 6 semaines a venir :
     * passe -> TERMINEE (payee) ou ANNULEE ; en cours aujourd'hui -> CONFIRMEE + vehicule LOUE ;
     * futur -> EN_ATTENTE ou CONFIRMEE (avec acompte).
     */
    private int creerReservations(List<Vehicule> vehicules, List<Client> clients, Random hasard, LocalDate aujourdhui) {
        List<Reservation> reservations = new ArrayList<>();
        int compteur = 0;

        for (Vehicule v : vehicules) {
            boolean enMaintenance = v.getStatut() == StatutVehicule.MAINTENANCE;
            LocalDate curseur = aujourdhui.minusDays(180 + hasard.nextInt(10));
            LocalDate limite = enMaintenance ? aujourdhui.minusDays(10) : aujourdhui.plusDays(45);
            while (true) {
                LocalDate debut = curseur.plusDays(1 + hasard.nextInt(9));
                LocalDate fin = debut.plusDays(hasard.nextInt(7));
                if (fin.isAfter(limite)) {
                    break;
                }
                curseur = fin;
                compteur++;
                // Le client de demonstration recoit une reservation sur 25 pour avoir un historique
                Client client = (compteur % 25 == 0) ? clients.get(0) : clients.get(1 + hasard.nextInt(clients.size() - 1));
                Reservation r = reservation(client, v, debut, fin);

                if (fin.isBefore(aujourdhui)) {
                    boolean annulee = hasard.nextInt(100) < 12;
                    r.setStatut(annulee ? StatutReservation.ANNULEE : StatutReservation.TERMINEE);
                    if (!annulee) {
                        Contrat c = contrat(r, debut.minusDays(1));
                        c.ajouterPaiement(paiement(c.getMontantTotal(), debut, hasard));
                    }
                } else if (!debut.isAfter(aujourdhui)) {
                    r.setStatut(StatutReservation.CONFIRMEE);
                    v.setStatut(StatutVehicule.LOUE);
                    Contrat c = contrat(r, debut.minusDays(1));
                    c.ajouterPaiement(paiement(acompte(c), debut.minusDays(1), hasard));
                } else {
                    boolean confirmee = hasard.nextInt(100) < 55;
                    r.setStatut(confirmee ? StatutReservation.CONFIRMEE : StatutReservation.EN_ATTENTE);
                    if (confirmee) {
                        Contrat c = contrat(r, aujourdhui.minusDays(hasard.nextInt(5)));
                        if (hasard.nextBoolean()) {
                            c.ajouterPaiement(paiement(acompte(c), c.getDateSignature(), hasard));
                        }
                    }
                }
                reservations.add(r);
            }
        }
        // Une seule sauvegarde : les contrats (cascade ALL) puis leurs paiements (cascade ALL) suivent
        reservationRepository.saveAll(reservations);
        return reservations.size();
    }

    // ------------------------------------------------------------------ petites fabriques

    private static String immatriculation(int i) {
        char a = (char) ('A' + (i * 7) % 26);
        char b = (char) ('B' + (i * 3) % 25);
        char c = (char) ('C' + (i * 5) % 24);
        char d = (char) ('D' + (i * 11) % 23);
        return "" + a + b + "-" + String.format("%03d", 101 + i * 17 % 899) + "-" + c + d;
    }

    private static Agence agence(String nom, String ville, String adresse, String telephone) {
        Agence a = new Agence();
        a.setNom(nom);
        a.setVille(ville);
        a.setAdresse(adresse);
        a.setTelephone(telephone);
        return a;
    }

    private static Employe employe(String prenom, String nom, RoleEmploye role, Agence agence) {
        Employe e = new Employe();
        e.setPrenom(prenom);
        e.setNom(nom);
        e.setRole(role);
        e.setAgence(agence);
        return e;
    }

    private static Client client(String prenom, String nom, String email, String permis, LocalDate inscription) {
        Client c = new Client();
        c.setPrenom(prenom);
        c.setNom(nom);
        c.setEmail(email);
        c.setTelephone("06 00 00 " + String.format("%02d %02d", Math.abs(email.hashCode()) % 100, permis.length()));
        c.setNumPermis(permis);
        c.setDateInscription(inscription);
        return c;
    }

    private static Maintenance maintenance(LocalDate debut, LocalDate fin, String description) {
        Maintenance m = new Maintenance();
        m.setDateDebut(debut);
        m.setDateFin(fin);
        m.setDescription(description);
        return m;
    }

    private static Reservation reservation(Client client, Vehicule v, LocalDate debut, LocalDate fin) {
        Reservation r = new Reservation();
        r.setClient(client);
        r.setVehicule(v);
        r.setDateDebut(debut);
        r.setDateFin(fin);
        return r;
    }

    /** Cree le contrat et le rattache a la reservation (les deux cotes de la relation). */
    private static Contrat contrat(Reservation r, LocalDate signature) {
        Contrat c = new Contrat();
        c.setDateSignature(signature);
        c.setMontantTotal(r.montantCalcule());
        c.setValide(true);
        r.attacherContrat(c);
        return c;
    }

    private static BigDecimal acompte(Contrat c) {
        return c.getMontantTotal().multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
    }

    private static Paiement paiement(BigDecimal montant, LocalDate date, Random hasard) {
        Paiement p = new Paiement();
        p.setMontant(montant);
        p.setDatePaiement(date);
        p.setModePaiement(ModePaiement.values()[hasard.nextInt(ModePaiement.values().length)]);
        return p;
    }
}
