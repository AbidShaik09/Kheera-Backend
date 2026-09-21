package com.knightdevelopers.kheerabackend.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class WorkflowMigrationIntegrationTest extends PostgreSqlIntegrationTest {
    @Test void upgradesLegacyProjectsStagesAndItemsWithoutLosingRecords() {
        String schema = "board_migration_" + UUID.randomUUID().toString().replace("-", "");
        String separator = POSTGRES.getJdbcUrl().contains("?") ? "&" : "?";
        var ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl()+separator+"currentSchema="+schema, POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("25").load().migrate();
        var jdbc = new JdbcTemplate(ds);
        UUID space=jdbc.queryForObject("insert into spaces(space_name) values ('Legacy') returning id",UUID.class);
        UUID custom=jdbc.queryForObject("insert into projects(project_name,space_id) values ('Custom',?) returning id",UUID.class,space);
        UUID empty=jdbc.queryForObject("insert into projects(project_name,space_id) values ('Empty',?) returning id",UUID.class,space);
        UUID deleted=jdbc.queryForObject("insert into projects(project_name,space_id) values ('Deleted stages',?) returning id",UUID.class,space);
        UUID preserved=jdbc.queryForObject("insert into project_workflows(project_id,workflow_name) values (?,NULL) returning id",UUID.class,custom);
        jdbc.update("insert into project_workflows(project_id,workflow_name,is_deleted) values (?,'Gone',true)",deleted);
        jdbc.update("insert into work_items(project_id,title) values (?,'One'),(?,'Two'),(?,'Three'),(?,'Four')",custom,custom,empty,deleted);
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();
        assertThat(jdbc.queryForObject("select count(*) from work_items",Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select count(*) from project_workflows where project_id=? and not is_deleted",Integer.class,custom)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select workflow_name from project_workflows where id=?",String.class,preserved)).isEqualTo("Unnamed stage");
        assertThat(jdbc.queryForObject("select is_complete from project_workflows where id=?",Boolean.class,preserved)).isFalse();
        assertThat(jdbc.queryForList("select position from work_items where project_id=? order by position",Integer.class,custom)).containsExactly(0,1);
        assertThat(jdbc.queryForObject("select count(*) from project_workflows where project_id=? and not is_deleted",Integer.class,deleted)).isEqualTo(6);
        assertThat(jdbc.queryForObject("select count(*) from work_items w join project_workflows s on s.id=w.workflow_id and s.project_id=w.project_id where not s.is_deleted",Integer.class)).isEqualTo(4);
    }
}
