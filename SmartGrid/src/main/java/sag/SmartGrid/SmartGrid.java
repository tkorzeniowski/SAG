package sag.SmartGrid;

import java.io.IOException;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
//import sag.SAG.Greeter;
//import sag.SAG.Printer;
//import sag.SAG.Greeter.Greet;
//import sag.SAG.Greeter.WhoToGreet;

/**
 * Hello world!
 *
 */
public class SmartGrid 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        
        final ActorSystem system = ActorSystem.create("SmartGridSystem");
        
        try {
        	final ActorRef control = system.actorOf(ControlCenter.props(), "control");
        	final ActorRef producer = system.actorOf(Producer.props(50.5, control), "producer");
        	final ActorRef client = system.actorOf(Client.props(20.2, control), "client");
        	
        	
        	System.out.println(">>> Press ENTER to exit <<<");
        	System.in.read();
        	
        } catch (IOException ioe) {
        } finally {
          system.terminate();
        }
        
        
    }
}
