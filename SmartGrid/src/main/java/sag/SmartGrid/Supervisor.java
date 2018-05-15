package sag.SmartGrid;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import messages.CostMatrix;
import messages.Status;
import messages.Offer;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Supervisor extends AbstractActor{

    private List<ActorRef> clients;
    private ActorRef network;
    private List<Double> demands, production;
    private double[][] costMatrix, supplyPlan;
    private int N = 0;
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);


    static public Props props() {
        return Props.create(Supervisor.class, () -> new Supervisor());
    }


    private void amplDemo(CostMatrix cm){
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
            // set model parameters
            Parameter x = ampl.getParameter("N");
            x.setValues(N);

            x = ampl.getParameter("dem");
            x.setValues(demands.toArray());

            x = ampl.getParameter("prod");
            x.setValues(production.toArray());


            costMatrix = cm.getCostMatrix();
            System.out.println("Otrzymana macierz kosztów");
            for(int i=0; i<N; ++i){
                for(int j=0; j<N; ++j){
                    System.out.print(costMatrix[i][j] + " ");
                }
                System.out.println();
            }
            // convert matrix to vector
            double[] costMatrixVector = new double[N*N];
            System.arraycopy(costMatrix[0], 0, costMatrixVector, 0, costMatrix[0].length);

            for(int i=0; i<(N-1); ++i){ // N-1
                System.arraycopy(costMatrix[i+1], 0, costMatrixVector, costMatrix[i].length, costMatrix[i+1].length);
            }

            x = ampl.getParameter("cTransp");
            x.setValues(costMatrixVector);

            // --------------------------------
            ampl.solve(); // solve model to get supply plan

        } catch (java.io.IOException e){
            System.out.println("File not found!");
        }

        if(ampl.getObjective("f_celu").value() == 0){
            System.out.println("Brak rozwiązania! ograniczenia niespełnione");
        } else {
            Variable v = ampl.getVariable("xTransp");
            //System.out.println(ampl.getParameter("prod").getValues());
            //System.out.print(v.getValues());

            double[] valColumn = v.getValues().getColumnAsDoubles("val"); // values of xTransp

            //System.out.println(valColumn[1]);

            // convert model results to supply plan
            supplyPlan = new double[N][N];
            int index=0;
            for(int i=0; i<N; ++i){
                supplyPlan[i] = new double[N];
                for(int j=0; j<N; ++j) {
                    supplyPlan[i][j] = valColumn[index];
                    ++index;
                }
            }

            System.out.println("Supply plan:");
            for(int i=0; i<N; ++i){
                for(int j=0; j<N; ++j){
                    System.out.print(supplyPlan[i][j] + " ");
                }
                System.out.println();
            }

            sendSupplyPlan(); // inform clients

        }

    }

    private void createSupplyPlan(){
        N = clients.size();
        System.out.println("N = " + N);
        int i=0;
        System.out.println("Otrzymałem następujące oferty:");
        for(ActorRef client: clients){
            System.out.println(clients.get(i) + " " + demands.get(i) + " " + production.get(i));
            ++i;
        }

        network.tell(new CostMatrix(getSelf(), clients, null), getSelf()); // ask for cost matrix

    }

    private void sendSupplyPlan(){
        int k = 0;
        for(ActorRef actor: clients){
            double demand=0;
            for(int i=0; i<N; ++i){
                demand += supplyPlan[i][k];
            }
            ++k;

            Offer msg = new Offer(actor, demand, 0.0);
            actor.tell(msg, actor);
        }
    }

    @Override
    public void preStart(){

        getContext().getSystem().scheduler().schedule(
                Duration.create(100, TimeUnit.MILLISECONDS), // Initial delay 100 milliseconds
                Duration.create(3, TimeUnit.SECONDS),     // Frequency 3 seconds
                super.getSelf(), // Send the message to itself
                "createSupplyPlan",
                getContext().getSystem().dispatcher(),
                null
                );


        /*
           getContext().getSystem().scheduler().schedule(
           Duration.create(100, TimeUnit.MILLISECONDS), // Initial delay 100 milliseconds
           Duration.create(4, TimeUnit.SECONDS),     // Frequency 2 seconds
           super.getSelf(), // Send the message to itself
           "sendSupplyPlan",
           getContext().getSystem().dispatcher(),
           null
           );
           */
    }

    private void receiveOffer(Offer msg){

        log.info(msg.getSender() + " " + msg.getDemand() + " " + msg.getProduction() );

        if(clients == null || clients.isEmpty()){ clients = new ArrayList<ActorRef>(); }

        if(demands == null || demands.isEmpty()){ demands = new ArrayList<Double>(); }

        if(production == null || production.isEmpty()){ production = new ArrayList<Double>(); }

        clients.add(msg.getSender());
        demands.add(msg.getDemand());
        production.add(msg.getProduction());

        Status status = new Status(getSelf(), Status.StatusType.OFFER_ACK);
        msg.getSender().tell(status, ActorRef.noSender());

    }

    private void receiveStatus(Status msg){
        if(msg.getStatus() == Status.StatusType.DECLARE_NETWORK){
            this.network = msg.getSender();
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Offer.class, this::receiveOffer)
            .match(Status.class, this::receiveStatus)
            .matchEquals("createSupplyPlan", m -> createSupplyPlan())
            .matchEquals("sendSupplyPlan", m -> sendSupplyPlan())
            .match(CostMatrix.class, this::amplDemo)
            .build();
    }

}
