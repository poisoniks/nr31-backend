package org.nr31.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.nr31.backend.hibernate.Jackson3JsonNodeType;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

@Entity
@Table(name = "page_revisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @Type(Jackson3JsonNodeType.class)
    @Column(name = "layout_data", nullable = false, columnDefinition = "jsonb")
    private JsonNode layoutData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RevisionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
