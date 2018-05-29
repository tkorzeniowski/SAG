package sag.messages;

/**
 * Rodzaj wiadomości przesyłany przez klienta do swojego nadzorcy, zawierająca ofertę
 * obejmującą zapotrzebowanie i produkcję medium.
 */
public class Offer {
	public final double demand, production;
	//private boolean firstSupervisor = true;

	public Offer(double demand, double offer) {
		this.demand = demand;
		this.production = offer;
	}
}