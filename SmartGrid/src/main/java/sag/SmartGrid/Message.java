package sag.SmartGrid;

import akka.actor.ActorRef;

public class Message {

	private double demand, offer;
	private ActorRef sender;

	public Message(double d, double o, ActorRef s){
		this.demand = d;
		this.offer = o;
		this.sender = s;
	}
	
	public double getDemand() {
		return demand;
	}

	public void setDemand(double demand) {
		this.demand = demand;
	}

	public double getOffer() {
		return offer;
	}

	public void setOffer(double offer) {
		this.offer = offer;
	}

	public ActorRef getSender() {
		return sender;
	}

	public void setSender(ActorRef sender) {
		this.sender = sender;
	}
	
}