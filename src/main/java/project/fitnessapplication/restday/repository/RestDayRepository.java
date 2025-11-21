package project.fitnessapplication.restday.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import project.fitnessapplication.restday.model.RestDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestDayRepository extends JpaRepository<RestDay, UUID> {
    List<RestDay> findByUserIdAndActiveTrueOrderByDateDesc(UUID userId);
    List<RestDay> findByUserIdAndDateBetweenAndActiveTrueOrderByDateDesc(UUID userId, LocalDate from, LocalDate to);
    Optional<RestDay> findByUserIdAndDateAndActiveTrue(UUID userId, LocalDate date);
    boolean existsByUserIdAndDateAndActiveTrue(UUID userId, LocalDate date);
    Optional<RestDay> findByIdAndUserIdAndActiveTrue(UUID id, UUID userId);
    void deleteByIdAndUserId(UUID id, UUID userId);

    @Query("select count(r) from RestDay r where r.userId = :userId and r.active = true and r.date between :from and :to")
    long countActiveRestDaysInRange(UUID userId, LocalDate from, LocalDate to);

    @Query("select r from RestDay r where r.userId = :userId and r.active = true and r.date between :from and :to order by r.date desc")
    List<RestDay> findActiveRestDaysInRange(UUID userId, LocalDate from, LocalDate to);
}
