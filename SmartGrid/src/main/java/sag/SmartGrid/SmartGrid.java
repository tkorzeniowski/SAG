package sag.SmartGrid;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;

//import sag.SAG.Greeter;
//import sag.SAG.Printer;
//import sag.SAG.Greeter.Greet;
//import sag.SAG.Greeter.WhoToGreet;
/**
 * Hello world!
 */

public class SmartGrid
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        
        final ActorSystem system = ActorSystem.create("SmartGridSystem");
        
        try {
        	final ActorRef control = system.actorOf(Supervisor.props(), "control");
        	//final ActorRef network = system.actorOf(Network.props(50.5, control), "producer");
            final ActorRef client1 = system.actorOf(Client.props(20.2, 0, 1, 1, control), "client1");
        	final ActorRef client2 = system.actorOf(Client.props(21.2, 5, 2, 1, control), "client2");
            final ActorRef client3 = system.actorOf(Client.props(22.2, 10, 3, 1, control), "client3");
            final ActorRef client4 = system.actorOf(Client.props(23.2, 0, 4, 1, control), "client4");
            final ActorRef client5 = system.actorOf(Client.props(24.2, 0, 5, 1, control), "client5");
        	
        	
        	System.out.println(">>> Press ENTER to exit <<<");
        	System.in.read();
        	
        } catch (IOException ioe) {
        } finally {
          system.terminate();
        }
        
        
    }
}
