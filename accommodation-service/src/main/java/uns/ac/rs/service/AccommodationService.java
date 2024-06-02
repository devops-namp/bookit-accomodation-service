package uns.ac.rs.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uns.ac.rs.entity.Accommodation;
import uns.ac.rs.repository.AccommodationRepository;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AccommodationService {

    @Inject
    AccommodationRepository accommodationRepository;

    public List<Accommodation> getAll() {
        return accommodationRepository.listAll();
    }

    public Optional<Accommodation> getById(Long id) {
        return accommodationRepository.findByIdOptional(id);
    }

    public void addAccommodation(Accommodation accommodation) {
        accommodationRepository.persist(accommodation);
    }

    public void updateAccommodation(Long id, Accommodation updatedAccommodation) {
        accommodationRepository.findByIdOptional(id).ifPresent(existingAccommodation -> {
            existingAccommodation.setName(updatedAccommodation.getName());
            existingAccommodation.setLocation(updatedAccommodation.getLocation());
            existingAccommodation.setFilters(updatedAccommodation.getFilters());
            existingAccommodation.setMinGuests(updatedAccommodation.getMinGuests());
            existingAccommodation.setMaxGuests(updatedAccommodation.getMaxGuests());
            existingAccommodation.setPriceType(updatedAccommodation.getPriceType());
            existingAccommodation.setPriceAdjustments(updatedAccommodation.getPriceAdjustments());
            existingAccommodation.setImages(updatedAccommodation.getImages());
            accommodationRepository.persist(existingAccommodation);
        });
    }

    public void deleteAccommodation(Long id) {
        accommodationRepository.findByIdOptional(id).ifPresent(accommodationRepository::delete);
    }
}