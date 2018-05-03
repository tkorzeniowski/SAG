package sag.SmartGrid;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Client extends AbstractActor{

	private double clientDemand;
	private final ActorRef controlActor;
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	
	static public Props props(double demand, ActorRef smartActor) {
	    return Props.create(Client.class, () -> new Client(demand, smartActor));
	  }
	
	
	public Client(double demand, ActorRef smartActor){
		this.clientDemand = demand;
		this.controlActor = smartActor;
		stateDemand();
		
	}
	
	public void stateDemand(){
		Message msg = new Message(clientDemand, 0, getSelf());
		System.out.println("Client sent demand = " + this.clientDemand);
		controlActor.tell(msg, getSelf());
	}
	
	private void receiveMessage(Message msg){
		
		log.info(msg.getSender() + " " + msg.getDemand() + " " + msg.getOffer());

		this.clientDemand -= msg.getDemand();
		System.out.println("Client demand after = " + this.clientDemand);
		log.info("Client demand after = " + this.clientDemand);
		
	}
	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Message.class, this::receiveMessage).build();
	}
	
}