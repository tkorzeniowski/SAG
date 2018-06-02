package sag.model;

import akka.actor.ActorRef;
import akka.japi.Pair;

import java.util.List;

/*
* Wewnętrzny stan nadzorcy.*/
public class SupervisorState {
    private List<ClientOffer> clientOffers;
    private ActorRef network, supervisor;
    private CostMatrix supplyPlan;
    private Pair<Double, Double> location;
    private List<ClientLocation>  neighbours;
    private List<ActorRef> rejectedMediumRequest;

    public SupervisorState(ActorRef supervisor,
                           List<ClientOffer> clientOffers,
                           List<ClientLocation> neighbours,
                           List<ActorRef> rejectedMediumRequest,
                           ActorRef network,
                           CostMatrix supplyPlan,
                           Pair<Double, Double> location) {

        this.supervisor = supervisor;
        this.clientOffers = clientOffers;
        this.neighbours = neighbours;
        this.rejectedMediumRequest = rejectedMediumRequest;
        this.network = network;
        this.supplyPlan = supplyPlan;
        this.location = location;
    }

    public ActorRef getSupervisor() { return supervisor; }
    public List<ClientOffer> getClientOffers() { return clientOffers; }
    public List<ClientLocation> getNeighbours() { return neighbours; }
    public List<ActorRef> getRejectedMediumRequest() { return rejectedMediumRequest; }
    public ActorRef getNetwork() { return network; }
    public CostMatrix getSupplyPlan() { return supplyPlan; }
    public Pair<Double, Double>  getLocation() { return location; }
}
