package com.urlshortener.repository;

import com.urlshortener.model.Url;
import com.urlshortener.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortCode(String shortCode);
    Boolean existsByShortCode(String shortCode);
    Page<Url> findByUser(User user, Pageable pageable);
} 