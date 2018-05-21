package sag.model;

import akka.actor.ActorRef;
import akka.japi.Pair;

public class ClientLocation {
    private ActorRef client;
    private Pair<Double, Double> location;

    public ClientLocation(ActorRef client, Pair<Double, Double> location) {
        this.client = client;
        this.location = location;
    }

    public ClientLocation(ActorRef client, double x, double y) {
        this.client = client;
        this.location = new Pair<>(x,y);
    }

    public ActorRef getClient() { return client; }
    public double x() { return location.first(); }
    public double y() { return location.second(); }
    public Pair<Double, Double>  getLocation() { return location; }
}
