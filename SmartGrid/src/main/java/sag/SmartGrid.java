package sag;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import sag.actors.DeadLetterMonitor;
import sag.actors.SupervisorsMaster;
import sag.messages.StatusInfo;
import sag.utils.ActorCreator;

import java.io.IOException;
import java.util.Map;

public class SmartGrid {
    public static void main( String[] args ) {
        final ActorSystem system = ActorSystem.create("SmartGridSystem");

        final ActorRef deadLetterMonitor = system.actorOf(DeadLetterMonitor.props(), "monitor");
        system.eventStream().subscribe(deadLetterMonitor, DeadLetter.class);

        system.actorOf(SupervisorsMaster.props(), "supervisorsMaster");

        try {
            final ActorCreator creator = new ActorCreator(system);
            final Map<String, ActorRef> supervisors = creator.createSupervisors("supervisors.txt");
            final Map<String, ActorRef> networks = creator.createNetworks("networks.txt", supervisors);
            creator.createClients("clients.txt", supervisors, networks);

            Thread.sleep(4000);

            // symulacja awarii nadzorcy
            supervisors
                .get("supervisor1")
                .tell(new StatusInfo(StatusInfo.StatusType.KILL), ActorRef.noSender());
        } catch (final IOException exc) {
            System.out.println(exc.getMessage());
            system.terminate();
        } catch (final InterruptedException ie) {
            System.out.println("Przerwano działanie programu.");
            system.terminate();
        }

    }
}
