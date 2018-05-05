package sag.SmartGrid;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import messages.Status;
import messages.Location;
import messages.Offer;

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
        Location msg = new Location(getSelf(), xCoord, yCoord);
        log.info("myLocationIs: (" + msg.getX() + ", " + msg.getY() + ")");
        recepient.tell(msg, getSelf());
    }

    private void sendOffer(){
        Offer msg = new Offer(getSelf(), demand, production );
        log.info("stateOffer: dem-" + msg.getDemand() + " , prod-" + msg.getProduction());
        clientSupervisor.tell(msg, getSelf());
    }

    private void receiveSupply(Offer msg){

        log.info(msg.getSender() + " " + msg.getDemand() + " " + msg.getProduction());
        this.production -= msg.getProduction();
        this.demand -= msg.getDemand();
        log.info("Client demand after = " + this.demand);

    }

    private void receiveStatus(Status status){
        if(status.getSender().equals(clientSupervisor)){
            if(status.getStatus() == Status.StatusType.OFFER_ACK){
                //stop waiting for ack
            }
        }

        if(status.getStatus() == Status.StatusType.SEND_LOCATION){
            sendLocation(status.getSender());
        }

    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Offer.class, this::receiveSupply)
                .match(Status.class, this::receiveStatus)
                .build();
    }

}
