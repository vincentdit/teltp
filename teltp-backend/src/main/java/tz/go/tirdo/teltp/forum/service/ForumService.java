package tz.go.tirdo.teltp.forum.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.forum.dto.ForumDtos.*;
import tz.go.tirdo.teltp.forum.entity.DiscussionPost;
import tz.go.tirdo.teltp.forum.entity.DiscussionThread;
import tz.go.tirdo.teltp.forum.repository.PostRepository;
import tz.go.tirdo.teltp.forum.repository.ThreadRepository;

@Service
public class ForumService {

    private final ThreadRepository threads;
    private final PostRepository posts;

    public ForumService(ThreadRepository threads, PostRepository posts) {
        this.threads = threads;
        this.posts = posts;
    }

    @Transactional
    public ThreadResponse createThread(String authorUuid, CreateThreadRequest req) {
        DiscussionThread t = new DiscussionThread();
        t.setCourseUuid(req.courseUuid());
        t.setAuthorUuid(authorUuid);
        t.setTitle(req.title());
        t.setBody(req.body());
        return toThread(threads.save(t));
    }

    @Transactional
    public PostResponse reply(String authorUuid, CreatePostRequest req) {
        DiscussionThread t = threads.findByUuid(req.threadUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Thread", req.threadUuid()));
        if (t.isLocked()) throw new BusinessRuleException("Thread is locked");
        DiscussionPost p = new DiscussionPost();
        p.setThreadUuid(req.threadUuid());
        p.setAuthorUuid(authorUuid);
        p.setBody(req.body());
        return toPost(posts.save(p));
    }

    @Transactional(readOnly = true)
    public PageResponse<ThreadResponse> threadsForCourse(String courseUuid, Pageable pageable) {
        return PageResponse.from(
                threads.findByCourseUuidOrderByPinnedDescCreatedAtDesc(courseUuid, pageable), this::toThread);
    }

    @Transactional(readOnly = true)
    public ThreadDetailResponse threadDetail(String threadUuid) {
        DiscussionThread t = threads.findByUuid(threadUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Thread", threadUuid));
        var postList = posts.findByThreadUuidOrderByCreatedAtAsc(threadUuid).stream().map(this::toPost).toList();
        return new ThreadDetailResponse(toThread(t), postList);
    }

    private ThreadResponse toThread(DiscussionThread t) {
        return new ThreadResponse(t.getUuid(), t.getCourseUuid(), t.getAuthorUuid(), t.getTitle(),
                t.getBody(), t.isPinned(), t.isLocked(), t.getCreatedAt());
    }

    private PostResponse toPost(DiscussionPost p) {
        return new PostResponse(p.getUuid(), p.getThreadUuid(), p.getAuthorUuid(), p.getBody(), p.getCreatedAt());
    }
}
