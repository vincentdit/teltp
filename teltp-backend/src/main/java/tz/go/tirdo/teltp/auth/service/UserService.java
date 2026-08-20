package tz.go.tirdo.teltp.auth.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.auth.dto.AuthDtos.UserResponse;
import tz.go.tirdo.teltp.auth.entity.User;
import tz.go.tirdo.teltp.auth.repository.UserRepository;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;

@Service
public class UserService {

    private final UserRepository users;
    private final UserMapper mapper;

    public UserService(UserRepository users, UserMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        Page<User> page = users.findAll(pageable);
        return PageResponse.from(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(String uuid) {
        return mapper.toResponse(require(uuid));
    }

    @Transactional
    public UserResponse setActive(String uuid, boolean active) {
        User u = require(uuid);
        u.setActive(active);
        return mapper.toResponse(users.save(u));
    }

    /** Package-accessible cross-module resolution hook (mirrors CIAP getEntity pattern). */
    public User getEntity(String uuid) {
        return require(uuid);
    }

    /** Resolve the current principal's user uuid from username. */
    public String uuidForUsername(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username)).getUuid();
    }

    private User require(String uuid) {
        return users.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("User", uuid));
    }
}
