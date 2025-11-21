package project.fitnessapplication.template.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name="template_items",
        indexes={
                @Index(name="ix_ti_tpl", columnList="template_id"),
                @Index(name="ix_ti_ex", columnList="exercise_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true) @EqualsAndHashCode(of="id")
public class TemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", columnDefinition = "char(36)")
    private UUID id;


    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "exercise_id", columnDefinition = "char(36)", nullable = false)
    private UUID exerciseId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "template_id", columnDefinition = "char(36)", nullable = false)
    private UUID templateId;

    @Column(name="target_sets") private Integer targetSets;

    @Column(name="position",nullable=false)
    @Builder.Default
    private Integer position = 1;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "group_id", columnDefinition = "char(36)")
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", length = 20)
    private project.fitnessapplication.workout.model.SetGroupType groupType;

    @Column(name = "group_order")
    private Integer groupOrder;
    
    @Column(name = "set_number")
    private Integer setNumber;
}
