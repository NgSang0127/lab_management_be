package org.sang.labmanagement.owner;

import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.exception.OperationNotPermittedException;
import org.sang.labmanagement.user.Role;
import org.sang.labmanagement.user.User;
import org.sang.labmanagement.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerService {
	private final UserRepository userRepository;

	@Transactional
	public void transferOwnership(Authentication connectedUser,Long newOwnerId){
		var currentOwner=(User) connectedUser.getPrincipal();
		if(!currentOwner.getRole().equals(Role.OWNER)){
			throw new OperationNotPermittedException("Only the owner can transfer ownership");
		}
		User newOwner=userRepository.findById(newOwnerId)
				.orElseThrow(()->new UsernameNotFoundException("New Owner not found with id :"+newOwnerId));
		currentOwner.setRole(Role.ADMIN);
		newOwner.setRole(Role.CO_OWNER);
		userRepository.save(newOwner);
		userRepository.save(currentOwner);
	}

	@Transactional
	public void promoteToCoOwner(Authentication connectedUser, Long userId) {
		var owner=(User) connectedUser.getPrincipal();
		if (!owner.getRole().equals(Role.OWNER)) {
			throw new OperationNotPermittedException("Only the owner can promote users");
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		user.setRole(Role.CO_OWNER);
		userRepository.save(user);
	}
}
