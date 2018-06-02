package sag.messages;

/**
 * Rodzaj wiadomości przesyłany pomiędzy nadzorcą a klientami, którzy wcześniej przesłali mu ofertę.
 * W ramach wiadomości przekazywane są wartości:
 * a) ile medium otrzyma klient,
 * b) ile mudium udało się przesłać pozostałym klientom w sieci.
 */
public class RequestMedium {
    public boolean returnMedium; // false - przyjęcie oferty, true - zwrot nadmiaru
    public final double offer;

    public RequestMedium(double offer, boolean returnMedium) {
        this.offer = offer;
        this.returnMedium = returnMedium;
    }
}
