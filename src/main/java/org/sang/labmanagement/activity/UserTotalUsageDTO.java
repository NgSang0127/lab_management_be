package org.sang.labmanagement.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sang.labmanagement.user.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserTotalUsageDTO {
	private User user;

	private Long totalUsageTime;
}
