package sag.actors;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Pair;
import org.apache.log4j.Logger;
import sag.messages.*;
import sag.model.ClientLocation;
import sag.model.ClientOffer;
import sag.model.CostMatrix;
import sag.model.SupervisorState;
import sag.utils.SupplyPlanOptimizer;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Aktor reprezentujący nadzorcę. Komunikuje się z sieciami przesyłowymi w celu uzyskania
 * macierzy odległości, które później wykorzystuje do wyznaczenia macierzy kosztów
 * i przygotowania planu dostaw. Zbiera w tym celu oferty od powiązanych klientów,
 * a po ustaleniu planu, przesyła im potwierdzenia.
 */
public class Supervisor extends AbstractActor {

    // Nadzorca przechowuje listę ofert, zawierających informacje o klientach
    // oraz ofertach, jakie zgłosili, obejmujących ich zapotrzebowanie i produkcję medium.
    private List<ClientOffer> clientOffers = new ArrayList<>();
    private ActorRef network, master;
    private CostMatrix supplyPlan;
    private Pair<Double, Double> location;
    private List<ClientLocation>  neighbours = new ArrayList<>();
    private double currentProduction = 0, currentDemand = 0;
    private List<ActorRef> rejectedMediumRequest = new ArrayList<>();
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final int MAX_NEIGHBOURS = 2;
    //private org.apache.log4j.Logger log = Logger.getLogger(Supervisor.class);

    /**
     * Klasa konfigurująca określająca sposób tworzenia aktora klasy Supervisor.
     * @param x Położenie nadzorcy - współrzędna x.
     * @param y Położenie nadzorcy - współrzędna y.
     * @return Obiekt konfiguracji aktora nadzorcy.
     */
    static public Props props(double x, double y) {
        return Props.create(Supervisor.class, () -> new Supervisor(x, y));
    }

    /**
     * Tworzenie nadzorcy na podstawie współrzędnych. Położenie nadzorcy rozumiane jest niedosłownie,
     * ale jako położenie głównego węzła w sieci, która podlega danemu nadzorcy.
     * @param x Położenie nadzorcy - współrzędna x.
     * @param y Położenie nadzorcy - współrzędna y.
     */
    private Supervisor (double x, double y) {
        this.location = new Pair<>(x, y);
        getContext()
            .actorSelection("../*")
            .tell(new StatusInfo(StatusInfo.StatusType.GET_SUPERVISOR_MASTER), getSelf());
    }

    /*
     * Zapis nadrzędnego nadzorcy, by wysyłać mu kopię stanu przed awarią.
     */
    private void saveMaster() { master = this.sender(); }

	/*
     * Reakcja na otrzymanie od sieci gotowej macierzy odległości. Na jej podstawie
     * oraz na podstawie zgromadzonych ofert wyznaczany jest plan dostaw.
     */
    private void receiveCostMatrix(final AnnounceCostMatrix cm) {
        supplyPlan = SupplyPlanOptimizer.optimize(cm.costMatrix, clientOffers);										   
        if (supplyPlan.costVector.length == 0) {
            log.info("Nie udało się wyznaczyć planu dostaw.");
            createSupplyPlan();
        }
    }
	
	/*
     * Zgłasza do sieci zapotrzebowanie na macierz odległości pomiędzy klientami
     * w celu wyznaczenia macierzy kosztów przesyłu.
     */
    private void createSupplyPlan() {
        currentDemand = 0;
        currentProduction = 0;
        int n = clientOffers.size();

        //log.info("Otrzymałem następujące oferty (" + n + "): ");

        ArrayList<ActorRef> c = new ArrayList<>();
        for (ClientOffer co: clientOffers) {
            //log.info(co.client() + " " + co.demand() + " " + co.production());
            currentDemand += co.demand();
            currentProduction += co.production();
            c.add(co.client());
        }

        log.info("Zaczynam wyznaczać plan: cd= " + currentDemand + " cp= " + currentProduction + " n=" + n);
        if (n > 0 && currentProduction >= currentDemand) {
            network.tell(new RequestCostMatrix(c), getSelf()); // ask for cost matrix
        } else {
            startNegotiations(); // more medium required from other supervisors
        }
    }

    /**
     * W obszarze brakuje medium, potrzeba rozpoczęcia negocjacji z innymi obszarami.
     */
    private void startNegotiations() {
        log.info("Rozpoczynam negocjacje");
        for (ClientLocation neighbour : neighbours) {
            neighbour
                .getClient()
                .tell(new StatusInfo(StatusInfo.StatusType.REQUEST_MEDIUM), getSelf());
        }
    }

    /*
     * Wysyłanie medium do innego obszaru
     */
    private void sendMedium(ActorRef sender) {
        if (currentProduction > currentDemand) {
            ClientLocation neighbour = null;
            for (ClientLocation cl : neighbours) {
                if (cl.getClient() == sender) {
                    neighbour = cl;
                    break;
                }
            }
            System.out.println(getSelf() + "moge wyslac " + (currentProduction-currentDemand));
            network.tell(new AnnounceLocation(neighbour.getLocation()), neighbour.getClient());
            clientOffers.add(new ClientOffer(neighbour.getClient(), currentProduction-currentDemand, 0.0 ));
            sender.tell(new RequestMedium(currentProduction-currentDemand, false), getSelf());
            currentDemand = currentProduction;
        } else {
            sender.tell(new RequestMedium(0.0, false), getSelf());
        }
    }

    /*
    * Nadzorca otrzymuje od innych nadzorców medium. Jeśli ilość otrzymanego medium jest niezerowa to całość medium
    * zostaje przyjęte przez nadzorcę lub cześć nadmiarowa zostaje odesłana. Jeśli ilość otrzymanego medium = 0 to
    * nadzorca, który nie może pożyczyć (bezzwrotnie) medium zostaje zapisany. Gdy żaden z nazdorców nie może przesłać
    * medium zostaje podana stosowna informacja.*/
    private void receiveMedium(RequestMedium requestMedium) {
        log.info(this.getSender() + " reqMed " + requestMedium.offer + " " + requestMedium.returnMedium);
        if (requestMedium.offer > 0) {
            for (ClientLocation cl : neighbours) {
                if(cl.getClient() == this.sender()) {
                    network.tell(new AnnounceLocation(cl.getLocation()), cl.getClient());
                    break;
                }
            }

            if (!requestMedium.returnMedium) {
                if (currentProduction + requestMedium.offer <= currentDemand ) {   // wciąż brakuje medium
                    log.warning("Wciąż brakuje medium.");
                    //log.warn("Wciąż brakuje medium.");
                    clientOffers.add(new ClientOffer(this.sender(), 0.0, requestMedium.offer));
                    currentProduction += requestMedium.offer;
                } else if (currentProduction + requestMedium.offer > currentDemand) { // dostaliśmy więcej niż potrzebujemy
                    log.warning("Przyszła nadwyzka, oddaję nadmiar.");
                    //log.warn("Przyszła nadwyzka, oddaję nadmiar.");
                    double overproduction = currentProduction + requestMedium.offer - currentDemand;
                    clientOffers.add(new ClientOffer(this.sender(), 0.0, requestMedium.offer - (0.98 * overproduction)));
                    this.sender().tell(new RequestMedium(0.98*overproduction, true), getSelf());
                    currentProduction = currentDemand;
                }
            } else if(requestMedium.returnMedium) {
                for (ClientOffer co : clientOffers) {
                    if (co.client() == this.sender()) {
                        if (co.demand() == requestMedium.offer) {
                            clientOffers.remove(co);
                        } else {
                            co.setOffer(co.demand() - requestMedium.offer, co.production());
                        }
                        currentDemand -= requestMedium.offer;
                    }
                }
            }
            createSupplyPlan();

        } else {
            if (!rejectedMediumRequest.contains(this.sender())) {
                rejectedMediumRequest.add(this.sender());
            }

            if (rejectedMediumRequest.size() == neighbours.size()) {
                // call MasterSupervisor or rather accept more neighbours from start
                log.error("Nie da sie wyznaczyc planu dostaw w moim obszarze.");
                log.info(rejectedMediumRequest.toString());
            }
        }
    }

    /*
     * Przesyła klientom wiadomości zawierające informacje, ile przekazano im medium
     * oraz ile wyprodukowanego medium zostało rozesłane do innych klientów
     * (odpowiednio zerowe wartości medium otrzymanego dla producenta i wyprodukowanego dla konsumenta).
     */
    private void sendSupplyPlan() {
        int n = clientOffers.size();
        if(supplyPlan.costVector.length == 0){
            createSupplyPlan();
        }else {
            supplyPlan.printCostMatrix();
            for (int k = 0; k < n; ++k) {
                double demand = 0.0;
                for (int i = 0; i < n; ++i) {
                    demand += supplyPlan.costVector[k + n * i];
                }
                Offer msg = new Offer(demand, 0.0);
                clientOffers.get(k).client().tell(msg, getSelf());
            }
        }
        clientOffers = new ArrayList<>();
    }

    /**
     * Symulacja okresowości działania nadzorcy.
     */
    @Override
    public void preStart() {
        getContext().getSystem().scheduler().scheduleOnce(
                Duration.create(100, TimeUnit.MILLISECONDS), // Initial delay 150 milliseconds
                super.getSelf(), // Send the message to itself
                "getNeighbours",
                getContext().getSystem().dispatcher(),
                null
        );

        getContext().getSystem().scheduler().scheduleOnce(
                Duration.create(400, TimeUnit.MILLISECONDS), // Initial delay 400 milliseconds
                super.getSelf(), // Send the message to itself
                "selectNeighbours",
                getContext().getSystem().dispatcher(),
                null
        );

        getContext().getSystem().scheduler().schedule(
            Duration.create(1, TimeUnit.SECONDS), // Initial delay 500 milliseconds
            Duration.create(6, TimeUnit.SECONDS),     // Frequency 3 seconds
            super.getSelf(), // Send the message to itself
            "createSupplyPlan",
            getContext().getSystem().dispatcher(),
            null
        );

        getContext().getSystem().scheduler().schedule(
                Duration.create(5500, TimeUnit.MILLISECONDS), // Initial delay 500 milliseconds
                Duration.create(6, TimeUnit.SECONDS),     // Frequency 3 seconds
                super.getSelf(), // Send the message to itself
                "sendSupplyPlan",
                getContext().getSystem().dispatcher(),
                null
        );
    }


    /*
     * Reakcja na otrzymanie oferty od klienta. Nadzorca po przyjęciu oferty
     * przesyła klientowi potwierdzenie.
     */
    private void receiveOffer(Offer msg) {


        boolean supervisorOffer = false;
        for(ClientLocation cl :  neighbours){
            if(cl.getClient() == this.sender()){
                supervisorOffer = true;
                break;
            }
        }
        if(!supervisorOffer) {
            log.info("Otrzymałem ofertę " + this.sender() + " " + msg.demand + " " + msg.production);
            clientOffers.add(new ClientOffer(this.sender(), msg.demand, msg.production));

            StatusInfo status = new StatusInfo(StatusInfo.StatusType.OFFER_ACK);
            this.sender().tell(status, ActorRef.noSender());
        }
    }

    /*
    * Reakcja nadzorcy w zależności od otrzymanego statusu.*/
    private void receiveStatus(final StatusInfo msg) {
        if(msg.status == StatusInfo.StatusType.DECLARE_NETWORK) {
            this.network = this.sender();
        }

        if(msg.status == StatusInfo.StatusType.GET_NEIGHBOURS) {
            log.info("Otrzymalem GET_NEIGHBOURS: " + getSelf() + " " + getSender());
            this.sender().tell(new AnnounceLocation(location), getSelf());
        }

        if(msg.status == StatusInfo.StatusType.REQUEST_MEDIUM) {
            sendMedium(this.sender());
        }

        if(msg.status == StatusInfo.StatusType.KILL) {
            log.warning("Wykryłem awarię");
            //log.warn("Wykryłem awarię");
            throw new NullPointerException(); // symulacja awarii
        }
    }

    /**
    * Poszukiwanie sąsiednich nadzorców (z którymi później można negocjować braki medium)
    */
    private void  getNeighbours() {
        getContext()
            .actorSelection("../*")
            .tell(new StatusInfo(StatusInfo.StatusType.GET_NEIGHBOURS), getSelf());
    }

    /**
    * Zbieranie wszystkich lokalizacji nadzorców
    */
    private void receiveLocation(AnnounceLocation msg) {
        if(this.sender() != getSelf()) {
            neighbours.add(new ClientLocation(this.sender(), msg.location));
        }
    }

    /**
    * Wybieranie najbliższych sąsiadów
    */
    private void selectNeighbours() {
        List<Double> distance = new ArrayList<>();
        for (ClientLocation cl : neighbours) {
            distance.add(Math.sqrt(Math.pow(location.first() - cl.x(), 2) + Math.pow(location.second() - cl.y(), 2)));
            log.info("print neighbours " + " " + cl.getClient().toString() + " " + distance.toString());
        }

        List<ClientLocation> nn = new ArrayList<>(); // nearest neighbours
        /*
        int n = 2; // liczba sąsiadów z którymi będziemy negocjować niedobory medium
        if(neighbours.size() < 2 ) { // jeśli w systemie jest mniej niż 5 sąsiadów
            n = neighbours.size();
        }
        */
        for (int i = 0; i < MAX_NEIGHBOURS; ++i) {
            int minIndex = distance.indexOf(Collections.min(distance));
            nn.add(neighbours.get(minIndex));
            distance.remove(minIndex);
        }

        neighbours = nn;
        //log.info(getSelf() + "selected neighbour " + nn.get(0).getClient().toString());
    }

    /*
     * Pobieranie ostatiego stanu zapisanego u mastera (wywoływane tylko przy restarcie nadzorcy)
     */
    private void loadState(SupervisorState state) {
        this.master = this.sender();
        this.clientOffers = state.getClientOffers();
        this.neighbours = state.getNeighbours();
        this.rejectedMediumRequest = state.getRejectedMediumRequest();
        this.network = state.getNetwork();
        this.supplyPlan = state.getSupplyPlan();
        this.location = state.getLocation();
    }

    @Override
    public void postRestart(Throwable reason) {
        log.info("postRestart");
    }

    /**
     * Przechowanie stanu u mastera, aby odzyskać go po restarcie.
     */
    @Override
    public void preRestart(Throwable reason, Optional<Object> message) {
        log.info("preRestart");
        master.tell(
            new SupervisorState(
                clientOffers,
                neighbours,
                rejectedMediumRequest,
                network,
                supplyPlan,
                location
            ),
            getSelf()
        );
    }

	/**
     * Reaguje na przyjęcie wiadomości od innego aktora zgodnie z zadanymi wzorcami zachowań.
     * Nadzorca jest przygotowany na otrzymywanie ofert od klienta oraz zgłoszeń od sieci.
     * Ponadto odbiera macierze odległości przesyłane przez sieci.
     * @return Sposób zachowania aktora w obliczu otrzymania wiadomości konkretnego rodzaju.
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Offer.class, this::receiveOffer)
                .match(StatusInfo.class, this::receiveStatus)
                .matchEquals("createSupplyPlan", m -> createSupplyPlan())
                .matchEquals("sendSupplyPlan", m -> sendSupplyPlan())
                .matchEquals("getNeighbours", m -> getNeighbours())
                .matchEquals("selectNeighbours", m -> selectNeighbours())
                .matchEquals("masterCall", m -> saveMaster())
                .match(AnnounceCostMatrix.class, this::receiveCostMatrix)
                .match(AnnounceLocation.class, this::receiveLocation)
                .match(RequestMedium.class, this::receiveMedium)
                .match(SupervisorState.class, this::loadState)
                .build();
    }
}
