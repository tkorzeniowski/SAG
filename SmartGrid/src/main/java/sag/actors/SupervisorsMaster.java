package sag.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.apache.log4j.Logger;
import sag.messages.StatusInfo;
import sag.model.SupervisorState;

/**
 * Nazdorca wszystkich nadzorców. Jest odpowiedzialny na przechowywanie stanów pozostałych nadzorców.
 * Gdy otrzyma wiadomość przechowującą stan jednocześnie odsyła otrzymany stan (prośba o zapis
 * stanu pojawia się tylko gdy podrzędny nadzorca wymaga restartu).
 */
public class SupervisorsMaster extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    //private org.apache.log4j.Logger log = Logger.getLogger(Supervisor.class);

    static public Props props() {
        return Props.create(SupervisorsMaster.class, SupervisorsMaster::new);
    }

    private SupervisorsMaster() {
        log.info("MASTER ŻYJE!");
    }

    /*
     * Zapisuje stan wysłany przez podnadzorcę.
     */
    private void saveState(SupervisorState state) {
        log.info("OTRZYMAŁEM STAN " + this.sender());
        this.sender().tell(state, getSelf());
    }

    /*
     * Odpowiada na wiadomość podnadzorcy, żeby ten mógł zapamiętać,
     * kto jest jego nadzorcą.
     */
    private void receiveStatus(StatusInfo msg) {
        if (msg.status == StatusInfo.StatusType.GET_SUPERVISOR_MASTER) {
            this.sender().tell("masterCall", getSelf());
        }
    }

    /**
     * Reaguje na przyjęcie wiadomości od innego aktora zgodnie z zadanymi wzorcami zachowań.
     * Super nadzorca jest przygotowany na otrzymywanie stanów swoich podnadzorców,
     * w celu ich odesłania w przypadku wystąpienia awarii.
     * @return Sposób zachowania aktora w obliczu otrzymania wiadomości konkretnego rodzaju.
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SupervisorState.class, this::saveState)
                .match(StatusInfo.class, this::receiveStatus)
                .build();
    }
}
