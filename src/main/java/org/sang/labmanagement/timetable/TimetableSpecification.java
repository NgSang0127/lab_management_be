package org.sang.labmanagement.timetable;

import jakarta.persistence.criteria.*;
import org.apache.commons.lang3.StringUtils;
import org.sang.labmanagement.course.Course;
import org.sang.labmanagement.room.Room;
import org.sang.labmanagement.semester.Semester;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TimetableSpecification {

	public static Specification<Timetable> getTimetableByKeywordAndRoomAndStatus(
			String keyword, String roomName, String semesterIds, String status) {
		return (root, query, criteriaBuilder) -> {
			// Ensure distinct results to avoid duplicates from joins
			query.distinct(true);

			List<Predicate> predicates = new ArrayList<>();

			// Filter by keyword (searches in timetableName, classId, description, course.name, course.NH, course.TH)
			if (StringUtils.isNotBlank(keyword)) {
				String keywordPattern = "%" + keyword.toLowerCase() + "%";
				// Timetable fields
				Predicate timetableNamePredicate = criteriaBuilder.like(
						criteriaBuilder.lower(root.get("timetableName")), keywordPattern);
				Predicate classIdPredicate = criteriaBuilder.like(
						criteriaBuilder.lower(root.get("classId")), keywordPattern);
				Predicate descriptionPredicate = criteriaBuilder.like(
						criteriaBuilder.lower(root.get("description")), keywordPattern);

				// Course fields (left join to include timetables without courses)
				Join<Timetable, Course> courseJoin = root.join("courses", JoinType.LEFT);
				Predicate courseNamePredicate = criteriaBuilder.like(
						criteriaBuilder.lower(courseJoin.get("name")), keywordPattern);
				Predicate courseNHPredicate = criteriaBuilder.like(
						criteriaBuilder.lower(courseJoin.get("NH")), keywordPattern);
				Predicate courseTHPredicate = criteriaBuilder.like(
						criteriaBuilder.lower(courseJoin.get("TH")), keywordPattern);

				// Combine all predicates with OR
				predicates.add(criteriaBuilder.or(
						timetableNamePredicate,
						classIdPredicate,
						descriptionPredicate,
						courseNamePredicate,
						courseNHPredicate,
						courseTHPredicate
				));
			}

			// Filter by roomName
			if (StringUtils.isNotBlank(roomName)) {
				Join<Timetable, Room> roomJoin = root.join("room", JoinType.LEFT);
				predicates.add(criteriaBuilder.like(
						criteriaBuilder.lower(roomJoin.get("name")),
						"%" + roomName.toLowerCase() + "%"
				));
			}

			// Filter by semesterIds
			if (StringUtils.isNotBlank(semesterIds)) {
				Join<Timetable, Semester> semesterJoin = root.join("semester", JoinType.LEFT);
				List<Long> semesterIdList = new ArrayList<>();
				for (String id : semesterIds.split(",")) {
					try {
						semesterIdList.add(Long.parseLong(id.trim()));
					} catch (NumberFormatException e) {
						// Skip invalid IDs
					}
				}
				if (!semesterIdList.isEmpty()) {
					predicates.add(semesterJoin.get("id").in(semesterIdList));
				}
			}

			// Filter by status
			if (StringUtils.isNotBlank(status)) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("status")),
						status.toLowerCase()
				));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
		}
	}

