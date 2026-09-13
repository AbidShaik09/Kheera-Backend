package com.knightdevelopers.kheerabackend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knightdevelopers.kheerabackend.dto.*;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.service.*;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Sql(statements = "TRUNCATE users, spaces CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MembershipIntegrationTest extends PostgreSqlIntegrationTest {
    @Autowired MembershipService service;
    @Autowired SpaceLifecycleService spaces;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired javax.sql.DataSource dataSource;
    UUID space, admin, reader, owner;
    @BeforeEach void setup() throws Exception {
        users.saveAndFlush(new User("owner@example.com", "hash", "Same"));
        users.saveAndFlush(new User("second@example.com", "hash", "Same"));
        users.saveAndFlush(new User("third@example.com", "hash", "Other"));
        space = spaces.create("owner@example.com", mapper.readValue("{\"name\":\"Team\"}", SpaceWriteRequest.class)).id();
        admin = role("Administrator"); reader = role("Member");
        owner = jdbc.queryForObject("select id from space_members where space_id=?", UUID.class, space);
    }
    UUID role(String name) { return jdbc.queryForObject("select id from space_roles where space_id=? and role_name=?", UUID.class, space, name); }
    MembershipAddRequest addRequest(String email, UUID role) throws Exception {
        return mapper.readValue("{\"email\":\"" + email + "\",\"roleId\":\"" + role + "\"}", MembershipAddRequest.class);
    }
    MembershipRoleRequest roleRequest(UUID role) throws Exception {
        return mapper.readValue("{\"roleId\":\"" + role + "\"}", MembershipRoleRequest.class);
    }
    void status(Runnable action, int status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(SpaceApiException.class, e -> assertThat(e.status().value()).isEqualTo(status));
    }
    @Test void duplicateAndRejoin() throws Exception {
        var input = addRequest("second@example.com", reader);
        var first = service.add("owner@example.com", space, input);
        status(() -> service.add("owner@example.com", space, input), 409);
        service.remove("owner@example.com", space, first.id());
        var restored = service.add("owner@example.com", space, input);
        assertThat(restored.id()).isEqualTo(first.id());
        assertThat(jdbc.queryForObject("select count(*) from space_members where space_id=?", Integer.class, space)).isEqualTo(2);
    }
    @Test void lastAdministrator() throws Exception {
        var demote = roleRequest(reader);
        status(() -> service.remove("owner@example.com", space, owner), 409);
        status(() -> service.changeRole("owner@example.com", space, owner, demote), 409);
        service.add("owner@example.com", space, addRequest("second@example.com", admin));
        service.changeRole("owner@example.com", space, owner, demote);
        status(() -> service.remove("owner@example.com", space, owner), 403);
        assertThat(service.list("owner@example.com", space, 0, 25, "name,asc", "").totalItems()).isEqualTo(2);
    }
    @Test void isolationAndDeletedRows() throws Exception {
        var request = addRequest("third@example.com", reader);
        status(() -> service.add("second@example.com", space, request), 404);
        status(() -> service.list("second@example.com", space, 0, 25, "name,asc", ""), 404);
        UUID foreign = spaces.create("second@example.com", mapper.readValue("{\"name\":\"Foreign\"}", SpaceWriteRequest.class)).id();
        UUID foreignRole = jdbc.queryForObject("select id from space_roles where space_id=? and role_name='Administrator'", UUID.class, foreign);
        var crossRole = addRequest("third@example.com", foreignRole);
        status(() -> service.add("owner@example.com", space, crossRole), 404);
        UUID foreignMember = jdbc.queryForObject("select id from space_members where space_id=?", UUID.class, foreign);
        status(() -> service.remove("owner@example.com", space, foreignMember), 404);
        jdbc.update("update users set is_deleted=true where email='third@example.com'");
        status(() -> service.add("owner@example.com", space, request), 404);
        var unknown = addRequest("unknown@example.com", reader);
        status(() -> service.add("owner@example.com", space, unknown), 404);
        jdbc.update("update users set is_deleted=true where email='owner@example.com'");
        status(() -> service.list("owner@example.com", space, 0, 25, "name,asc", ""), 401);
    }
    @Test void pagination() throws Exception {
        service.add("owner@example.com", space, addRequest("second@example.com", reader));
        service.add("owner@example.com", space, addRequest("third@example.com", reader));
        var first = service.list("owner@example.com", space, 0, 1, "name,asc", "Same");
        var second = service.list("owner@example.com", space, 1, 1, "name,asc", "Same");
        assertThat(first.totalItems()).isEqualTo(2); assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.items().getFirst().id()).isNotEqualTo(second.items().getFirst().id());
        assertThat(service.list("owner@example.com", space, 0, 1, "name,asc", "Same")).isEqualTo(first);
        assertThat(service.list("owner@example.com", space, 0, 25, "name,asc", "%").items()).isEmpty();
        jdbc.update("update users set is_deleted=true where email='second@example.com'");
        assertThat(service.list("owner@example.com", space, 0, 25, "name,asc", "Same").totalItems()).isEqualTo(1);
        jdbc.update("update space_roles set is_deleted=true where id=?", reader);
        assertThat(service.list("owner@example.com", space, 0, 25, "name,asc", "").totalItems()).isEqualTo(1);
    }
    @Test void grantSubset() throws Exception {
        var member = service.add("owner@example.com", space, addRequest("second@example.com", reader));
        jdbc.update("insert into space_role_permissions(space_role_id,space_permission_id) select ?,id from space_permissions where space_id=? and permission_name in ('space.members.add','space.members.change-role','space.members.remove')", reader, space);
        var promote = roleRequest(admin); var addAdmin = addRequest("third@example.com", admin);
        status(() -> service.changeRole("second@example.com", space, member.id(), promote), 403);
        status(() -> service.add("second@example.com", space, addAdmin), 403);
        status(() -> service.remove("second@example.com", space, owner), 403);
        service.remove("second@example.com", space, member.id());
        status(() -> service.list("second@example.com", space, 0, 25, "name,asc", ""), 404);
    }
    @Test void catalogue() {
        assertThat(service.permissions("owner@example.com", space, 0, 100).totalItems()).isEqualTo(7);
        assertThat(service.roles("owner@example.com", space, 0, 100).totalItems()).isEqualTo(2);
        jdbc.update("update space_permissions set is_deleted=true where permission_name='space.members.read'");
        status(() -> service.roles("owner@example.com", space, 0, 100), 403);
    }
    @Test void invalidBoundsAndSorts() {
        for (int size : new int[]{0, -1, 101}) status(() -> service.list("owner@example.com", space, 0, size, "name,asc", ""), 400);
        for (int page : new int[]{-1, 100001}) status(() -> service.list("owner@example.com", space, page, 25, "name,asc", ""), 400);
        for (String sort : List.of("password,asc", "name,sideways", "name", "name,asc,id", "user.email,asc"))
            status(() -> service.list("owner@example.com", space, 0, 25, sort, ""), 400);
        status(() -> service.list("owner@example.com", space, 0, 25, "name,asc", "a".repeat(101)), 400);
    }
    @Test void boundedQueryCount() throws Exception {
        service.add("owner@example.com", space, addRequest("second@example.com", reader));
        service.add("owner@example.com", space, addRequest("third@example.com", reader));
        var statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        try {
            statistics.clear(); service.list("owner@example.com", space, 0, 1, "name,asc", "");
            long one = statistics.getPrepareStatementCount();
            statistics.clear(); service.list("owner@example.com", space, 0, 2, "name,asc", "");
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(one).isLessThanOrEqualTo(6);
        } finally { statistics.setStatisticsEnabled(false); }
    }
    @Test void migrationPreservesExistingAuthorityAndRevokedGrants() throws Exception {
        String schema = "membership_migration_" + UUID.randomUUID().toString().replace("-", "");
        org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("24").load().migrate();
        try (var connection = dataSource.getConnection()) {
            connection.setSchema(schema);
            var isolated = new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true));
            UUID s = UUID.randomUUID(), r = UUID.randomUUID(), p = UUID.randomUUID(), revoked = UUID.randomUUID();
            isolated.update("insert into spaces(id,space_name) values (?, 'Legacy')", s);
            isolated.update("insert into space_roles(id,space_id,role_name) values (?,?,'Custom manager')", r, s);
            isolated.update("insert into space_permissions(id,space_id,permission_name) values (?,?,'space.members.manage')", p, s);
            isolated.update("insert into space_role_permissions(space_role_id,space_permission_id) values (?,?)", r, p);
            isolated.update("insert into space_permissions(id,space_id,permission_name) values (?,?,'space.members.remove')", revoked, s);
            isolated.update("insert into space_role_permissions(space_role_id,space_permission_id,is_deleted) values (?,?,true)", r, revoked);
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
            assertThat(isolated.queryForObject("select count(*) from space_permissions", Integer.class)).isEqualTo(5);
            assertThat(isolated.queryForObject("select count(*) from space_role_permissions where is_deleted=false", Integer.class)).isEqualTo(4);
            assertThat(isolated.queryForObject("select is_deleted from space_role_permissions where space_permission_id=?", Boolean.class, revoked)).isTrue();
            connection.setSchema("public");
        } finally { jdbc.execute("drop schema " + schema + " cascade"); }
    }
    @Test void revocation() throws Exception {
        var member = service.add("owner@example.com", space, addRequest("second@example.com", reader));
        jdbc.update("insert into projects(project_name,space_id) values ('Retained',?)", space);
        UUID project = jdbc.queryForObject("select id from projects where space_id=?", UUID.class, space);
        jdbc.update("insert into work_items(title,project_id,assigned_to_id) values ('Historical task',?,?)", project, member.id());
        UUID task = jdbc.queryForObject("select id from work_items where project_id=?", UUID.class, project);
        jdbc.update("insert into work_item_comments(description,work_item_id,space_member_id) values ('Historical comment',?,?)", task, member.id());
        service.remove("owner@example.com", space, member.id());
        status(() -> spaces.detail("second@example.com", space), 404);
        assertThat(jdbc.queryForObject("select count(*) from projects where space_id=?", Integer.class, space)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select assigned_to_id from work_items where id=?", UUID.class, task)).isEqualTo(member.id());
        assertThat(jdbc.queryForObject("select count(*) from work_item_comments where space_member_id=? and is_deleted=false", Integer.class, member.id())).isEqualTo(1);
        spaces.delete("owner@example.com", space);
        status(() -> service.roles("owner@example.com", space, 0, 25), 404);
    }
    List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
            Callable<Integer> a = () -> { ready.countDown(); start.await(10, TimeUnit.SECONDS); return first.call(); };
            Callable<Integer> b = () -> { ready.countDown(); start.await(10, TimeUnit.SECONDS); return second.call(); };
            var fa = pool.submit(a); var fb = pool.submit(b);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(fa.get(20, TimeUnit.SECONDS), fb.get(20, TimeUnit.SECONDS));
        }
    }
    int outcome(Runnable action) { try { action.run(); return 200; } catch (SpaceApiException e) { return e.status().value(); } }
    @Test void concurrentAdds() throws Exception {
        var request = addRequest("second@example.com", reader);
        Callable<Integer> add = () -> outcome(() -> service.add("owner@example.com", space, request));
        assertThat(race(add, add)).containsExactlyInAnyOrder(200, 409);
        UUID id = jdbc.queryForObject("select sm.id from space_members sm join users u on u.id=sm.user_id where u.email='second@example.com'", UUID.class);
        service.remove("owner@example.com", space, id);
        assertThat(race(add, add)).containsExactlyInAnyOrder(200, 409);
    }
    @Test void concurrentDemotionsAndRemovals() throws Exception {
        var second = service.add("owner@example.com", space, addRequest("second@example.com", admin));
        var demote = roleRequest(reader);
        assertThat(race(() -> outcome(() -> service.changeRole("owner@example.com", space, owner, demote)),
                () -> outcome(() -> service.remove("second@example.com", space, second.id()))))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(jdbc.queryForObject("select count(*) from space_members where space_id=? and space_role_id=? and is_deleted=false", Integer.class, space, admin)).isEqualTo(1);
    }
}
