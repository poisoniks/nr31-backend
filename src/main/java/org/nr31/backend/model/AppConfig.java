package org.nr31.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.nr31.backend.hibernate.Jackson3JsonNodeType;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Entity
@Table(name = "app_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {

    @Id
    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description", columnDefinition = "jsonb")
    private Map<String, String> description;

    @Type(Jackson3JsonNodeType.class)
    @Column(name = "config_value", nullable = false, columnDefinition = "jsonb")
    private JsonNode configValue;

    @Type(Jackson3JsonNodeType.class)
    @Column(name = "config_schema", columnDefinition = "jsonb")
    private JsonNode configSchema;
}
