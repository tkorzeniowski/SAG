package sag.messages;

import akka.japi.Pair;

public class AnnounceLocation {
    public AnnounceLocation(final Pair<Double, Double> location) {
        this.location = location;
    }

    public AnnounceLocation(final Double x, final Double y) {
        this.location = new Pair<>(x,y);
    }

    public final Pair<Double, Double> location;
}
