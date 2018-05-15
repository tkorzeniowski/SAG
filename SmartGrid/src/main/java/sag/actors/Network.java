package sag.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import sag.model.CostMatrix;
import sag.model.Location;
import sag.messages.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Aktor reprezentujący sieć przesyłową. Komunikuje się z aktorem nadzorcą
 * przekazując mu macierz kosztów przesłania medium pomiędzy klientami,
 * wyznaczanej na podstawie ich położeń.
 */
public class Network extends AbstractActor {

    private final static double infinity = 1e10;

    // Sieć przechowuje aktualne listy wszystkich swoich klientów
    // oraz ich położeń (osobno współrzędne x i y), początkowo puste.
    private List<ActorRef> clients = new ArrayList<>();
    private List<Double> xLocation = new ArrayList<>();
    private List<Double> yLocation = new ArrayList<>();
    private ActorRef networkSupervisor;

    /**
     * Klasa konfigurująca określająca sposób tworzenia aktora klasy Network.
     * @param supervisor Aktor nadzorcy przypisany danemu aktorowi sieci.
     * @return Obiekt konfiguracji aktora sieci.
     */
    static public Props props(ActorRef supervisor) {
        return Props.create(Network.class, () -> new Network(supervisor));
    }

    /**
     * Konstruktor klasy sieć. Informuje przypisanego nadzorcę o swoim istnieniu.
     * @param supervisor Aktor nadzorcy przypisany danemu aktorowi sieci.
     */
    public Network(ActorRef supervisor) {
        this.networkSupervisor = supervisor;
        sendStatus(Status.StatusType.DECLARE_NETWORK);
    }

    /*
     * Przesyła wiadomość z informacją o statusie sieci do przypisanego nadzorcy.
     */
    private void sendStatus(Status.StatusType statusType) {
        Status msg = new Status(getSelf(), statusType);
        networkSupervisor.tell(msg, getSelf());
    }

    /*
     * Na postastawie wiadomości otrzymanych od klientów uzupełnia wewnętrzne listy
     * klientów oraz ich położeń, które pozwalają na skonstruowanie macierzy kosztów.
     */
    private void receiveLocation(Location msg) {
        clients.add(msg.getSender());
        xLocation.add(msg.getX());
        yLocation.add(msg.getY());
    }

    private double[][] calculateCostMatrix(List<ActorRef> cList) {
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

    private void sendCostMatrix(CostMatrix msg) {
        double[][] cm = calculateCostMatrix(msg.getClients());
        CostMatrix cmMsg = new CostMatrix(getSelf(),null, cm);
        networkSupervisor.tell(cmMsg, getSelf());
    }

    @Override
    public void preStart() {
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
                .build();
	}

}