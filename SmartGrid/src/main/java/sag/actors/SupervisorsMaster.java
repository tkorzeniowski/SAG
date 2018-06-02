package sag.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import sag.messages.StatusInfo;
import sag.model.SupervisorState;

import java.util.ArrayList;
import java.util.List;

/*
* Nazdorca wszystkich nadzorców. Jest odpowiedzialny na przechowywanie stanu pozostałych nadzorców.
* Gdy otrzyma wiadomość dotyczącą zapisania stanu jednocześnie odsyła otrzymany stan (prośba o zapis
* stanu pojawia się tylko gdy podrzędny nadzorca wymaga restartu).*/
public class SupervisorsMaster extends AbstractActor {

    private List<SupervisorState> states = new ArrayList<>();
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static public Props props() {

        return Props.create(SupervisorsMaster.class, SupervisorsMaster::new);
    }

    private SupervisorsMaster() {
        log.info("MASTER ŻYJE!");
    }

    private void saveState(SupervisorState state){
        states.add(state);
        log.info("ZAPISALEM STAN " + this.sender());
        this.sender().tell(state, getSelf());
    }

    private void receiveStatus(StatusInfo msg){
        if(msg.status == StatusInfo.StatusType.GET_SUPERVISOR_MASTER){
            this.sender().tell("masterCall", getSelf());
        }
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SupervisorState.class, this::saveState)
                .match(StatusInfo.class, this::receiveStatus)
                .build();
    }
}
