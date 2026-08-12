package com.vionsys.hireai.candidate.specification;

import org.springframework.data.jpa.domain.Specification;

import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Skill;
import com.vionsys.hireai.candidate.filter.CandidateFilter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public final class CandidateSpecification {

	private CandidateSpecification() {
	}

	public static Specification<Candidate> withFilter(
			CandidateFilter filter) {

		return (root, query, cb) -> {

			var predicate = cb.conjunction();

			if (filter == null) {
				return predicate;
			}

			// Candidate ID
			if (filter.getCandidateId() != null
					&& !filter.getCandidateId().isBlank()) {

				predicate = cb.and(
						predicate,
						cb.equal(
								root.get("candidateId"),
								filter.getCandidateId()
						)
				);
			}

			// First name
			if (filter.getFirstName() != null
					&& !filter.getFirstName().isBlank()) {

				predicate = cb.and(
						predicate,
						cb.like(
								cb.lower(root.get("firstName")),
								"%" + filter.getFirstName().toLowerCase() + "%"
						)
				);
			}

			// Last name
			if (filter.getLastName() != null
					&& !filter.getLastName().isBlank()) {

				predicate = cb.and(
						predicate,
						cb.like(
								cb.lower(root.get("lastName")),
								"%" + filter.getLastName().toLowerCase() + "%"
						)
				);
			}

			// Email
			if (filter.getEmail() != null
					&& !filter.getEmail().isBlank()) {

				predicate = cb.and(
						predicate,
						cb.equal(
								cb.lower(root.get("email")),
								filter.getEmail().toLowerCase()
						)
				);
			}

			// Phone
			if (filter.getPhone() != null
					&& !filter.getPhone().isBlank()) {

				predicate = cb.and(
						predicate,
						cb.equal(
								root.get("phone"),
								filter.getPhone()
						)
				);
			}

			// Location
			if (filter.getLocation() != null
					&& !filter.getLocation().isBlank()) {

				predicate = cb.and(
						predicate,
						cb.like(
								cb.lower(root.get("location")),
								"%" + filter.getLocation().toLowerCase() + "%"
						)
				);
			}

			// Candidate status
			if (filter.getCandidateStatus() != null) {

				predicate = cb.and(
						predicate,
						cb.equal(
								root.get("candidateStatus"),
								filter.getCandidateStatus()
						)
				);
			}

			// Minimum experience
			if (filter.getExperience() != null) {

				predicate = cb.and(
						predicate,
						cb.greaterThanOrEqualTo(
								root.get("experience"),
								filter.getExperience()
						)
				);
			}

			// Skill
			if (filter.getSkill() != null
					&& !filter.getSkill().isBlank()) {

				Join<Candidate, Skill> skillJoin =
						root.join("skills", JoinType.INNER);

				predicate = cb.and(
						predicate,
						cb.equal(
								cb.lower(skillJoin.get("name")),
								filter.getSkill().toLowerCase()
						)
				);
			}

			/*
			 * Candidate has a ManyToMany relationship with Skill.
			 * distinct(true) prevents duplicate Candidate records
			 * when the query joins the candidate_skills table.
			 */
			query.distinct(true);

			return predicate;
		};
	}
}