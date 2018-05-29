package sag.messages;

/**
 * Rodzaj wiadomości przesyłany pomiędzy nadzorcą a klientami, którzy wcześniej przesłali mu ofertę.
 * W ramach wiadomości przekazywane są wartości:
 * a) ile medium otrzyma klient,
 * b) ile mudium udało się przesłać pozostałym klientom w sieci.
 */
public class Settlement {
    public final double mediumReceived, mediumSent;

    public Settlement(double received, double sent) {
        this.mediumReceived = received;
        this.mediumSent = sent;
    }
}
