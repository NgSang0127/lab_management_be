package org.sang.labmanagement.asset.importexport;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("api/v1/admin/assets/import-export")
@RequiredArgsConstructor
public class AssetImportExportController {

	private final AssetImportExportService assetImportExportService;


	@PostMapping("/import/csv")
	public ResponseEntity<?> importAssetsFromCSV(@RequestParam("file") MultipartFile file) {
		try {
			assetImportExportService.importAssetsFromCSV(file);
			return ResponseEntity.ok("Assets imported successfully!");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Failed to import assets: " + e.getMessage());
		}
	}


	@PostMapping("/import/excel")
	public ResponseEntity<?> importAssetsFromExcel(@RequestParam("file") MultipartFile file) {
		try {
			assetImportExportService.importAssetsFromExcel(file);
			return ResponseEntity.ok("Assets imported successfully!");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Failed to import assets: " + e.getMessage());
		}
	}


	@GetMapping("/export/csv")
	public ResponseEntity<InputStreamResource> exportAssetsToCSV() {
		try {
			ByteArrayInputStream in = assetImportExportService.exportAssetsToCSV();

			String filename = "assets_" + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".csv";

			ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
					.filename(filename)
					.build();

			HttpHeaders headers = new HttpHeaders();
			headers.setContentDisposition(contentDisposition);
			headers.setContentType(MediaType.parseMediaType("text/csv"));


			return ResponseEntity.ok()
					.headers(headers)
					.body(new InputStreamResource(in));
		} catch (Exception e) {
			return ResponseEntity.status(500).build();
		}
	}


	@GetMapping("/export/excel")
	public ResponseEntity<InputStreamResource> exportAssetsToExcel() {
		try {
			ByteArrayInputStream in = assetImportExportService.exportAssetsToExcel();

			String filename = "assets_" + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) +
					".xlsx";

			ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
					.filename(filename)
					.build();

			HttpHeaders headers = new HttpHeaders();
			headers.setContentDisposition(contentDisposition);
			headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));


			return ResponseEntity.ok()
					.headers(headers)
					.body(new InputStreamResource(in));
		} catch (Exception e) {
			return ResponseEntity.status(500).build();
		}
	}
}
