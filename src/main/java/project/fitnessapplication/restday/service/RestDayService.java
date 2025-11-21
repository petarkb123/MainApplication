package project.fitnessapplication.restday.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplication.restday.dto.CreateRestDayRequest;
import project.fitnessapplication.restday.dto.RestDayDto;
import project.fitnessapplication.restday.model.RestDay;
import project.fitnessapplication.restday.repository.RestDayRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RestDayService {

    private final RestDayRepository restDayRepository;

    public RestDayDto createRestDay(UUID userId, CreateRestDayRequest request) {
        if (restDayRepository.existsByUserIdAndDateAndActiveTrue(userId, request.date())) {
            throw new IllegalArgumentException("A rest day already exists for this date.");
        }

        RestDay restDay = RestDay.builder()
                .userId(userId)
                .date(request.date())
                .reason(request.reason())
                .notes(request.notes())
                .active(true)
                .build();
        return RestDayDto.fromEntity(restDayRepository.save(restDay));
    }

    @Transactional(readOnly = true)
    public List<RestDayDto> getRestDaysInRange(UUID userId, LocalDate from, LocalDate to) {
        return restDayRepository.findByUserIdAndDateBetweenAndActiveTrueOrderByDateDesc(userId, from, to)
                .stream()
                .map(RestDayDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RestDayDto> getRestDaysByUser(UUID userId) {
        return restDayRepository.findByUserIdAndActiveTrueOrderByDateDesc(userId)
                .stream()
                .map(RestDayDto::fromEntity)
                .collect(Collectors.toList());
    }

    public RestDayDto updateRestDay(UUID userId, UUID restDayId, CreateRestDayRequest request) {
        RestDay existingRestDay = restDayRepository.findByIdAndUserIdAndActiveTrue(restDayId, userId)
                .orElseThrow(() -> new NoSuchElementException("Rest day not found or not active."));

        if (!existingRestDay.getDate().equals(request.date()) &&
                restDayRepository.existsByUserIdAndDateAndActiveTrue(userId, request.date())) {
            throw new IllegalArgumentException("A rest day already exists for the new date.");
        }

        existingRestDay.setDate(request.date());
        existingRestDay.setReason(request.reason());
        existingRestDay.setNotes(request.notes());
        return RestDayDto.fromEntity(restDayRepository.save(existingRestDay));
    }

    public void deleteRestDay(UUID userId, UUID restDayId) {
        RestDay existingRestDay = restDayRepository.findByIdAndUserIdAndActiveTrue(restDayId, userId)
                .orElseThrow(() -> new NoSuchElementException("Rest day not found or not active."));
        restDayRepository.delete(existingRestDay);
    }

    @Transactional(readOnly = true)
    public boolean isRestDay(UUID userId, LocalDate date) {
        return restDayRepository.existsByUserIdAndDateAndActiveTrue(userId, date);
    }

    @Transactional(readOnly = true)
    public long countRestDaysInRange(UUID userId, LocalDate from, LocalDate to) {
        return restDayRepository.countActiveRestDaysInRange(userId, from, to);
    }
}
