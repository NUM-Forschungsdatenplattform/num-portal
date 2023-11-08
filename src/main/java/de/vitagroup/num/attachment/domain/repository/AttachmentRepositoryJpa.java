package de.vitagroup.num.attachment.domain.repository;

import de.vitagroup.num.attachment.domain.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepositoryJpa extends JpaRepository<Attachment, Long> {

    @Query("SELECT new Attachment (atc.id, atc.name, atc.description, atc.uploadDate, atc.reviewCounter) " +
            "FROM Attachment atc ")
    List<Attachment> getAttachments();

    @Modifying
    @Query("UPDATE Attachment atch SET atch.reviewCounter = atch.reviewCounter + 1 WHERE atch.projectId = :projectId")
    void updateReviewCounterByProjectId(@Param("projectId") Long projectId);

    Optional<Attachment> findByIdAndProjectId(Long id, Long projectId);

    @Query("SELECT new Attachment (atc.id, atc.name, atc.description, atc.uploadDate, atc.reviewCounter) " +
            "FROM Attachment atc " +
            "WHERE atc.projectId = :projectId")
    List<Attachment> findAttachmentsByProjectId(@Param("projectId") Long projectId);
}
