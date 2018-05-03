package sag.SmartGrid;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import messages.Offer;

public class Network extends AbstractActor{

    private double infinity = 1e10;
    private double[][] costMatrix;
/*
	private double producerOffer;
	private final ActorRef controlActor;
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	
	static public Props props(double offer, ActorRef smartActor) {
	    return Props.create(Network.class, () -> new Network(offer, smartActor));
	  }
	
	
	public Network(double offer, ActorRef smartActor){
		this.producerOffer = offer;
		this.controlActor = smartActor;
		//stateOffer();
		
	}

	public void stateOffer(){
		Offer msg = new Offer(getSelf(),0, producerOffer );
		System.out.println("Network sent offer = " + this.producerOffer);
		controlActor.tell(msg, getSelf());
	}

	private void receiveMessage(Offer msg){
		log.info(msg.getSender()+ " " + msg.getDemand() + " " + msg.getProduction());
		
		this.producerOffer -= msg.getProduction();
		System.out.println("Network offer after = " + this.producerOffer);
		log.info("Network offer after = " + this.producerOffer);
		
	}

*/
	@Override
	public Receive createReceive() {
		//return receiveBuilder().match(Offer.class, this::receiveMessage).build();
		return receiveBuilder().build();
	}

}