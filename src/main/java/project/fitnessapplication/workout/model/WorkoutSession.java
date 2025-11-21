package project.fitnessapplication.workout.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="workout_sessions",
        indexes={
                @Index(name="ix_ws_user", columnList="user_id"),
                @Index(name="ix_ws_status", columnList="status")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true) @EqualsAndHashCode(of="id")
public class WorkoutSession {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", columnDefinition = "char(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_id", nullable = false, columnDefinition = "char(36)")
    private UUID userId;

    @Column(name="started_at",nullable=false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name="finished_at") private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;


}
