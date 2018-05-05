package messages;

import akka.actor.ActorRef;

import java.util.List;

public class CostMatrix {

    private ActorRef sender;
    private List<ActorRef> clients;
    private double[][] costMatrix;

    public ActorRef getSender() { return sender; }

    public void setSender(ActorRef sender) { this.sender = sender; }

    public List<ActorRef> getClients() { return clients; }

    public void setClients(List<ActorRef> clients) { this.clients = clients; }

    public CostMatrix(ActorRef sender, List<ActorRef> clients, double[][] cm){
        this.sender = sender;
        this.clients = clients;
        this.costMatrix = cm;
    }

    public double[][] getCostMatrix() {
        return costMatrix;
    }
}
