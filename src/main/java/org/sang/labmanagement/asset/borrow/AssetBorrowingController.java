package org.sang.labmanagement.asset.borrow;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/borrowings")
@RequiredArgsConstructor
public class AssetBorrowingController {

	private final AssetBorrowingService assetBorrowingService;

	@GetMapping
	public ResponseEntity<PageResponse<AssetBorrowingDTO>>getAllBorrowings(
			@RequestParam(name = "page", defaultValue = "0", required = false) int page,
			@RequestParam(name = "size", defaultValue = "10", required = false) int size,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "statuses", required = false) String statuses,
			@RequestParam(name = "sortBy", required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = "asc", required = false) String sortOrder
	){

		Sort sort = Sort.unsorted();
		if (sortBy != null && !sortBy.isBlank()) {
			String mappedSortBy;
			Sort.Direction direction = sortOrder.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
			switch (sortBy) {
				case "assetName":
					mappedSortBy = "asset.name";
					break;
				case "username":
					mappedSortBy = "user.username";
					break;
				case "id":
				case "status":
				case "borrowDate":
				case "expectedReturnDate":
				case "returnDate":
					mappedSortBy = sortBy;
					break;
				default:
					System.err.println("Invalid sortBy value: " + sortBy + ", defaulting to id");
					mappedSortBy = "id";
			}
			sort = Sort.by(direction, mappedSortBy);
		}

		Pageable pageable = PageRequest.of(page, size, sort);
		PageResponse<AssetBorrowingDTO> response = assetBorrowingService.getAllBorrowings(pageable, keyword, statuses);
		return ResponseEntity.ok(response);
	}

	@PostMapping
	public ResponseEntity<AssetBorrowingDTO> createBorrowing(@RequestBody AssetBorrowingDTO borrowingDTO) {
		AssetBorrowingDTO result = assetBorrowingService.borrowAsset(borrowingDTO);
		return ResponseEntity.ok(result);
	}

	// Phê duyệt yêu cầu mượn
	@PutMapping("/{id}/approve")
	@PreAuthorize("hasRole('ADMIN')") // Chỉ admin phê duyệt
	public ResponseEntity<AssetBorrowingDTO> approveBorrowing(@PathVariable Long id) {
		AssetBorrowingDTO result = assetBorrowingService.approveBorrowing(id);
		return ResponseEntity.ok(result);
	}

	// Trả tài sản
	@PutMapping("/{id}/return")
	public ResponseEntity<AssetBorrowingDTO> returnAsset(@PathVariable Long id) {
		AssetBorrowingDTO result = assetBorrowingService.returnAsset(id);
		return ResponseEntity.ok(result);
	}

	// Lấy thông tin giao dịch mượn
	@GetMapping("/{id}")
	public ResponseEntity<AssetBorrowingDTO> getBorrowingById(@PathVariable Long id) {
		AssetBorrowingDTO result = assetBorrowingService.getBorrowingById(id);
		return ResponseEntity.ok(result);
	}

	// Lấy danh sách giao dịch mượn của người dùng
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<AssetBorrowingDTO>> getBorrowingsByUser(@PathVariable Long userId) {
		List<AssetBorrowingDTO> result = assetBorrowingService.getBorrowingsByUser(userId);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/check-overdue")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> checkOverdueBorrowings() {
		assetBorrowingService.checkOverdueBorrowings();
		return ResponseEntity.ok("Overdue borrowings checked successfully");
	}

}
