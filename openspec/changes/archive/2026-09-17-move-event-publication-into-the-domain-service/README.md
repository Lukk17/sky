# move-event-publication-into-the-domain-service

Move Kafka event publication out of the sky-booking and sky-offer controllers into the domain services that own the rules, and add the ArchUnit rule that stops an inbound adapter reaching a driven port again.
