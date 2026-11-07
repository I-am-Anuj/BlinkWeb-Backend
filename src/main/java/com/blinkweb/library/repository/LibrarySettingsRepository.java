package com.blinkweb.library.repository;

import com.blinkweb.library.entity.LibrarySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LibrarySettingsRepository extends JpaRepository<LibrarySettings, Long> {

    @Query("SELECT s FROM LibrarySettings s")
    Optional<LibrarySettings> findCurrentSettings();
}
