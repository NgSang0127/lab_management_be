package org.sang.labmanagement.asset.borrow;

import org.sang.labmanagement.asset.history.AssetHistoryDTO;
import org.springframework.stereotype.Component;

@Component
public class AssetBorrowingMapper {
	public AssetBorrowingDTO toDTO(AssetBorrowing assetBorrowing){
		return AssetBorrowingDTO.builder()
				.id(assetBorrowing.getId())
				.assetId(assetBorrowing.getAsset().getId())
				.userId(assetBorrowing.getUser().getId())
				.borrowDate(assetBorrowing.getBorrowDate())
				.assetName(assetBorrowing.getAsset().getName())
				.username(assetBorrowing.getUser().getUsername())
				.status(assetBorrowing.getStatus())
				.returnDate(assetBorrowing.getReturnDate())
				.expectedReturnDate(assetBorrowing.getExpectedReturnDate())
				.remarks(assetBorrowing.getRemarks())
				.build();
	}

}
