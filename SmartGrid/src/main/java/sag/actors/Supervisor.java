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
import sag.model.CostMatrix;
import sag.utils.SupplyPlanOptimizer;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
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
    private ActorRef network;
    private CostMatrix supplyPlan;
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    /**
     * Klasa konfigurująca określająca sposób tworzenia aktora klasy Supervisor.
     * @return Obiekt konfiguracji aktora nadzorcy.
     */
    static public Props props() {
        return Props.create(Supervisor.class, Supervisor::new);
    }

    /*
     * Reakcja na otrzymanie od sieci gotowej macierzy odległości. Na jej podstawie
     * oraz na podstawie zgromadzonych ofert wyznaczany jest plan dostaw.
     */
    private void receiveCostMatrix(final AnnounceCostMatrix cm) {
        supplyPlan = SupplyPlanOptimizer.optimize(cm.costMatrix, clientOffers);
    }

    /*
     * Zgłasza do sieci zapotrzebowanie na macierz odległości pomiędzy klientami
     * w celu wyznaczenia macierzy kosztów przesyłu.
     */
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
//        int n = clientOffers.size();
//        for (int k = 0; k < n; ++k) {
//            double demand = 0.0f;
//            for (int i = 0; i < n; ++i) {
//                demand += 0.0; //supplyPlan[i][k];
//            }
//            Offer msg = new Offer(demand, 0.0);
//            clientOffers.get(k).client().tell(msg, getSelf()); // tu zmieniłam nadawcę na nadzorcę zamiast klienta
//        }
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

    /*
     * Reakcja na otrzymanie oferty od klienta. Nadzorca po przyjęciu oferty
     * przesyła klientowi potwierdzenie.
     */
    private void receiveOffer(Offer msg) {
        log.info(this.sender() + " " + msg.demand + " " + msg.production);

        clientOffers.add(new ClientOffer(this.sender(), msg.demand, msg.production));

        StatusInfo status = new StatusInfo(StatusInfo.StatusType.OFFER_ACK);
        this.sender().tell(status, ActorRef.noSender());
    }

    /*
     * Reakcja na zadeklarowanie nowej sieci przesyłowej. Nadzorca obejmuje
     * pieczę nad siecią, która wyśle zgłoszenie.
     */
    private void receiveStatus(StatusInfo msg) {
        if(msg.status == StatusInfo.StatusType.DECLARE_NETWORK) {
            this.network = this.sender();
        }
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
            .match(AnnounceCostMatrix.class, this::receiveCostMatrix)
            .build();
    }
}
