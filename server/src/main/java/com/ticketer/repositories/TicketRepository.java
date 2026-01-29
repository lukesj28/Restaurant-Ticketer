package com.ticketer.repositories;

import com.ticketer.models.Ticket;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(int id);

    List<Ticket> findAllActive();

    List<Ticket> findAllCompleted();

    List<Ticket> findAllClosed();

    boolean deleteById(int id);

    void deleteAll();

    void persistClosedTickets();

    void moveToCompleted(int id);

    void moveToClosed(int id);

    void moveToActive(int ticketId);

}
