package project.fitnessapplication.stats.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplication.stats.dto.SessionSummaryDto;
import project.fitnessapplication.stats.dto.WeeklySummaryDto;
import project.fitnessapplication.workout.model.WorkoutSession;
import project.fitnessapplication.workout.model.WorkoutSet;
import project.fitnessapplication.workout.repository.WorkoutSessionRepository;
import project.fitnessapplication.workout.repository.WorkoutSetRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final WorkoutSessionRepository sessionRepo;
    private final WorkoutSetRepository setRepo;

    public StatsService(WorkoutSessionRepository sessionRepo, WorkoutSetRepository setRepo) {
        this.sessionRepo = sessionRepo;
        this.setRepo = setRepo;
    }

    public WeeklySummaryDto weekly(UUID userId, LocalDate from, LocalDate to) {
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.plusDays(1).atStartOfDay().minusNanos(1);

        List<WorkoutSession> sessions = sessionRepo
                .findByUserIdAndStartedAtBetweenOrderByStartedAtAsc(userId, fromTs, toTs);

        Map<UUID, WorkoutSession> byId = sessions.stream()
                .collect(Collectors.toMap(WorkoutSession::getId, s -> s));

        List<UUID> sids = sessions.stream().map(WorkoutSession::getId).toList();
        List<WorkoutSet> sets = sids.isEmpty() ? List.of() : setRepo.findAllBySessionIdIn(sids);

        Map<LocalDate, DayAcc> days = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) days.put(d, new DayAcc());

        for (WorkoutSession s : sessions) days.get(s.getStartedAt().toLocalDate()).sessions++;

        for (WorkoutSet ws : sets) {
            WorkoutSession s = byId.get(ws.getSessionId());
            if (s == null) continue;
            DayAcc acc = days.get(s.getStartedAt().toLocalDate());
            acc.sets++;
            if (ws.getReps() != null) acc.reps += ws.getReps();
            if (ws.getWeight() != null && ws.getReps() != null) {
                acc.volume = acc.volume.add(ws.getWeight().multiply(BigDecimal.valueOf(ws.getReps())));
            }
        }

        List<WeeklySummaryDto.DayStat> dayStats = days.entrySet().stream()
                .map(e -> new WeeklySummaryDto.DayStat(e.getKey(), e.getValue().sessions, e.getValue().sets, e.getValue().reps, e.getValue().volume))
                .toList();

        return new WeeklySummaryDto(from, to, dayStats);
    }

    public List<SessionSummaryDto> sessionSummaries(UUID userId, LocalDate from, LocalDate to) {
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.plusDays(1).atStartOfDay().minusNanos(1);

        List<WorkoutSession> sessions = sessionRepo
                .findByUserIdAndStartedAtBetweenOrderByStartedAtAsc(userId, fromTs, toTs);

        Map<UUID, List<WorkoutSet>> setsBySession = setRepo.findAllBySessionIdIn(
                        sessions.stream().map(WorkoutSession::getId).toList())
                .stream().collect(Collectors.groupingBy(WorkoutSet::getSessionId));

        List<SessionSummaryDto> out = new ArrayList<>();
        for (WorkoutSession s : sessions) {
            List<WorkoutSet> sets = setsBySession.getOrDefault(s.getId(), List.of());
            int totalSets = sets.size();
            int totalReps = sets.stream().filter(x -> x.getReps() != null).mapToInt(WorkoutSet::getReps).sum();
            BigDecimal volume = sets.stream()
                    .filter(x -> x.getWeight() != null && x.getReps() != null)
                    .map(x -> x.getWeight().multiply(BigDecimal.valueOf(x.getReps())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            out.add(new SessionSummaryDto(s.getId(), s.getStartedAt(), s.getFinishedAt(), totalSets, totalReps, volume));
        }
        return out;
    }

    private static class DayAcc {
        int sessions = 0;
        int sets = 0;
        int reps = 0;
        BigDecimal volume = BigDecimal.ZERO;
    }
}
