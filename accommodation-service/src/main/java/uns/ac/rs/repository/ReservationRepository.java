package uns.ac.rs.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import uns.ac.rs.entity.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ReservationRepository implements PanacheRepository<Reservation> {

    @Override
    public List<Reservation> listAll() {
        return list("SELECT r FROM Reservation r WHERE NOT r.deleted");
    }

    @Override
    public Optional<Reservation> findByIdOptional(Long id) {
        return find("SELECT r FROM Reservation r WHERE NOT r.deleted AND r.id = ?1", id).singleResultOptional();
    }

    public List<Reservation> getByHost(String username) {
        return list("SELECT r FROM Reservation r JOIN r.accommodation a WHERE a.hostUsername = ?1 AND NOT r.deleted", username);
    }

    public List<Reservation> getByGuest(String username) {
        return list("SELECT r FROM Reservation r WHERE NOT r.deleted AND r.guestUsername = ?1", username);
    }

    public boolean exists(Long accommodationId, LocalDate date) {
        return list("SELECT r FROM Reservation r WHERE NOT r.deleted AND r.accommodation.id = ?1 AND " +
            "r.fromDate <= ?2 AND r.toDate >= ?2 AND (r.state = 'PENDING' OR r.state = 'APPROVED')", accommodationId, date).size() > 0;
    }

    @Transactional
    public void reject(Long reservationId) {
        update("status = ?1 where id = ?2", "REJECTED", reservationId);
    }

    public void deleteByHost(String username) {
        update("deleted = true where accommodation.hostUsername = ?1", username);
    }
}