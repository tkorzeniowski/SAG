package sag.SmartGrid;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import messages.CostMatrix;
import messages.Location;
import messages.Status;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Network extends AbstractActor{

    private double infinity = 1e10;
    //private double[][] costMatrix;
    private List<ActorRef> clients;
    private List<Double> xLocation, yLocation;
    private ActorRef networkSupervisor;

    static public Props props(ActorRef supervisor) {
        return Props.create(Network.class, () -> new Network(supervisor));
    }

    public Network(ActorRef supervisor){
        this.networkSupervisor = supervisor;
        informSupervisor();
    }

    private void informSupervisor(){
        Status msg = new Status(getSelf(), Status.StatusType.DECLARE_NETWORK);
        networkSupervisor.tell(msg, getSelf());
    }

    private void receiveLocation(Location msg){
        if(clients == null || clients.isEmpty()){ clients = new ArrayList<ActorRef>(); }

        if(xLocation == null || xLocation.isEmpty()){ xLocation = new ArrayList<Double>(); }

        if(yLocation == null || yLocation.isEmpty()){ yLocation = new ArrayList<Double>(); }

        clients.add(msg.getSender());
        xLocation.add(msg.getX());
        yLocation.add(msg.getY());

    }

    /*
    private void calculateCostMatrix(){
        costMatrix = new double[clients.size()][clients.size()];

        for(int i = 0; i<clients.size(); ++i){
            costMatrix[i] = new double[clients.size()];

            for(int j = 0; j< clients.size(); ++j){
                costMatrix[i][j] = Math.sqrt(Math.pow(xLocation.get(i).doubleValue() - xLocation.get(j).doubleValue(), 2) + Math.pow(yLocation.get(i).doubleValue() - yLocation.get(j).doubleValue(), 2));
                costMatrix[j][i] = costMatrix[i][j];
            }

            costMatrix[i][i] = infinity;
        }

        System.out.println("Macierz kosztów:");
        for(int i = 0; i<clients.size(); ++i){
            for(int j = 0; j< clients.size(); ++j){
                System.out.print(costMatrix[i][j] + " ");
            }
            System.out.println();
        }

    }
    */

    private double[][] calculateCostMatrix(List<ActorRef> cList){
        double[][] cm = new double[cList.size()][cList.size()];

        List<ActorRef> tmpClients;
        List<Double> x, y;

        tmpClients = new ArrayList<ActorRef>();
        x = new ArrayList<Double>();
        y = new ArrayList<Double>();

        for(ActorRef tc : cList){
            int i = 0;
            for(ActorRef client : clients){
                if(tc.compareTo(client) == 0){
                    tmpClients.add(client);
                    x.add(xLocation.get(i).doubleValue());
                    y.add(yLocation.get(i).doubleValue());
                    break;
                }
                ++i;
            }
        }

        for(int i = 0; i<cList.size(); ++i){
            cm[i] = new double[cList.size()];

            for(int j = 0; j< cList.size(); ++j){
                cm[i][j] = Math.sqrt(Math.pow(x.get(i).doubleValue() - x.get(j).doubleValue(), 2) + Math.pow(y.get(i).doubleValue() - y.get(j).doubleValue(), 2));
                cm[j][i] = cm[i][j];
            }

            cm[i][i] = infinity;
        }

        return cm;
    }

    private void sendCostMatrix(CostMatrix msg){
        double[][] cm = calculateCostMatrix(msg.getClients());
        CostMatrix cmMsg = new CostMatrix(getSelf(),null, cm);
        networkSupervisor.tell(cmMsg, getSelf());
    }

    @Override
    public void preStart(){
        /*
        getContext().getSystem().scheduler().schedule(
                Duration.create(2, TimeUnit.SECONDS), // Initial delay 2 seconds
                Duration.create(0, TimeUnit.SECONDS),     // Frequency 0 seconds
                super.getSelf(), // Send the message to itself
                "createCostMatrix",
                getContext().getSystem().dispatcher(),
                null
        );
        */

    }

    @Override
	public Receive createReceive() {
		return receiveBuilder()
                .match(Location.class, this::receiveLocation)
                .match(CostMatrix.class, this::sendCostMatrix)
                //.matchEquals("createCostMatrix", m -> calculateCostMatrix())
                .build();
	}

}