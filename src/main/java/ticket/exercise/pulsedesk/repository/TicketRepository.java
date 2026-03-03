package ticket.exercise.pulsedesk.repository;

import ticket.exercise.pulsedesk.model.entity.Ticket;
import ticket.exercise.pulsedesk.model.enums.Category;
import ticket.exercise.pulsedesk.model.enums.Priority;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket> findByCategory(Category category);

    List<Ticket> findByPriority(Priority priority);
}