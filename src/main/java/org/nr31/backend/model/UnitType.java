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
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "unit_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a participating military unit")
public class UnitType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the unit type", example = "1")
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Schema(description = "Localized name of the unit type", example = "{\"en\": \"Alpha Squad\", \"uk\": \"Загін Альфа\"}")
    private Map<String, String> name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Schema(description = "Localized description of the unit type", example = "{\"en\": \"Special operations squad\", \"uk\": \"Загін спеціального призначення\"}")
    private Map<String, String> description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_icon_id")
    @Schema(description = "Custom icon file associated with this unit type")
    private FileMetadata customIcon;
}
