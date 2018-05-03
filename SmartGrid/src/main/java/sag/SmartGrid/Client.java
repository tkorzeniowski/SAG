package sag.SmartGrid;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import messages.Ack;
import messages.Offer;

public class Client extends AbstractActor{

    private double demand, production, xCoord, yCoord;
    private final ActorRef clientSupervisor;
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);


    static public Props props(double demand, double production, double x, double y, ActorRef smartActor) {
        return Props.create(Client.class, () -> new Client(demand, production, x, y, smartActor));
    }


    public Client(double demand, double production, double x, double y, ActorRef smartActor){
        this.demand = demand;
        this.production = production;
        this.xCoord = x;
        this.yCoord = y;
        this.clientSupervisor = smartActor;
        stateOffer();
    }

    public void stateOffer(){
        Offer msg = new Offer(getSelf(), demand, production );
        //System.out.println("Client sent demand = " + this.demand);
        log.info("stateOffer: dem-" + msg.getDemand() + " , prod-" + msg.getProduction());
        clientSupervisor.tell(msg, getSelf());
    }

    private void receiveSupply(Offer msg){

        log.info(msg.getSender() + " " + msg.getDemand() + " " + msg.getProduction());
        this.production -= msg.getProduction();
        this.demand -= msg.getDemand();
        //System.out.println("Client demand after = " + this.demand);
        log.info("Client demand after = " + this.demand);

    }

    private void receiveAck(Ack ack){
        if(ack.getSender().equals(clientSupervisor)){
            if(ack.getAck() == Ack.AckType.OFFER_ACK){
                //wait for supply plan
                /*
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie){

                }
                */
            }
        }
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Offer.class, this::receiveSupply)
                .match(Ack.class, this::receiveAck)
                .build();
    }

}
