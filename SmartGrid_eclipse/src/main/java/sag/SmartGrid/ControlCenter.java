package sag.SmartGrid;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ControlCenter extends AbstractActor{

	private double demand = 0, offer = 0;
	private ActorRef clientActor, producerActor;
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	
	static public Props props() {
	    return Props.create(ControlCenter.class, () -> new ControlCenter());
	  }
	
	
	public ControlCenter(){
	}
	
	private void receiveMessage(Message msg){
		
		log.info(msg.getSender() + " " + msg.getDemand() + " " + msg.getOffer() );
		
		if(msg.getSender().toString().contains("client")){
			this.clientActor = msg.getSender();
		}
		
		if(msg.getSender().toString().contains("producer")){
			this.producerActor = msg.getSender();
		}
		

		if(msg.getDemand()>0){
			this.demand = msg.getDemand();
		}
		if(msg.getOffer() > 0){
			this.offer = msg.getOffer();
		}
				
		
		if(this.demand > 0 && this.offer > 0){
			System.out.println("Calculating...");
			calculate();
		}
		
	}
	
	
	private void calculate(){
		if(this.demand < this.offer){
			Message msg = new Message(this.demand, this.demand, getSelf());
			clientActor.tell(msg, getSelf());
			producerActor.tell(msg, getSelf());
		}else{
			Message msg = new Message(0, 0, ActorRef.noSender());
			clientActor.tell(msg, getSelf());
			producerActor.tell(msg, getSelf());
		}
	}
	
		
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Message.class, this::receiveMessage).build();
	}
	
}