package org.sang.labmanagement.asset.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetConfigurationService {
	private final AssetConfigurationRepository assetConfigurationRepository;

}
