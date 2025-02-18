package org.sang.labmanagement.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
public class OwnerController {
	private final OwnerService ownerService;

	@PreAuthorize("hasRole('OWNER') or hasRole('CO_OWNER')")
	@PostMapping("/transfer-ownership/{newOwnerId}")
	public ResponseEntity<String> transferOwnership(@PathVariable Long newOwnerId,
			Authentication connectedUser){
		ownerService.transferOwnership(connectedUser,newOwnerId);
		return ResponseEntity.ok("Ownership transferred successful !");
	}

	@PreAuthorize("hasRole('OWNER') or hasRole('CO_OWNER')")
	@PostMapping("/promote/{userId}")
	public ResponseEntity<String> promoteUser(@PathVariable Long userId,
			Authentication connectedUser) {
		ownerService.promoteToCoOwner(connectedUser, userId);
		return ResponseEntity.ok("User promoted to Co-Owner successfully");
	}
}
