package sag.messages;

import akka.actor.ActorRef;

import java.util.ArrayList;

/**
 * Rodzaj wiadomości przesyłanej przez nadzorcę do swojej sieci
 * w celu zgłoszenia zapotrzebowania na gotową macierz kosztów.
 */
public class RequestCostMatrix {
    private ArrayList<ActorRef> clients;
    public RequestCostMatrix(ArrayList<ActorRef> clients) { this.clients = clients; }

    public ArrayList<ActorRef> getClients() {
        return clients;
    }
}
