package tz.go.tirdo.teltp.forum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.forum.entity.DiscussionThread;

import java.util.Optional;

public interface ThreadRepository extends JpaRepository<DiscussionThread, Long> {
    Optional<DiscussionThread> findByUuid(String uuid);
    Page<DiscussionThread> findByCourseUuidOrderByPinnedDescCreatedAtDesc(String courseUuid, Pageable pageable);
}
