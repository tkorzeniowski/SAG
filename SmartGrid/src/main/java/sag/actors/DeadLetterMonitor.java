package sag.actors;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Aktor kontrolujący wystąpienia tzw. martwych listów. Jeśli wiadomość nie zostanie dostarczona
 * do adresata, informacja o tym jest zapisywana w logach.
 */
public class DeadLetterMonitor extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    /**
     * Klasa konfigurująca określająca sposób tworzenia aktora klasy DeadLetterMonitor.
     * @return Obiekt konfiguracji aktora monitora.
     */
    static public Props props() {
        return Props.create(DeadLetterMonitor.class, DeadLetterMonitor::new);
    }

    private DeadLetterMonitor() {
        log.info("monitor martwych listów włączony");
    }

    private void receiveDeadLetter(DeadLetter deadLetter){
        log.info("Martwy list: " + deadLetter.message());
    }

    /**
     * Reaguje na przyjęcie wiadomości od innego aktora zgodnie z zadanymi wzorcami zachowań.
     * @return Sposób zachowania aktora w obliczu otrzymania wiadomości konkretnego rodzaju.
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
				.match(DeadLetter.class, this::receiveDeadLetter)
                .build();
    }
}
