package com.knightdevelopers.kheerabackend.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class WorkItemMigrationIntegrationTest extends PostgreSqlIntegrationTest {
    @Test void provisionsTypesWithoutRewritingHistoricalTasks() {
        String schema="task_migration_"+UUID.randomUUID().toString().replace("-", "");
        var ds=new DriverManagerDataSource(POSTGRES.getJdbcUrl()+(POSTGRES.getJdbcUrl().contains("?")?"&":"?")+"currentSchema="+schema,POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("26").load().migrate();
        var jdbc=new JdbcTemplate(ds);
        UUID space=jdbc.queryForObject("insert into spaces(space_name) values ('Legacy') returning id",UUID.class);
        UUID project=jdbc.queryForObject("insert into projects(project_name,space_id) values ('Empty types',?) returning id",UUID.class,space);
        UUID custom=jdbc.queryForObject("insert into projects(project_name,space_id) values ('Custom',?) returning id",UUID.class,space);
        jdbc.update("insert into work_item_types(project_id,name) values (?,'Bug')",custom);
        UUID item=jdbc.queryForObject("insert into work_items(title,project_id) values ('Legacy',?) returning id",UUID.class,project);
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();
        assertThat(jdbc.queryForList("select name from work_item_types where project_id=?",String.class,project)).containsExactly("Task");
        assertThat(jdbc.queryForList("select name from work_item_types where project_id=?",String.class,custom)).containsExactly("Bug");
        assertThat(jdbc.queryForObject("select work_item_type_id from work_items where id=?",UUID.class,item)).isNull();
        UUID fresh=jdbc.queryForObject("insert into projects(project_name,space_id) values ('New',?) returning id",UUID.class,space);
        assertThat(jdbc.queryForList("select name from work_item_types where project_id=?",String.class,fresh)).containsExactly("Task");
    }
}
