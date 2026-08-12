package com.vionsys.hireai.candidate.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import com.vionsys.hireai.common.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
		name = "skills",
		indexes = {
				@Index(
						name = "idx_skill_name",
						columnList = "name"
				)
		}
)
@SQLDelete(sql = "UPDATE skills SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Skill extends BaseEntity {

	@Id
	@GeneratedValue
	@Column(
			name = "id",
			nullable = false,
			updatable = false
	)
	private UUID id;

	@Column(
			name = "name",
			nullable = false,
			unique = true,
			length = 100
	)
	private String name;

	@Column(
			name = "description",
			length = 255
	)
	private String description;

	@Builder.Default
	@ManyToMany(
			mappedBy = "skills",
			fetch = FetchType.LAZY
	)
	private Set<Candidate> candidates = new HashSet<>();
}