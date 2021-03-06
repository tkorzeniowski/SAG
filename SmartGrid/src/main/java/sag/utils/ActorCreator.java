package sag.utils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import sag.actors.Client;
import sag.actors.Network;
import sag.actors.Supervisor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Klasa pomocnicza pozwalająca na wczytywanie z plików aktorów konkretnego typu.
 */
public class ActorCreator {
    private ActorSystem actorSystem;
    private ClassLoader classLoader = ActorCreator.class.getClassLoader();

    public ActorCreator(final ActorSystem system) {
        this.actorSystem = system;
    }

    /**
     * Utworzenie map z nazw aktorów do aktorów na podstawie pliku o wskazanej nazwie.
     * @param fileName Nazwa pliku przechowująca nazwy nadzorców.
     * @return Mapa nazwa-nadzorca.
     */
    public Map<String, ActorRef> createSupervisors(final String fileName) throws IOException {
        Stream<String> lines = resourceLines(fileName);
        return lines
            .map(line -> line.split(" "))
            .map(parts -> {
                String supervisorName = parts[0];
                Double xCoord = Double.parseDouble(parts[1]);
                Double yCoord = Double.parseDouble(parts[2]);
                return new Pair<>(
                        supervisorName,
                        actorSystem.actorOf(Supervisor.props(xCoord, yCoord), supervisorName)
                );
            })
            .collect(Collectors.toMap(Pair::first, Pair::second));
    }

    /**
     * Utworzenie map z nazw sieci do sieci na podstawie pliku o wskazanej nazwie.
     * @param fileName Nazwa pliku przechowująca nazwy sieci oraz przypisanych im nadzorców.
     * @param supervisors Mapa nazwa-nadzorca.
     * @return Mapa nazwa-sieć.
     */
    public Map<String, ActorRef> createNetworks(final String fileName,
                                                final Map<String, ActorRef> supervisors) throws IOException {
        Stream<String> lines = resourceLines(fileName);
        return lines
            .map(line -> line.split(" "))
            .map(pair -> {
                String networkName = pair[0];
                String supervisorName = pair[1];
                return new Pair<>(
                    networkName,
                    actorSystem.actorOf(Network.props(supervisors.get(supervisorName)), networkName)
                );
            })
            .collect(Collectors.toMap(Pair::first, Pair::second));
    }

    /**
     * Utworzenie map z nazw klienta do klienta na podstawie pliku o wskazanej nazwie.
     * @param fileName Nazwa pliku przechowująca nazwy klientów oraz przypisane im
     *                 zapotrzebowanie na medium, produkcję medium, położenie reprezentowane
     *                 przez dwie współrzędne, nadzorców oraz sieci.
     * @param supervisors Mapa nazwa-nadzorca.
     * @param networks Mapa nazwa-sieć.
     * @return Mapa nazwa-klient.
     */
    public Map<String, ActorRef> createClients(final String fileName,
                                               final Map<String, ActorRef> supervisors,
                                               final Map<String, ActorRef> networks) throws IOException {
        Stream<String> lines = resourceLines(fileName);
        return lines
            .map(line -> line.split(" "))
            .map(parts -> {
                String clientName = parts[0];
                Double demand = Double.parseDouble(parts[1]);
                Double production = Double.parseDouble(parts[2]);
                Double xCoord = Double.parseDouble(parts[3]);
                Double yCoord = Double.parseDouble(parts[4]);
                String supervisorName = parts[5];
                String networkName = parts[6];
                return new Pair<>(
                    clientName,
                    actorSystem.actorOf(
                        Client.props(
                                demand,
                                production,
                                xCoord,
                                yCoord,
                                supervisors.get(supervisorName),
                                networks.get(networkName)
                        ), clientName)
                );
            })
            .collect(Collectors.toMap(Pair::first, Pair::second));
    }

    private Stream<String> resourceLines(final String fileName) throws IOException {
        final InputStream is = classLoader.getResourceAsStream(fileName);
        if (is == null) {
            throw new IOException("Nie udało się wczytać pliku bądź plik nie istnieje: " + fileName);
        }
        return new BufferedReader(new InputStreamReader(is)).lines();
    }
}
