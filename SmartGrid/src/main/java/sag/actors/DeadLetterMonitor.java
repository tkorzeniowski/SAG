package sag.actors;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/*
* Jeśli pojawi się martwy list, odpowiedz wysyłającemu, żeby ponowił wiadomość (funkcjonalność nieaktywna)*/
public class DeadLetterMonitor extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static public Props props() {

        return Props.create(DeadLetterMonitor.class, DeadLetterMonitor::new);
    }

    private DeadLetterMonitor() {
        log.info("monitor martwych listów włączony");
    }

    private void receiveDeadLetter(DeadLetter deadLetter){
        log.info("Martwy list: " + deadLetter.message());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
				.match(DeadLetter.class, this::receiveDeadLetter)
                .build();
    }
}
