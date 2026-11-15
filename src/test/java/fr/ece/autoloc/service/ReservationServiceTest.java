package fr.ece.autoloc.service;

import fr.ece.autoloc.domain.Agence;
import fr.ece.autoloc.domain.CategorieVehicule;
import fr.ece.autoloc.domain.Client;
import fr.ece.autoloc.domain.Contrat;
import fr.ece.autoloc.domain.ModePaiement;
import fr.ece.autoloc.domain.Reservation;
import fr.ece.autoloc.domain.StatutReservation;
import fr.ece.autoloc.domain.StatutVehicule;
import fr.ece.autoloc.domain.Vehicule;
import fr.ece.autoloc.exception.RegleMetierException;
import fr.ece.autoloc.exception.RequeteInvalideException;
import fr.ece.autoloc.repository.ClientRepository;
import fr.ece.autoloc.repository.ReservationRepository;
import fr.ece.autoloc.repository.VehiculeRepository;
import fr.ece.autoloc.web.dto.ReservationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitaire : les repositories sont remplaces par des "mocks" (faux objets pilotes par le test).
 * On teste uniquement les regles metier, sans base de donnees ni Spring : c'est tres rapide.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 9);
    private static final String EMAIL = "user@autoloc.fr";

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private VehiculeRepository vehiculeRepository;
    @Mock
    private ClientRepository clientRepository;

    private ReservationService service;
    private Vehicule vehicule;
    private Client client;

    @BeforeEach
    void preparer() {
        // Horloge figee : le test donne le meme resultat quel que soit le jour ou on le lance
        Clock horloge = Clock.fixed(AUJOURDHUI.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new ReservationService(reservationRepository, vehiculeRepository, clientRepository, horloge);

        Agence agence = new Agence();
        agence.setIdAgence(1L);
        agence.setVille("Paris");

        vehicule = new Vehicule();
        vehicule.setIdVehicule(10L);
        vehicule.setMarque("Peugeot");
        vehicule.setModele("208");
        vehicule.setImmatriculation("AB-123-CD");
        vehicule.setCategorie(CategorieVehicule.CITADINE);
        vehicule.setTarifJournalier(new BigDecimal("40.00"));
        vehicule.setStatut(StatutVehicule.DISPONIBLE);
        vehicule.setAgence(agence);

        client = new Client();
        client.setIdClient(5L);
        client.setPrenom("Camille");
        client.setNom("Martin");
        client.setEmail(EMAIL);
    }

    private Reservation reservation(Long id, LocalDate debut, LocalDate fin, StatutReservation statut) {
        Reservation r = new Reservation();
        r.setIdReservation(id);
        r.setClient(client);
        r.setVehicule(vehicule);
        r.setDateDebut(debut);
        r.setDateFin(fin);
        r.setStatut(statut);
        return r;
    }

    @Test
    void louer_refuse_un_vehicule_deja_loue_sur_les_dates() {
        when(clientRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(client));
        when(vehiculeRepository.findPourMiseAJour(10L)).thenReturn(Optional.of(vehicule));
        when(reservationRepository.compterChevauchements(eq(10L), anyCollection(), any(), any(), isNull()))
                .thenReturn(1L);

        ReservationRequest requete = new ReservationRequest(10L, AUJOURDHUI.plusDays(2), AUJOURDHUI.plusDays(4));

        assertThatThrownBy(() -> service.louer(EMAIL, requete))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("déjà loué");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void louer_refuse_une_date_de_fin_avant_la_date_de_debut() {
        when(clientRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(client));
        ReservationRequest requete = new ReservationRequest(10L, AUJOURDHUI.plusDays(5), AUJOURDHUI.plusDays(2));

        assertThatThrownBy(() -> service.louer(EMAIL, requete)).isInstanceOf(RequeteInvalideException.class);
    }

    @Test
    void louer_enregistre_une_location_en_attente_avec_le_bon_montant() {
        when(clientRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(client));
        when(vehiculeRepository.findPourMiseAJour(10L)).thenReturn(Optional.of(vehicule));
        when(reservationRepository.compterChevauchements(eq(10L), anyCollection(), any(), any(), isNull()))
                .thenReturn(0L);

        // Du 11 au 13 novembre = 3 jours x 40 EUR = 120 EUR
        var reponse = service.louer(EMAIL, new ReservationRequest(10L, AUJOURDHUI.plusDays(2), AUJOURDHUI.plusDays(4)));

        assertThat(reponse.statut()).isEqualTo(StatutReservation.EN_ATTENTE);
        assertThat(reponse.nbJours()).isEqualTo(3);
        assertThat(reponse.montant()).isEqualByComparingTo("120.00");
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void confirmer_rattache_un_contrat_au_montant_de_la_location() {
        Reservation reservation = reservation(77L, AUJOURDHUI.plusDays(1), AUJOURDHUI.plusDays(5), StatutReservation.EN_ATTENTE);
        when(reservationRepository.findDetail(77L)).thenReturn(Optional.of(reservation));
        when(vehiculeRepository.findPourMiseAJour(10L)).thenReturn(Optional.of(vehicule));

        service.confirmer(77L);

        // Le contrat est rattache des deux cotes : la cascade ALL l'enregistrera avec la reservation
        Contrat contrat = reservation.getContrat();
        assertThat(contrat).isNotNull();
        assertThat(contrat.getReservation()).isSameAs(reservation);
        assertThat(contrat.getMontantTotal()).isEqualByComparingTo("200.00"); // 5 jours x 40
        assertThat(contrat.isValide()).isTrue();
        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
        assertThat(vehicule.getStatut()).isEqualTo(StatutVehicule.LOUE);
    }

    @Test
    void terminer_encaisse_le_solde_et_libere_le_vehicule() {
        Reservation reservation = reservation(78L, AUJOURDHUI.minusDays(2), AUJOURDHUI, StatutReservation.CONFIRMEE);
        Contrat contrat = new Contrat();
        contrat.setMontantTotal(new BigDecimal("120.00"));
        contrat.setValide(true);
        reservation.attacherContrat(contrat);
        vehicule.setStatut(StatutVehicule.LOUE);
        when(reservationRepository.findDetail(78L)).thenReturn(Optional.of(reservation));

        service.terminer(78L);

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.TERMINEE);
        assertThat(vehicule.getStatut()).isEqualTo(StatutVehicule.DISPONIBLE);
        assertThat(contrat.getPaiements()).hasSize(1);
        assertThat(contrat.getPaiements().get(0).getContrat()).isSameAs(contrat);
        assertThat(contrat.getPaiements().get(0).getModePaiement()).isEqualTo(ModePaiement.CARTE);
    }

    @Test
    void l_utilisateur_ne_peut_plus_annuler_le_jour_du_depart() {
        when(clientRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(client));
        Reservation reservation = reservation(79L, AUJOURDHUI, AUJOURDHUI.plusDays(2), StatutReservation.CONFIRMEE);
        when(reservationRepository.findDetail(79L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.annulerParUtilisateur(EMAIL, 79L))
                .isInstanceOf(RegleMetierException.class);
        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
    }
}
