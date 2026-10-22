package kz.nurbolat.bookaggregator.parser.core;

import jakarta.persistence.*;
import kz.nurbolat.bookaggregator.store.Store;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One pass of a store parser with its statistics.
 */
@Entity
@Table(name = "parser_runs")
@Getter
@Setter
@NoArgsConstructor
public class ParserRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private TriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status = RunStatus.RUNNING;

    @Column(name = "pages_ok", nullable = false)
    private int pagesOk;

    @Column(name = "pages_failed", nullable = false)
    private int pagesFailed;

    @Column(name = "offers_upserted", nullable = false)
    private int offersUpserted;

    @Column(name = "prices_changed", nullable = false)
    private int pricesChanged;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
