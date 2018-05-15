package sag.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import sag.messages.StatusInfo;
import sag.model.ClientLocation;
import sag.messages.Offer;

public class Client extends AbstractActor{

    private double demand, production, xCoord, yCoord;
    private final ActorRef clientSupervisor, network;
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static public Props props(double demand, double production, double x, double y, ActorRef supervisor, ActorRef network) {
        return Props.create(Client.class, () -> new Client(demand, production, x, y, supervisor, network));
    }

    public Client(double demand, double production, double x, double y, ActorRef supervisor, ActorRef network){
        this.demand = demand;
        this.production = production;
        this.xCoord = x;
        this.yCoord = y;
        this.clientSupervisor = supervisor;
        this.network = network;

        sendLocation(this.network); // inform network that new client appeared
        sendOffer();
    }

    private void sendLocation(ActorRef recepient){
        ClientLocation msg = new ClientLocation(getSelf(), xCoord, yCoord);
        log.info("myLocationIs: (" + msg.x() + ", " + msg.y() + ")");
        recepient.tell(msg, getSelf());
    }

    private void sendOffer() {
        Offer msg = new Offer(demand, production);
        log.info("stateOffer: dem-" + msg.demand + " , prod-" + msg.production);
        clientSupervisor.tell(msg, getSelf());
    }

    private void receiveSupply(Offer msg) {
        log.info(this.sender() + " " + msg.demand + " " + msg.production);
        this.production -= msg.production;
        this.demand -= msg.demand;
        log.info("Client demand after = " + this.demand);
    }

    private void receiveStatus(StatusInfo status){
        if(this.sender().equals(clientSupervisor)){
            if(status.status == StatusInfo.StatusType.OFFER_ACK){
                //stop waiting for ack
            }
        }

        if(status.status == StatusInfo.StatusType.SEND_LOCATION){
            sendLocation(this.sender());
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Offer.class, this::receiveSupply)
                .match(StatusInfo.class, this::receiveStatus)
                .build();
    }
}
