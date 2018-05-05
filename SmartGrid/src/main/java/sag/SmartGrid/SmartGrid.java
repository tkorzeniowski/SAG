package sag.SmartGrid;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SmartGrid
{
    private static ActorSystem system;
    private static List<ActorRef> supervisors, networks, clients;


    public static void main( String[] args )
    {
        system = ActorSystem.create("SmartGridSystem");

        SmartGrid sg = new SmartGrid();
        sg.getActors();

        /*
        try {
        	final ActorRef supervisor = system.actorOf(Supervisor.props(), "supervisor");
        	final ActorRef network = system.actorOf(Network.props(supervisor), "network");
            final ActorRef client1 = system.actorOf(Client.props(0.0, 100.0, 1, 1, supervisor, network), "client1");
        	final ActorRef client2 = system.actorOf(Client.props(50.0, 0.0, 2, 1, supervisor, network), "client2");
            final ActorRef client3 = system.actorOf(Client.props(25.0, 0.0, 3, 1, supervisor, network), "client3");
            final ActorRef client4 = system.actorOf(Client.props(17.0, 0.0, 4, 1, supervisor, network), "client4");
            final ActorRef client5 = system.actorOf(Client.props(8.0, 0.0, 5, 1, supervisor, network), "client5");
        	
        	
        	//System.out.println(">>> Press ENTER to exit <<<");
        	System.in.read();
        	
        } catch (IOException ioe) {
        } finally {
          system.terminate();
        }
        */
        
    }

    private void getActors(){
        ClassLoader classLoader = getClass().getClassLoader();

        // get supervisors
        File supervisorsFile = new File(classLoader.getResource("supervisors.txt").getFile());
        supervisors = new ArrayList<ActorRef>();
        try (Scanner scanner = new Scanner(supervisorsFile)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                supervisors.add(system.actorOf(Supervisor.props(), line));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        System.out.println("Moi nadzorcy");
        for(ActorRef a : supervisors){
            System.out.println(a.toString());
            String s = a.toString();
            System.out.println(s.substring(s.lastIndexOf("/") + 1, s.indexOf("#")));
        }
        */

        // get networks
        File networksFile = new File(classLoader.getResource("networks.txt").getFile());
        networks = new ArrayList<ActorRef>();
        try (Scanner scanner = new Scanner(networksFile)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] param = line.split(" ");

                for(ActorRef sup : supervisors){
                    String s = sup.toString();
                    if(s.substring(s.lastIndexOf("/") + 1, s.indexOf("#")).equals(param[1])){
                        networks.add(system.actorOf(Network.props(sup), param[0]));
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        System.out.println("Moje sieci:");
        for(ActorRef net : networks){
            System.out.println(net.toString());
            String s = net.toString();
            System.out.println(s.substring(s.lastIndexOf("/") + 1, s.indexOf("#")));
        }
        */

        //get clients
        File clientsFile = new File(classLoader.getResource("clients.txt").getFile());
        clients = new ArrayList<ActorRef>();
        try (Scanner scanner = new Scanner(clientsFile)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] param = line.split(" ");

                int i=0;
                for(ActorRef sup : supervisors){
                    String s = sup.toString();
                    if(s.substring(s.lastIndexOf("/") + 1, s.indexOf("#")).equals(param[5])){
                        break;
                    } else {
                        ++i;
                    }
                }

                int j = 0;
                for(ActorRef net : networks){
                    String s = net.toString();
                    if(s.substring(s.lastIndexOf("/") + 1, s.indexOf("#")).equals(param[6])){
                        break;
                    }else{
                        ++j;
                    }
                }

                clients.add(system.actorOf(Client.props(Double.parseDouble(param[1]), Double.parseDouble(param[2]), Double.parseDouble(param[3]), Double.parseDouble(param[4]), supervisors.get(i), networks.get(j)), param[0]));

            }

            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        System.out.println("Moi klienci:");
        for(ActorRef c : clients){
            System.out.println(c.toString());
            String s = c.toString();
            System.out.println(s.substring(s.lastIndexOf("/") + 1, s.indexOf("#")));
        }
        */

    }


}
