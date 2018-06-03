package sag.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import sag.messages.AnnounceCostMatrix;
import sag.messages.AnnounceLocation;
import sag.messages.RequestCostMatrix;
import sag.model.CostMatrix;
import sag.model.ClientLocation;
import sag.messages.StatusInfo;

import java.util.ArrayList;

/**
 * Aktor reprezentujący sieć przesyłową. Komunikuje się z aktorem nadzorcą
 * przekazując mu macierz kosztów przesłania medium pomiędzy klientami,
 * wyznaczanej na podstawie ich położeń.
 */
public class Network extends AbstractActor {

    // Sieć przechowuje aktualne listy wszystkich swoich klientów
    // oraz ich położeń, początkowo puste.
    private ArrayList<ClientLocation> locations = new ArrayList<>();
    private ActorRef networkSupervisor;

    /**
     * Klasa konfigurująca określająca sposób tworzenia aktora klasy Network.
     * @param supervisor Aktor nadzorcy przypisany danemu aktorowi sieci.
     * @return Obiekt konfiguracji aktora sieci.
     */
    static public Props props(ActorRef supervisor) {
        return Props.create(Network.class, () -> new Network(supervisor));
    }

    /**
     * Konstruktor klasy sieć. Informuje przypisanego nadzorcę o swoim istnieniu.
     * @param supervisor Aktor nadzorcy przypisany danemu aktorowi sieci.
     */
    private Network(ActorRef supervisor) {
        this.networkSupervisor = supervisor;
        sendStatus(StatusInfo.StatusType.DECLARE_NETWORK);
    }

    /*
     * Przesyła wiadomość z informacją o statusie sieci do przypisanego nadzorcy.
     */
    private void sendStatus(StatusInfo.StatusType statusType) {
        StatusInfo msg = new StatusInfo(statusType);
        networkSupervisor.tell(msg, getSelf());
    }

    /*
     * Na podstawie wiadomości otrzymanych od klientów uzupełnia wewnętrzną listę
     * klientów oraz ich położeń, które pozwalają na skonstruowanie macierzy kosztów.
     */
    private void receiveLocation(AnnounceLocation msg) {
        boolean senderExists = false;
        for (ClientLocation cl : locations) {
            if (cl.getClient() == this.sender()) {
                senderExists = true;
                break;
            }
        }
        if (!senderExists) {
            locations.add(new ClientLocation(this.sender(), msg.location));
        }
    }

    /*
     * Tworzy i wysyła macierz kosztów do przypisanego nadzorcy.
     */
    private void sendCostMatrix(RequestCostMatrix rcm) {
        ArrayList<ActorRef> clientOffers = rcm.getClients();
        ArrayList<ClientLocation> orderedLocations = new ArrayList<>();

        for (ActorRef tc : clientOffers) {
            for (ClientLocation loc : locations) {
                if (tc.compareTo(loc.getClient()) == 0) {
                    orderedLocations.add(loc);
                    break;
                }
            }
        }

        CostMatrix cm = new CostMatrix(orderedLocations);
        AnnounceCostMatrix msg = new AnnounceCostMatrix(cm);
        networkSupervisor.tell(msg, getSelf());
    }


    /**
     * Reaguje na przyjęcie wiadomości od innego aktora zgodnie z zadanymi wzorcami zachowań.
     * Sieć jest przygotowana na otrzymywanie lokacji od klienta oraz na zgłaszenie
     * zapotrzebowania na macierz kosztów przez nadzorcę.
     * @return Sposób zachowania aktora w obliczu otrzymania wiadomości konkretnego rodzaju.
     */
    @Override
	public Receive createReceive() {
		return receiveBuilder()
                .match(AnnounceLocation.class, this::receiveLocation)
                .match(RequestCostMatrix.class, this::sendCostMatrix)
                .build();
	}
}