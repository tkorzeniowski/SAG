package sag.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import sag.messages.AnnounceCostMatrix;
import sag.messages.Offer;
import sag.messages.RequestCostMatrix;
import sag.messages.StatusInfo;
import sag.model.ClientOffer;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Supervisor extends AbstractActor {

    private List<ClientOffer> clientOffers = new ArrayList<>();
    private ActorRef network;
    private double[][] costMatrix, supplyPlan;
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);


    static public Props props() {
        return Props.create(Supervisor.class, Supervisor::new);
    }

    private void receiveCostMatrix(final AnnounceCostMatrix cm) {

    }

    /**private void amplDemo(CostMatrix cm){
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

    }*/

    private void createSupplyPlan() {
        int n = clientOffers.size();
        System.out.println("N = " + n);
        System.out.println("Otrzymałem następujące oferty:");
        for (ClientOffer co: clientOffers) {
            System.out.println(co.client() + " " + co.demand() + " " + co.production());
        }
        network.tell(new RequestCostMatrix(), getSelf()); // ask for cost matrix
    }

    private void sendSupplyPlan() {
        int n = clientOffers.size();
        for (int k = 0; k < n; ++k) {
            double demand = 0.0f;
            for (int i = 0; i < n; ++i) {
                demand += supplyPlan[i][k];
            }
            Offer msg = new Offer(demand, 0.0);
            clientOffers.get(k).client().tell(msg, getSelf()); // tu zmieniłam nadawcę na nadzorcę zamiast klienta
        }
    }

    @Override
    public void preStart() {

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

    private void receiveOffer(Offer msg) {
        log.info(this.sender() + " " + msg.demand + " " + msg.production);

        clientOffers.add(new ClientOffer(this.sender(), msg.demand, msg.production));

        StatusInfo status = new StatusInfo(StatusInfo.StatusType.OFFER_ACK);
        this.sender().tell(status, ActorRef.noSender());
    }

    private void receiveStatus(StatusInfo msg) {
        if(msg.status == StatusInfo.StatusType.DECLARE_NETWORK) {
            this.network = this.sender();
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Offer.class, this::receiveOffer)
            .match(StatusInfo.class, this::receiveStatus)
            .matchEquals("createSupplyPlan", m -> createSupplyPlan())
            .matchEquals("sendSupplyPlan", m -> sendSupplyPlan())
            .match(AnnounceCostMatrix.class, this::receiveCostMatrix)
            .build();
    }
}
