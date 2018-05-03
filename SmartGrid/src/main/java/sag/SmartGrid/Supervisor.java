package sag.SmartGrid;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.ampl.AMPL;
import com.ampl.Parameter;
import com.ampl.Variable;
import messages.Ack;
import messages.Offer;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Supervisor extends AbstractActor{

	private double demand = 0, offer = 0;
	//private ActorRef clientActor, producerActor;
	private List<ActorRef> clients;
	private List<Double> demands, production;
	private double[][] costMatrix, supplyPlan;
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	
	static public Props props() {
	    return Props.create(Supervisor.class, () -> new Supervisor());
	  }
	
	
	public Supervisor(){
	    atStart();
	    //amplDemo();
	}

	private void amplDemo(){
	    AMPL ampl = new AMPL();

	    ClassLoader classLoader = getClass().getClassLoader();

	    try {
			ampl.reset();
			String modelFile = new File(classLoader.getResource("SmartGridModel.mod").getFile()).getAbsolutePath();
            ampl.read(modelFile);

            // --------------------------------
			//String dataFile = new File(classLoader.getResource("SmartGridData.dat").getFile()).getAbsolutePath();
            //ampl.readData(dataFile);

            // --------------------------------
			Parameter x = ampl.getParameter("N");
			x.setValues(5);
			

			List<Double> zap = new ArrayList<Double>();
			zap.add(0.0);
			zap.add(50.0);
			zap.add(25.0);
			zap.add(17.0);
			zap.add(8.0);
            x = ampl.getParameter("zap");
			x.setValues(zap.toArray());


			List<Double> prod = new ArrayList<Double>();
			prod.add(100.0);
			prod.add(0.0);
			prod.add(0.0);
			prod.add(0.0);
			prod.add(0.0);
            x = ampl.getParameter("prod");
			x.setValues(prod.toArray());


			double[] costMatrixVector = new double[25]; // N^2
			System.arraycopy(costMatrix[0], 0, costMatrixVector, 0, costMatrix[0].length);

			for(int i=0; i<4; ++i){ // N-1
                System.arraycopy(costMatrix[i+1], 0, costMatrixVector, costMatrix[i].length, costMatrix[i+1].length);
            }
            x = ampl.getParameter("cTransp");
			x.setValues(costMatrixVector);

			// --------------------------------
            ampl.solve();

        } catch (java.io.IOException e){
	        System.out.println("File not found!");
        }

        //System.out.println("wartość funkcji celu: " + ampl.getObjective("f_celu").value());

        if(ampl.getObjective("f_celu").value() == 0){
	    	System.out.println("Brak rozwiązania! ograniczenia niespełnione");
		} else {
			Variable v = ampl.getVariable("xTransp");
			System.out.println(ampl.getParameter("prod").getValues());
			System.out.print(v.getValues());

			double[] valColumn = v.getValues().getColumnAsDoubles("val");

			System.out.println(valColumn[1]);

		}
    }
	
	private void receiveOffer(Offer msg){
		
		log.info(msg.getSender() + " " + msg.getDemand() + " " + msg.getProduction() );

		if(clients == null || clients.isEmpty()){
			clients = new ArrayList<ActorRef>();
		}

		if(demands == null || demands.isEmpty()){
			demands = new ArrayList<Double>();
		}

		if(production == null || production.isEmpty()){
			production = new ArrayList<Double>();
		}

		clients.add(msg.getSender());
		demands.add(msg.getDemand());
		production.add(msg.getProduction());

		Ack ack = new Ack(getSelf(), Ack.AckType.OFFER_ACK);
		msg.getSender().tell(ack, ActorRef.noSender());

	}

	private void createSupplyPlan(){
		int i=0;
		System.out.println("Otrzymałem następujące oferty:");
		for(ActorRef client: clients){
			System.out.println(clients.get(i) + " " + demands.get(i) + " " + production.get(i));
			++i;
		}

		if(costMatrix == null){
            costMatrix = new double[5][5];
            costMatrix[0] = new double[] { 10e10, 2.0, 5.0, 7.0, 3.0 };
            costMatrix[1] = new double[] { 10.0, 10e10, 13.0, 2.0, 5.0 };
            costMatrix[2] = new double[] { 1.0, 1.0, 10e10, 2.0, 3.0 };
            costMatrix[3] = new double[] { 2.0, 2.0, 3.0, 10e10, 4.0 };
            costMatrix[4] = new double[] { 3.0, 6.0, 9.0, 12.0, 10e10 };
        }

        for(i=0; i<5; ++i){
		    for(int j=0; j<5; ++j){
		        System.out.print(costMatrix[i][j] + " ");
            }
            System.out.println();
        }

		amplDemo();
	}

	private void atStart(){
		getContext().getSystem().scheduler().schedule(
				Duration.create(100, TimeUnit.MILLISECONDS), // Initial delay 100 milliseconds
				Duration.create(2, TimeUnit.SECONDS),     // Frequency 2 seconds
				super.getSelf(), // Send the message to itself
				"supplyPlanTick",
				getContext().getSystem().dispatcher(),
				null
		);
	}
		
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Offer.class, this::receiveOffer)
				.matchEquals("supplyPlanTick", m->createSupplyPlan())
				.build();
	}
	
}