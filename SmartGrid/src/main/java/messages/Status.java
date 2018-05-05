package messages;

import akka.actor.ActorRef;

public class Status {

    private ActorRef sender;
    private StatusType status;

    public static enum StatusType {
        OFFER_ACK, // offer accepted by supervisor
        OK, // not decided yet
        NOT_OK, // not decided yet
        SEND_LOCATION, // if anyone asks, client sends its location
        DECLARE_NETWORK // network is assiigned to its supervisor
    };

    public Status(ActorRef sender, StatusType status){
        this.sender = sender;
        this.status = status;
    }

    public  ActorRef getSender(){ return sender; }

    public void setSender(ActorRef sender) { this.sender = sender; }

    public StatusType getStatus(){ return status; }

    public void setStatus(StatusType ack) { this.status = status; }

}
