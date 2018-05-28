package sag.messages;

import akka.japi.Pair;

/**
 * Rodzaj wiadomości przesyłanej przez klienta do przypisanej sobie sieci
 * w celu podania swojego położenia.
 */
public class AnnounceLocation {
    public AnnounceLocation(final Double x, final Double y) {
        this.location = new Pair<>(x,y);
    }
    public final Pair<Double, Double> location;
}
