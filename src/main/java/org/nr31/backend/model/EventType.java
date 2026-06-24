package org.nr31.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Cacheable;

import java.util.Map;

@Entity
@Table(name = "event_types")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a type of calendar event")
public class EventType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the event type", example = "1")
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Schema(description = "Localized name of the event type", example = "{\"en\": \"Training\", \"uk\": \"Тренування\"}")
    private Map<String, String> name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_icon_id")
    @Schema(description = "Custom icon file associated with this event type")
    private FileMetadata customIcon;

    @Builder.Default
    @Column(name = "attendance_weight", nullable = false)
    @Schema(description = "Weight of the event type for attendance calculation", example = "1")
    private int attendanceWeight = 1;
}
