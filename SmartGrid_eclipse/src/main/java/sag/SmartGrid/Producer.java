package sag.SmartGrid;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Producer extends AbstractActor{

	private double producerOffer;
	private final ActorRef controlActor;
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	
	static public Props props(double offer, ActorRef smartActor) {
	    return Props.create(Producer.class, () -> new Producer(offer, smartActor));
	  }
	
	
	public Producer(double offer, ActorRef smartActor){
		this.producerOffer = offer;
		this.controlActor = smartActor;
		stateOffer();
		
	}
	
	public void stateOffer(){
		Message msg = new Message(0, producerOffer, getSelf());
		System.out.println("Producer sent offer = " + this.producerOffer);
		controlActor.tell(msg, getSelf());
	}
	
	private void receiveMessage(Message msg){
		log.info(msg.getSender()+ " " + msg.getDemand() + " " + msg.getOffer());
		
		this.producerOffer -= msg.getOffer();
		System.out.println("Producer offer after = " + this.producerOffer);
		log.info("Producer offer after = " + this.producerOffer);
		
	}
	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Message.class, this::receiveMessage).build();
	}
	
}