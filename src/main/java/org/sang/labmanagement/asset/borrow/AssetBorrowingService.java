package org.sang.labmanagement.asset.borrow;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sang.labmanagement.asset.Asset;
import org.sang.labmanagement.asset.AssetRepository;
import org.sang.labmanagement.asset.AssetStatus;
import org.sang.labmanagement.asset.history.AssetHistory;
import org.sang.labmanagement.asset.history.AssetHistoryRepository;
import org.sang.labmanagement.common.PageResponse;
import org.sang.labmanagement.exception.ResourceNotFoundException;
import org.sang.labmanagement.security.email.EmailService;
import org.sang.labmanagement.security.email.EmailTemplateName;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserDetailsServiceImplement;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetBorrowingService {
	private final AssetBorrowingRepository assetBorrowingRepository;
	private final AssetHistoryRepository assetHistoryRepository;
	private final EmailService emailService;
	private final AssetRepository assetRepository;
	private final UserDetailsServiceImplement userService;
	private final AssetBorrowingMapper assetBorrowingMapper;

	// Borrowing asset
	@Transactional
	public AssetBorrowingDTO borrowAsset(AssetBorrowingDTO borrowingDTO) {
		// Kiểm tra tài sản
		Asset asset = assetRepository.findById(borrowingDTO.getAssetId())
				.orElseThrow(
						() -> new ResourceNotFoundException("Asset not found with id: " + borrowingDTO.getAssetId()));

		if (asset.getStatus() != AssetStatus.AVAILABLE) {
			throw new IllegalStateException("Asset is not available for borrowing: " + asset.getStatus());
		}
		if (asset.getQuantity() <= 0) {
			throw new IllegalStateException("Asset quantity is not sufficient");
		}

		// Kiểm tra người dùng
		User user = userService.getUserById(borrowingDTO.getUserId());

		// Kiểm tra thời gian trả dự kiến
		if (borrowingDTO.getExpectedReturnDate().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("Expected return date must be in the future");
		}

		// Tạo giao dịch mượn
		AssetBorrowing borrowing = AssetBorrowing.builder()
				.asset(asset)
				.user(user)
				.expectedReturnDate(borrowingDTO.getExpectedReturnDate())
				.status(BorrowingStatus.PENDING)
				.remarks(borrowingDTO.getRemarks())
				.createdAt(LocalDateTime.now())
				.build();

		// Lưu giao dịch
		AssetBorrowing savedBorrowing = assetBorrowingRepository.save(borrowing);

		// Trả về DTO
		return AssetBorrowingDTO.builder()
				.id(savedBorrowing.getId())
				.assetId(savedBorrowing.getAsset().getId())
				.userId(savedBorrowing.getUser().getId())
				.expectedReturnDate(savedBorrowing.getExpectedReturnDate())
				.status(savedBorrowing.getStatus())
				.username(savedBorrowing.getUser().getUsername())
				.assetName(savedBorrowing.getAsset().getName())
				.remarks(savedBorrowing.getRemarks())
				.build();
	}

	@Transactional
	public AssetBorrowingDTO approveBorrowing(Long borrowingId) {
		AssetBorrowing borrowing = assetBorrowingRepository.findById(borrowingId)
				.orElseThrow(() -> new ResourceNotFoundException("Borrowing not found with id: " + borrowingId));

		if (borrowing.getStatus() != BorrowingStatus.PENDING) {
			throw new IllegalStateException("Borrowing is not in PENDING state");
		}

		borrowing.setStatus(BorrowingStatus.BORROWED);
		borrowing.setBorrowDate(LocalDateTime.now());
		assetBorrowingRepository.save(borrowing);

		Asset asset = borrowing.getAsset();
		asset.setStatus(AssetStatus.BORROWED);
		asset.setAssignedUser(borrowing.getUser());
		asset.setQuantity(asset.getQuantity() - 1);
		assetRepository.save(asset);

		AssetHistory history = AssetHistory.builder()
				.asset(asset)
				.user(borrowing.getUser())
				.previousStatus(AssetStatus.AVAILABLE)
				.newStatus(AssetStatus.BORROWED)
				.changeDate(LocalDateTime.now())
				.remarks("Asset borrowed by user " + borrowing.getUser().getFullName())
				.build();
		assetHistoryRepository.save(history);

		return mapToDTO(borrowing);
	}

	@Transactional
	public AssetBorrowingDTO returnAsset(Long borrowingId) {
		AssetBorrowing borrowing = assetBorrowingRepository.findById(borrowingId)
				.orElseThrow(() -> new ResourceNotFoundException("Borrowing not found with id: " + borrowingId));

		if (borrowing.getStatus() != BorrowingStatus.BORROWED) {
			throw new IllegalStateException("Borrowing is not in BORROWED state");
		}

		Asset asset = borrowing.getAsset();
		borrowing.setStatus(BorrowingStatus.RETURNED);
		borrowing.setReturnDate(LocalDateTime.now());
		assetBorrowingRepository.save(borrowing);

		// Cập nhật tài sản
		asset.setStatus(AssetStatus.AVAILABLE);
		asset.setAssignedUser(null);
		asset.setQuantity(asset.getQuantity() + 1);
		assetRepository.save(asset);

		// Ghi lịch sử
		AssetHistory history = AssetHistory.builder()
				.asset(asset)
				.user(borrowing.getUser())
				.previousStatus(AssetStatus.BORROWED)
				.newStatus(AssetStatus.AVAILABLE)
				.changeDate(LocalDateTime.now())
				.remarks("Asset returned by user " + borrowing.getUser().getFullName())
				.build();
		assetHistoryRepository.save(history);

		return mapToDTO(borrowing);
	}

	public AssetBorrowingDTO getBorrowingById(Long borrowingId) {
		AssetBorrowing borrowing = assetBorrowingRepository.findById(borrowingId)
				.orElseThrow(() -> new ResourceNotFoundException("Borrowing not found with id: " + borrowingId));
		return mapToDTO(borrowing);
	}

	public List<AssetBorrowingDTO> getBorrowingsByUser(Long userId) {
		List<AssetBorrowing> borrowings = assetBorrowingRepository.findByUserId(userId);
		return borrowings.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	private AssetBorrowingDTO mapToDTO(AssetBorrowing borrowing) {
		return AssetBorrowingDTO.builder()
				.id(borrowing.getId())
				.assetId(borrowing.getAsset().getId())
				.assetName(borrowing.getAsset().getName())
				.username(borrowing.getUser().getUsername())
				.userId(borrowing.getUser().getId())
				.borrowDate(borrowing.getBorrowDate())
				.returnDate(borrowing.getReturnDate())
				.expectedReturnDate(borrowing.getExpectedReturnDate())
				.status(borrowing.getStatus())
				.remarks(borrowing.getRemarks())
				.build();
	}

	@Scheduled(cron = "0 0 0 * * ?")
	@Transactional
	public void checkOverdueBorrowings() {
		try {
			LocalDateTime now = LocalDateTime.now();
			List<AssetBorrowing> overdueBorrowings = assetBorrowingRepository
					.findAllByStatusAndExpectedReturnDateBefore(BorrowingStatus.BORROWED, now);

			for (AssetBorrowing borrowing : overdueBorrowings) {
				// Cập nhật trạng thái
				borrowing.setStatus(BorrowingStatus.OVERDUE);
				borrowing.setUpdatedAt(LocalDateTime.now());
				assetBorrowingRepository.save(borrowing);

				// Ghi lịch sử (log BorrowingStatus, not AssetStatus)
				AssetHistory history = AssetHistory.builder()
						.asset(borrowing.getAsset())
						.user(borrowing.getUser())
						.previousStatus(borrowing.getAsset().getStatus())
						.newStatus(borrowing.getAsset().getStatus())
						.changeDate(LocalDateTime.now())
						.remarks("Borrowing marked as OVERDUE for asset " + borrowing.getAsset().getName())
						.build();
				assetHistoryRepository.save(history);

				// Gửi email thông báo
				try {
					emailService.sendOverdueBorrowingEmail(
							borrowing.getUser().getEmail(),
							borrowing.getUser().getFullName(),
							borrowing.getId(),
							borrowing.getAsset().getName(),
							borrowing.getExpectedReturnDate(),
							borrowing.getRemarks(),
							EmailTemplateName.OVERDUE_BORROWING,
							"Overdue Asset Borrowing Notification",
							LocaleContextHolder.getLocale()
					);
					log.info("Sent overdue email for borrowing ID {} to user {}",
							borrowing.getId(), borrowing.getUser().getFullName());
				} catch (MessagingException e) {
					log.error("Failed to send overdue email for borrowing ID {}: {}",
							borrowing.getId(), e.getMessage());
				}

				log.info("Marked borrowing ID {} as OVERDUE for user {}",
						borrowing.getId(), borrowing.getUser().getFullName());
			}

			if (overdueBorrowings.isEmpty()) {
				log.info("No overdue borrowings found at {}", now);
			}
		} catch (Exception e) {
			log.error("Error checking overdue borrowings: {}", e.getMessage());
			throw new RuntimeException("Failed to check overdue borrowings", e);
		}
	}

	public PageResponse<AssetBorrowingDTO> getAllBorrowings(Pageable pageable, String keyword, String statuses) {
		// Tạo specification để lọc
		Specification<AssetBorrowing> spec = AssetBorrowingSpecification.filterByKeywordAndStatus(keyword, statuses);

		// Truy vấn dữ liệu phân trang
		Page<AssetBorrowing> assetBorrowingsPage = assetBorrowingRepository.findAll(spec, pageable);

		// Chuyển đổi sang DTO
		List<AssetBorrowingDTO> dtos = assetBorrowingsPage.getContent()
				.stream()
				.map(assetBorrowingMapper::toDTO)
				.toList();

		// Xây dựng response
		return PageResponse.<AssetBorrowingDTO>builder()
				.content(dtos)
				.number(assetBorrowingsPage.getNumber())
				.size(assetBorrowingsPage.getSize())
				.totalElements(assetBorrowingsPage.getTotalElements())
				.totalPages(assetBorrowingsPage.getTotalPages())
				.first(assetBorrowingsPage.isFirst())
				.last(assetBorrowingsPage.isLast())
				.build();
	}
}