package sag.model;

import akka.actor.ActorRef;
import akka.japi.Pair;

public class ClientOffer {
    private ActorRef client;
    private Pair<Double, Double> offer;

    public ClientOffer(ActorRef client, double demand, double production) {
        this.client = client;
        this.offer = new Pair<>(demand, production);
    }

    public ActorRef client() { return client; }
    public double demand() { return offer.first(); }
    public double production() { return offer.second(); }
}
