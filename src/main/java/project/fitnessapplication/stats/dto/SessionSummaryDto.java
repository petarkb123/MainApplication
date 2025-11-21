package project.fitnessapplication.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SessionSummaryDto(
        UUID sessionId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        int totalSets,
        int totalReps,
        BigDecimal totalVolume
) { }
