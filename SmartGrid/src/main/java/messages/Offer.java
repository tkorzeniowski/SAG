package messages;

import akka.actor.ActorRef;

public class Offer {

	private ActorRef sender;
	private double demand, production;
	//private boolean firstSupervisor = true;


	public Offer(ActorRef sender, double demand, double offer ){
		this.demand = demand;
		this.production = offer;
		this.sender = sender;
	}

	public ActorRef getSender() {
		return sender;
	}

	public void setSender(ActorRef sender) {
		this.sender = sender;
	}

	public double getDemand() {
		return demand;
	}

	public void setDemand(double demand) {
		this.demand = demand;
	}

	public double getProduction() {
		return production;
	}

	public void setProduction(double production) {
		this.production = production;
	}

}