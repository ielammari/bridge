package io.github.ielammari.bridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ielammari.bridge.model.OrganisationSettings;

public interface OrganisationSettingsRepository extends JpaRepository<OrganisationSettings, Short> {
}
