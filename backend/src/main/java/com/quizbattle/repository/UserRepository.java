package com.quizbattle.repository;

import com.quizbattle.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.team")
    List<User> findAllWithTeams();
}

