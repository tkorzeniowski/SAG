package sag;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import sag.actors.DeadLetterMonitor;
import sag.actors.SupervisorsMaster;
import sag.messages.StatusInfo;
import sag.utils.ActorCreator;

import java.util.Map;

public class SmartGrid {
    public static void main( String[] args ) {
        final ActorSystem system = ActorSystem.create("SmartGridSystem");

        final ActorRef deadLetterMonitor = system.actorOf(DeadLetterMonitor.props(), "monitor");
        system.eventStream().subscribe(deadLetterMonitor, DeadLetter.class);

        final ActorRef supervisorsMaster = system.actorOf(SupervisorsMaster.props(), "supervisorsMaster");

        final ActorCreator creator = new ActorCreator(system);
        final Map<String, ActorRef> supervisors = creator.createSupervisors("supervisors.txt");
        final Map<String, ActorRef> networks = creator.createNetworks("networks.txt", supervisors);
        creator.createClients("clients.txt", supervisors, networks);

        try{
            Thread.sleep(4000);
        }catch (InterruptedException ie){

        }
        // symulacja awarii nadzorcy
        supervisors.get("supervisor1").tell(new StatusInfo(StatusInfo.StatusType.KILL), ActorRef.noSender());
    }
}
