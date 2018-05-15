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
    public Network(ActorRef supervisor) {
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
        locations.add(new ClientLocation(this.sender(), msg.location));
    }

    /*
     * Tworzy i wysyła macierz kosztów do przypisanego nadzorcy.
     */
    private void sendCostMatrix(RequestCostMatrix rcm) {
        CostMatrix cm = new CostMatrix(locations);
        AnnounceCostMatrix msg = new AnnounceCostMatrix(cm);
        networkSupervisor.tell(msg, getSelf());
    }

    @Override
    public void preStart() {
        /*
        getContext().getSystem().scheduler().schedule(
                Duration.create(2, TimeUnit.SECONDS), // Initial delay 2 seconds
                Duration.create(0, TimeUnit.SECONDS),     // Frequency 0 seconds
                super.getSelf(), // Send the message to itself
                "createCostMatrix",
                getContext().getSystem().dispatcher(),
                null
        );
        */
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