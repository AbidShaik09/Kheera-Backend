package com.knightdevelopers.kheerabackend.repository;

import com.knightdevelopers.kheerabackend.entity.User;
import com.knightdevelopers.kheerabackend.dto.UserResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("""
            select new com.knightdevelopers.kheerabackend.dto.UserResponse(u.id, u.name, u.email)
            from User u
            where u.isDeleted = false
            order by u.name asc, u.email asc
            """)
    List<UserResponse> findActiveUserSummaries();

    @Query("""
            select new com.knightdevelopers.kheerabackend.dto.UserResponse(u.id, u.name, u.email)
            from User u
            where u.email = :email and u.isDeleted = false
            """)
    Optional<UserResponse> findActiveUserSummaryByEmail(@Param("email") String email);

}
