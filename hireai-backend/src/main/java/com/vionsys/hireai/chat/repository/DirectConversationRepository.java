package com.vionsys.hireai.chat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vionsys.hireai.chat.entity.DirectConversation;

@Repository("directConversationRepository")
public interface DirectConversationRepository extends JpaRepository<DirectConversation, UUID> {

    @Query("""
        SELECT c FROM DirectConversation c
        LEFT JOIN FETCH c.recruiter r
        LEFT JOIN FETCH c.candidate cd
        LEFT JOIN FETCH c.job j
        LEFT JOIN FETCH c.jobApplication ja
        WHERE (c.recruiter.id = :userId OR c.candidate.id = :userId)
          AND c.isActive = true
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
    """)
    List<DirectConversation> findAllByUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT c FROM DirectConversation c
        WHERE c.recruiter.id = :recruiterId
          AND c.candidate.id = :candidateId
          AND (:jobId IS NULL OR c.job.id = :jobId)
          AND c.isActive = true
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
    """)
    List<DirectConversation> findExistingConversations(
            @Param("recruiterId") UUID recruiterId,
            @Param("candidateId") UUID candidateId,
            @Param("jobId") UUID jobId
    );

    @Query("""
        SELECT c FROM DirectConversation c
        LEFT JOIN FETCH c.recruiter
        LEFT JOIN FETCH c.candidate
        LEFT JOIN FETCH c.job
        LEFT JOIN FETCH c.jobApplication
        WHERE c.id = :id AND c.isActive = true
    """)
    Optional<DirectConversation> findByIdWithDetails(@Param("id") UUID id);
}
