package org.sang.labmanagement.asset.configuration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetConfigurationRepository extends JpaRepository<AssetConfiguration,Long> {

}
