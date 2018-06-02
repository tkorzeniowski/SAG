package sag.messages;

/**
 * Rodzaj wiadomości przesyłanych pomiędzy różnymi aktorami w celu przekazania informacji
 * o swoim statusie. Swój status może przesyłać sieć do swojego nadzorcy w celu zadeklarowania
 * swojego istnienia, klient do sieci w celu przesłania swojego położenia oraz nadzorca
 * do klienta w celu potwierdzenia przyjęcia oferty.
 */
public class StatusInfo {
    public final StatusType status;

    public enum StatusType {
        OFFER_ACK, // offer accepted by supervisor
        SEND_LOCATION, // if anyone asks, client sends its location
        DECLARE_NETWORK, // network is assigned to its supervisor
        GET_NEIGHBOURS, // supervisor looks for other (nearest) supervisors
        REQUEST_MEDIUM, // supervisor needs medium from other supervisors
        KILL, // supervisor throws exception to simulate error
        GET_SUPERVISOR_MASTER // supervisor needs SupervisorsMaster to backup his state
    };

    public StatusInfo(final StatusType status) {
        this.status = status;
    }
}