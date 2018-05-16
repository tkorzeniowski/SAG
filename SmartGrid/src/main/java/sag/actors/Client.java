package sag.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import sag.messages.StatusInfo;
import sag.model.ClientLocation;
import sag.messages.Offer;

/**
 * Aktor reprezentujący klienta. Nie rozróżnia konsumenta od producenta, może bowiem zarówno
 * zgłaszać zapotrzebowanie jak i produkcję medium. Komunikuje się jednorazowo z siecią przysyłową
 * w celu przekazania swojego położenia. Ponadto wysyła oferty do swojego nadzorcy, który umożliwia
 * dokonanie transakcji z innymi klientami.
 */
public class Client extends AbstractActor {

    private double demand, production, xCoord, yCoord;
    private final ActorRef clientSupervisor, network;
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    /**
     * Klasa konfigurująca określająca sposób tworzenia aktora klasy Client.
     * @param demand Zapotrzebowanie na medium.
     * @param production Ilość wyprodukowanego medium.
     * @param x Położenie klienta - współrzędna x.
     * @param y Położenie klienta - współrzędna y.
     * @param supervisor Aktor nadzorcy przypisany danemu aktorowi sieci.
     * @param network Aktor sieci, do której należy klient.
     * @return Obiekt konfiguracji aktora sieci.
     */
    static public Props props(double demand, double production, double x, double y, ActorRef supervisor, ActorRef network) {
        return Props.create(Client.class, () -> new Client(demand, production, x, y, supervisor, network));
    }

    /**
     * Konstruktor klasy klient. Informuje sieć, do której należy o swoim istnieniu
     * oraz wysyła ofertę przypisanemu sobie nadzorcy.
     * @param demand Zapotrzebowanie na medium.
     * @param production Ilość wyprodokowanego medium.
     * @param x Położenie klienta - współrzędna x.
     * @param y Położenie klienta - współrzędna y.
     * @param supervisor Przypisany klientowi nadzorca.
     * @param network Sieć, do której należy klient.
     */
    public Client(double demand, double production, double x, double y, ActorRef supervisor, ActorRef network) {
        this.demand = demand;
        this.production = production;
        this.xCoord = x;
        this.yCoord = y;
        this.clientSupervisor = supervisor;
        this.network = network;

        sendLocation(this.network); // inform network that new client appeared
        sendOffer();
    }

    /*
     * Przesyła sieci wiadomość o swoim położeniu.
     */
    private void sendLocation(ActorRef recepient) {
        ClientLocation msg = new ClientLocation(getSelf(), xCoord, yCoord);
        log.info("myLocationIs: (" + msg.x() + ", " + msg.y() + ")");
        recepient.tell(msg, getSelf());
    }

    /*
     * Przesyła ofertę swojemu nadzorcy na podstawie swojego zapotrzebowania i produkcji medium.
     */
    private void sendOffer() {
        Offer msg = new Offer(demand, production);
        log.info("stateOffer: dem-" + msg.demand + " , prod-" + msg.production);
        clientSupervisor.tell(msg, getSelf());
    }

    /*
     * Reakcja na transakcję.
     * TODO - zastanowić się i uzupełnić, być może zmienić nazwę
     */
    private void receiveSupply(Offer msg) {
        log.info(this.sender() + " " + msg.demand + " " + msg.production);
        this.production -= msg.production;
        this.demand -= msg.demand;
        log.info("Client demand after = " + this.demand);
    }

    /*
     * Reakcja na informację o przyjęciu oferty przez nadzorcę.
     * TODO - uzupełnić
     */
    private void receiveStatus(StatusInfo status) {
        if (this.sender().equals(clientSupervisor)) {
            if (status.status == StatusInfo.StatusType.OFFER_ACK) {
                //stop waiting for ack
            }
        }

        if(status.status == StatusInfo.StatusType.SEND_LOCATION) {
            sendLocation(this.sender());
        }
    }

    /**
     * Reaguje na przyjęcie wiadomości od innego aktora zgodnie z zadanymi wzorcami zachowań.
     * Klient jest przygotowany jedynie na otrzymywanie potwierdzeń przyjęcia oferty
     * oraz transakcji przez nadzorcę.
     * TODO - będzie potrzebne potwierdzenie od sieci (inaczej wysłanie informacji o swoim...
     * TODO - ...istnieniu trzeba będzie ponawiać
     * @return Sposób zachowania aktora w obliczu otrzymania wiadomości konkretnego rodzaju.
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Offer.class, this::receiveSupply)
                .match(StatusInfo.class, this::receiveStatus)
                .build();
    }
}
