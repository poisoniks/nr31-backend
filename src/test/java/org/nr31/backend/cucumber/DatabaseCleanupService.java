package org.nr31.backend.cucumber;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DatabaseCleanupService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private boolean snapshotCreated = false;

    public void createSnapshot() {
        if (snapshotCreated) {
            return;
        }

        List<String> tableNames = discoverTableNames();
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS _snapshot");

        // Create a table to track modifications
        jdbcTemplate.execute("DROP TABLE IF EXISTS _snapshot._modified_tables");
        jdbcTemplate.execute("CREATE TABLE _snapshot._modified_tables (table_name text PRIMARY KEY)");

        // Create a generic trigger function
        jdbcTemplate.execute(
                "CREATE OR REPLACE FUNCTION _snapshot.track_modification() RETURNS trigger AS $$ " +
                        "BEGIN " +
                        "  INSERT INTO _snapshot._modified_tables (table_name) VALUES (TG_TABLE_NAME) ON CONFLICT DO NOTHING; " +
                        "  RETURN NULL; " +
                        "END; $$ LANGUAGE plpgsql"
        );

        for (String table : tableNames) {
            String quotedTable = quote(table);
            jdbcTemplate.execute("DROP TABLE IF EXISTS _snapshot." + quotedTable);
            jdbcTemplate.execute("CREATE TABLE _snapshot." + quotedTable + " AS TABLE public." + quotedTable);

            // Attach the trigger to track changes
            jdbcTemplate.execute(
                    "CREATE TRIGGER track_mod_trig AFTER INSERT OR UPDATE OR DELETE ON public." + quotedTable +
                            " FOR EACH STATEMENT EXECUTE FUNCTION _snapshot.track_modification()"
            );
        }

        // Save sequence values
        jdbcTemplate.execute("DROP TABLE IF EXISTS _snapshot._sequence_values");
        jdbcTemplate.execute("CREATE TABLE _snapshot._sequence_values (seq_name text PRIMARY KEY, seq_value bigint)");
        jdbcTemplate.execute(
                "INSERT INTO _snapshot._sequence_values (seq_name, seq_value) " +
                        "SELECT sequencename, last_value FROM pg_sequences WHERE schemaname = 'public'"
        );

        snapshotCreated = true;
    }

    public void resetDatabase() {
        if (!snapshotCreated) {
            throw new IllegalStateException("Snapshot not created. Call createSnapshot() first.");
        }

        List<String> modifiedTables = jdbcTemplate.queryForList(
                "SELECT table_name FROM _snapshot._modified_tables", String.class);

        // If the scenario was read-only, do nothing!
        if (modifiedTables.isEmpty()) {
            return;
        }

        // Use a single PL/pgSQL block to eliminate network roundtrips between Java and Postgres
        StringBuilder sql = new StringBuilder("DO $$ BEGIN ");

        for (String table : modifiedTables) {
            String quotedTable = quote(table);
            String columnsList = getNonGeneratedColumns(table);
            // Use DELETE instead of TRUNCATE to avoid implicit CASCADE wiping out unmodified dependent tables
            sql.append("DELETE FROM public.").append(quotedTable).append("; ");
            sql.append("INSERT INTO public.").append(quotedTable).append(" (").append(columnsList).append(") ")
                    .append(" SELECT ").append(columnsList).append(" FROM _snapshot.").append(quotedTable).append("; ");
        }

        // Clear the tracking table
        sql.append("DELETE FROM _snapshot._modified_tables; ");
        sql.append("END $$;");

        jdbcTemplate.execute("SET session_replication_role = 'replica'");
        try {
            jdbcTemplate.execute(sql.toString());

            // Restore all sequences in a single batched query
            jdbcTemplate.execute(
                    "DO $$ DECLARE r RECORD; BEGIN " +
                            "FOR r IN SELECT seq_name, seq_value FROM _snapshot._sequence_values " +
                            "WHERE seq_value IS NOT NULL LOOP " +
                            "EXECUTE format('SELECT setval(%L, %s)', r.seq_name, r.seq_value); " +
                            "END LOOP; END $$"
            );
        } finally {
            jdbcTemplate.execute("SET session_replication_role = 'origin'");
        }
    }

    private String getNonGeneratedColumns(String tableName) {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = ? " +
                "AND is_generated = 'NEVER' " +
                "ORDER BY ordinal_position", String.class, tableName);
        if (columns.isEmpty()) {
            return "*";
        }
        return String.join(", ", columns.stream().map(this::quote).toList());
    }

    private List<String> discoverTableNames() {
        return jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables " +
                        "WHERE schemaname = 'public' AND tablename != 'flyway_schema_history' " +
                        "ORDER BY tablename", String.class);
    }

    private String quote(String identifier) {
        return "\"" + identifier + "\"";
    }
}