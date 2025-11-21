package project.fitnessapplication.template.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="workout_templates",
        indexes=@Index(name="ix_tpl_owner", columnList="owner_user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true) @EqualsAndHashCode(of="id")
public class WorkoutTemplate {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", columnDefinition = "char(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "owner_user_id", nullable = false, columnDefinition = "char(36)")
    private UUID ownerUserId;

    @Column(nullable=false,length=120) private String name;

    @Column(name="created_on",nullable=false)
    @Builder.Default
    private LocalDateTime createdOn = LocalDateTime.now();


}
