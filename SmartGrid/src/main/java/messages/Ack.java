package messages;

import akka.actor.ActorRef;

public class Ack {

    private ActorRef sender;
    private AckType ack;

    public static enum AckType {OFFER_ACK, OK, NOT_OK};

    public Ack(ActorRef sender, AckType ack){
        this.sender = sender;
        this.ack = ack;
    }

    public  ActorRef getSender(){ return sender; }

    public void setSender(ActorRef sender) { this.sender = sender; }

    public AckType getAck(){ return ack; }

    public void setAck(AckType ack) { this.ack = ack; }

}
