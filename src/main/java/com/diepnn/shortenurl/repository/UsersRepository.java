package com.diepnn.shortenurl.repository;

import com.diepnn.shortenurl.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);
    Optional<Users> findByEmail(String email);

    @Query("SELECT u FROM Users u LEFT JOIN FETCH u.authProviders WHERE u.username = :username")
    Optional<Users> findByUsernameWithAuthProviders(String username);

    @Query("SELECT u FROM Users u LEFT JOIN FETCH u.authProviders WHERE u.email = :email")
    Optional<Users> findByEmailWithAuthProviders(String email);
}
