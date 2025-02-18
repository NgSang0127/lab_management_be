package org.sang.labmanagement.asset.report;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.asset.AssetStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AssetReportController {
	private final AssetReportService assetReportService;


	@GetMapping("/assets/count-by-status")
	public ResponseEntity<Map<AssetStatus, Long>> getAssetCountByStatus() {
		Map<AssetStatus, Long> report = assetReportService.getAssetCountByStatus();
		return ResponseEntity.ok(report);
	}
}
