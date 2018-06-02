package sag.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import sag.messages.AnnounceLocation;
import sag.messages.Offer;
//import sag.messages.RequestMedium;
import sag.messages.StatusInfo;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;										  								 

/**
 * Aktor reprezentujący klienta. Nie rozróżnia konsumenta od producenta, może bowiem zarówno
 * zgłaszać zapotrzebowanie jak i produkcję medium. Komunikuje się jednorazowo z siecią przysyłową
 * w celu przekazania swojego położenia. Ponadto wysyła oferty do swojego nadzorcy, który umożliwia
 * dokonanie transakcji z innymi klientami.
 */
public class Client extends AbstractActor {

    private double demand, production, sentDemand, sentProduction, xCoord, yCoord;
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
    static public Props props(double demand,
                              double production,
                              double x,
                              double y,
                              ActorRef supervisor,
                              ActorRef network) {

        return Props.create(Client.class, () ->
            new Client(demand, production, x, y, supervisor, network));
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
    private Client(double demand,
                   double production,
                   double x,
                   double y,
                   ActorRef supervisor,
                   ActorRef network) {

        this.demand = demand;
        this.production = production;
        this.xCoord = x;
        this.yCoord = y;
        this.clientSupervisor = supervisor;
        this.network = network;

        sendLocation(this.network); // inform network that new client appeared
    }

    /*
     * Przesyła sieci wiadomość o swoim położeniu.
     */
    private void sendLocation(ActorRef recepient) {
        log.info("myLocationIs: (" + xCoord + ", " + yCoord + ")");
        recepient.tell(new AnnounceLocation(xCoord, yCoord), getSelf());
    }

    /*
     * Przesyła ofertę swojemu nadzorcy na podstawie swojego zapotrzebowania i produkcji medium.
     */
    private void sendOffer() {

        if(production > 0){
            sentProduction = ThreadLocalRandom.current().nextDouble(production*0.95, production*1.05);
        }
        if(demand > 0){
            sentDemand = ThreadLocalRandom.current().nextDouble(demand*0.95, demand*1.05);
            if(sentDemand <= sentProduction){
                sentDemand -= sentProduction;
            }
        }

        Offer msg = new Offer(sentDemand, sentProduction);

        log.info("stateOffer: dem-" + msg.demand + " , prod-" + msg.production);
        clientSupervisor.tell(msg, getSelf());
    }

    /*
     * Reakcja na wiadomość o planowanej dostawie medium od nadzorcy.
     */
	private void receiveSupply(Offer msg) {
        //log.info(this.sender() + " " + msg.demand + " " + msg.production);
        log.info("Client demand after = " + (this.sentDemand - msg.demand));
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

        if(status.status == StatusInfo.StatusType.GET_NEIGHBOURS) {
            // do nothig, otherwise endless loop of messages
        }
    }

    /*
    * Wysyłanie wiadomości z ustaloną częstotliwością
    */
	@Override
    public void preStart() {
        getContext().getSystem().scheduler().schedule(
                Duration.create(100, TimeUnit.MILLISECONDS), // Initial delay 100 milliseconds
                Duration.create(5, TimeUnit.SECONDS),     // Frequency 5 seconds
                super.getSelf(), // Send the message to itself
                "sendOffer",
                getContext().getSystem().dispatcher(),
                null
        );
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
				.matchEquals("sendOffer", m -> sendOffer())
                .build();
    }
}
