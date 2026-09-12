package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.dto.SpaceListDto;
import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.entity.space.SpaceMembers;
import com.knightdevelopers.kheerabackend.entity.space.SpaceRoles;
import com.knightdevelopers.kheerabackend.entity.space.Spaces;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(statements = {
        "TRUNCATE TABLE space_members RESTART IDENTITY CASCADE",
        "TRUNCATE TABLE space_roles RESTART IDENTITY CASCADE",
        "TRUNCATE TABLE spaces RESTART IDENTITY CASCADE",
        "TRUNCATE TABLE users RESTART IDENTITY CASCADE"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SpaceMembersRepositoryIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private SpaceMembersRepository spaceMembersRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findActiveSpacesByUserEmailReturnsOnlyThatUsersActiveSpaces() {
        User owner = persist(new User("owner@example.com", "secret", "Owner"));
        User other = persist(new User("other@example.com", "secret", "Other"));
        Spaces alpha = persistSpaceWithOwner("Alpha", owner);
        Spaces beta = persistSpaceWithOwner("Beta", owner);
        persistSpaceWithOwner("Other Space", other);
        Spaces deletedMembershipSpace = persistSpaceWithOwner("Deleted Membership", owner);
        Spaces deletedSpace = persistSpaceWithOwner("Deleted Space", owner);
        deletedSpace.setDeleted(true);
        owner.getSpaceMembers().stream()
                .filter(member -> member.getSpace().getId().equals(deletedMembershipSpace.getId()))
                .findFirst()
                .orElseThrow()
                .setDeleted(true);
        entityManager.flush();
        entityManager.clear();

        List<SpaceListDto> spaces = spaceMembersRepository.findActiveSpacesByUserEmail("owner@example.com");

        assertThat(spaces)
                .extracting(SpaceListDto::getName)
                .containsExactly("Alpha", "Beta");
        assertThat(spaces)
                .extracting(SpaceListDto::getId)
                .containsExactly(alpha.getId(), beta.getId());
        assertThat(spaceMembersRepository.findActiveSpacesByUserEmail("missing@example.com")).isEmpty();
    }

    @Test
    void duplicateUserSpaceMembershipViolatesUniqueConstraint() {
        User owner = persist(new User("owner@example.com", "secret", "Owner"));
        Spaces space = persistSpaceWithOwner("Alpha", owner);
        SpaceRoles role = space.getRoles().getFirst();
        entityManager.flush();

        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("""
                            insert into space_members (user_id, space_id, space_role_id)
                            values (:userId, :spaceId, :roleId)
                            """)
                    .setParameter("userId", owner.getId())
                    .setParameter("spaceId", space.getId())
                    .setParameter("roleId", role.getId())
                    .executeUpdate();
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Spaces persistSpaceWithOwner(String name, User owner) {
        Spaces space = new Spaces();
        space.setSpaceName(name);
        SpaceRoles role = new SpaceRoles();
        role.setRoleName("Admin");
        space.addRole(role);
        space.addMember(owner, role);
        Spaces persistedSpace = persist(space);
        SpaceMembers member = persistedSpace.getSpaceMembers().getFirst();
        entityManager.persist(member);
        entityManager.flush();
        return persistedSpace;
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }
}
