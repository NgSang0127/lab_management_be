package org.sang.labmanagement.asset.report;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.asset.Asset;
import org.sang.labmanagement.asset.AssetRepository;
import org.sang.labmanagement.asset.AssetStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetReportService {
	private final AssetRepository assetRepository;

	public Map<AssetStatus, Long> getAssetCountByStatus() {
		List<Asset> assets = assetRepository.findAll();
		Map<AssetStatus, Long> countMap = new EnumMap<>(AssetStatus.class);
		for (AssetStatus status : AssetStatus.values()) {
			countMap.put(status, 0L);
		}
		for (Asset asset : assets) {
			countMap.put(asset.getStatus(), countMap.get(asset.getStatus()) + 1);
		}
		return countMap;
	}

}
