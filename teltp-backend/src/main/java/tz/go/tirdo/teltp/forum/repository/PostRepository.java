package tz.go.tirdo.teltp.forum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.forum.entity.DiscussionPost;

import java.util.List;

public interface PostRepository extends JpaRepository<DiscussionPost, Long> {
    List<DiscussionPost> findByThreadUuidOrderByCreatedAtAsc(String threadUuid);
}
