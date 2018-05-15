package sag.messages;

public class Offer {

	public final double demand, production;
	//private boolean firstSupervisor = true;

	public Offer(double demand, double offer) {
		this.demand = demand;
		this.production = offer;
	}
}