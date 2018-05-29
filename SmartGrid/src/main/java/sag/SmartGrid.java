package sag;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import sag.utils.ActorCreator;

import java.util.Map;

public class SmartGrid {
    public static void main( String[] args ) {
        final ActorSystem system = ActorSystem.create("SmartGridSystem");
        final ActorCreator creator = new ActorCreator(system);
        final Map<String, ActorRef> supervisors = creator.createSupervisors("supervisors.txt");
        final Map<String, ActorRef>networks = creator.createNetworks("networks.txt", supervisors);
        creator.createClients("clients.txt", supervisors, networks);
    }
}
