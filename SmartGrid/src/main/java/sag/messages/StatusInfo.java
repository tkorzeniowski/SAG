package sag.messages;

public class StatusInfo {

    public final StatusType status;

    public enum StatusType {
        OFFER_ACK, // offer accepted by supervisor
        OK, // not decided yet
        NOT_OK, // not decided yet
        SEND_LOCATION, // if anyone asks, client sends its location
        DECLARE_NETWORK // network is assiigned to its supervisor
    };

    public StatusInfo(final StatusType status) {
        this.status = status;
    }
}