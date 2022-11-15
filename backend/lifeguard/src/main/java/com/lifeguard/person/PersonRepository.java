package com.lifeguard.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository  extends JpaRepository<LifeguardUser, Long> {
    Optional<LifeguardUser> findByEmail(String email);
}
