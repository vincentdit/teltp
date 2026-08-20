package tz.go.tirdo.teltp.forum.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public final class ForumDtos {
    private ForumDtos() {}

    public record CreateThreadRequest(@NotBlank String courseUuid, @NotBlank String title, String body) {}
    public record CreatePostRequest(@NotBlank String threadUuid, @NotBlank String body) {}

    public record ThreadResponse(String uuid, String courseUuid, String authorUuid, String title,
                                 String body, boolean pinned, boolean locked, Instant createdAt) {}
    public record PostResponse(String uuid, String threadUuid, String authorUuid, String body, Instant createdAt) {}
    public record ThreadDetailResponse(ThreadResponse thread, List<PostResponse> posts) {}
}
