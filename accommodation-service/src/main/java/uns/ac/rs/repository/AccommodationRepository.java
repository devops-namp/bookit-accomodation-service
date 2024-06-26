package uns.ac.rs.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import uns.ac.rs.controlller.dto.AccommodationDto;
import uns.ac.rs.controlller.dto.AccommodationWithPrice;
import uns.ac.rs.controlller.dto.DateInfoDto;
import uns.ac.rs.entity.Accommodation;
import uns.ac.rs.entity.PriceAdjustmentDate;
import uns.ac.rs.entity.Reservation;
import uns.ac.rs.entity.events.AutoApproveEvent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class AccommodationRepository implements PanacheRepository<Accommodation> {

    @Override
    public List<Accommodation> listAll() {
        return list("SELECT a FROM Accommodation a WHERE NOT a.deleted");
    }

    @Override
    public Optional<Accommodation> findByIdOptional(Long id) {
        return find("SELECT a FROM Accommodation a WHERE NOT a.deleted AND a.id = ?1", id).singleResultOptional();
    }

    public List<AccommodationWithPrice> search(String name, String location, List<String> filters, Integer numGuests,
                                               LocalDate fromDate, LocalDate toDate, Double fromPrice, Double toPrice, String priceType) {
        StringBuilder query = new StringBuilder("SELECT a FROM Accommodation a " +
                "LEFT JOIN a.priceAdjustments pa " +
                "LEFT JOIN pa.priceAdjustmentDate pad " +
                "LEFT JOIN a.reservations r " +
                "WHERE NOT a.deleted");

        if (name != null) {
            query.append(" AND a.name LIKE CONCAT('%', :name, '%')");
        }
        if (location != null) {
            query.append(" AND a.location LIKE CONCAT('%', :location, '%')");
        }
        if (numGuests != null) {
            query.append(" AND a.maxGuests >= :numGuests AND a.minGuests <= :numGuests ");
        }
        if (priceType != null) {
            query.append(" AND a.priceType = :priceType");
        }
        if (filters != null && !filters.isEmpty()) {
            for (int i = 0; i < filters.size(); i++) {
                query.append(" AND a.filters LIKE CONCAT('%', :filter").append(i).append(", '%')");
            }
        }

        if (fromDate != null && toDate != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
            query.append(" AND NOT EXISTS (SELECT 1 FROM PriceAdjustment pa2 WHERE pa2.accommodation.id = a.id AND pa2.priceAdjustmentDate.date BETWEEN :fromDate AND :toDate AND pa2.priceAdjustmentDate.reservation.id IS NOT NULL)");
            query.append(" AND (SELECT COUNT(pa3) FROM PriceAdjustment pa3 WHERE pa3.accommodation.id = a.id AND pa3.priceAdjustmentDate.date BETWEEN :fromDate AND :toDate)=").append(daysBetween);
        }

        query.append(" GROUP BY a.id");

        var queryBuilder = getEntityManager().createQuery(query.toString(), Object[].class);

        if (name != null) {
            queryBuilder.setParameter("name", name);
        }
        if (location != null) {
            queryBuilder.setParameter("location", location);
        }
        if (numGuests != null) {
            queryBuilder.setParameter("numGuests", numGuests);
        }
        if (priceType != null) {
            queryBuilder.setParameter("priceType", priceType);
        }
        if (filters != null && !filters.isEmpty()) {
            for (int i = 0; i < filters.size(); i++) {
                queryBuilder.setParameter("filter" + i, filters.get(i));
            }
        }
        if (fromDate != null) {
            queryBuilder.setParameter("fromDate", fromDate);
        }
        if (toDate != null) {
            queryBuilder.setParameter("toDate", toDate);
        }

        List<Object[]> results = queryBuilder.getResultList();
        List<AccommodationWithPrice> accommodationsWithPrices = new ArrayList<>();
        for (Object[] result : results) {
            Accommodation accommodation = (Accommodation) result[0];
            double totalPrice = 0.0;

            if (fromDate != null && toDate != null) {
                List<PriceAdjustmentDate> priceAdjustments = getEntityManager().createQuery(
                        "SELECT pad FROM PriceAdjustmentDate pad WHERE pad.date BETWEEN :fromDate AND :toDate AND pad.priceAdjustment.accommodation.id = :accommodationId",
                        PriceAdjustmentDate.class)
                        .setParameter("fromDate", fromDate)
                        .setParameter("toDate", toDate)
                        .setParameter("accommodationId", accommodation.getId())
                        .getResultList();

                for (PriceAdjustmentDate pad : priceAdjustments) {
                    totalPrice += pad.getPrice();
                }
            }
            if ((fromPrice == null || totalPrice >= fromPrice) && (toPrice == null || totalPrice <= toPrice)) {
                accommodationsWithPrices.add(new AccommodationWithPrice(new AccommodationDto(accommodation), totalPrice));
            }
        }

        return accommodationsWithPrices;
    }


    public List<DateInfoDto> getMonthInformation(Accommodation accommodation, Integer month, Integer year) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        List<PriceAdjustmentDate> result = getEntityManager().createQuery(
                "SELECT pad FROM PriceAdjustmentDate pad WHERE pad.date BETWEEN :startOfMonth AND :endOfMonth AND pad.priceAdjustment.accommodation.id = :accommodationId",
                PriceAdjustmentDate.class)
                .setParameter("startOfMonth", startOfMonth)
                .setParameter("endOfMonth", endOfMonth)
                .setParameter("accommodationId", accommodation.getId())
                .getResultList();


        return result.stream()
                .map(pad -> new DateInfoDto(
                        pad.getDate().toString(),
                        pad.getPrice(),
                        pad.getReservation() == null ? "Available" : "Reserved"
                ))
                .collect(Collectors.toList());
    }

    public void deleteByHost(String username) {
        update("deleted = true where hostUsername = ?1", username);
    }
    @Transactional
    public void changeAutoapproveInAccommodations(AutoApproveEvent event) {
        update("autoApprove = ?1 where hostUsername = ?2",
                event.isAutoapprove(), event.getUsername());
    }

    @Transactional
    public List<Accommodation> findByHostUsername(String hostUsername) {
        return list("hostUsername", hostUsername);
    }
}
