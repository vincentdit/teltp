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
import tz.go.tirdo.teltp.integration.identity.NidaVerificationService;

@Service
public class UserService {

    private final UserRepository users;
    private final UserMapper mapper;
    private final NidaVerificationService nida;

    public UserService(UserRepository users, UserMapper mapper, NidaVerificationService nida) {
        this.users = users;
        this.mapper = mapper;
        this.nida = nida;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        Page<User> page = users.findAll(pageable);
        return PageResponse.from(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) return list(pageable);
        return PageResponse.from(users.search(q.trim(), pageable), mapper::toResponse);
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

    @Transactional
    public UserResponse recordNida(String username, String nationalId) {
        User u = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        NidaVerificationService.NidaResult result = nida.verify(nationalId);
        u.setNationalId(nationalId);
        u.setNidaVerified(result.verified());
        return mapper.toResponse(users.save(u));
    }

    public boolean nidaLive() { return nida.isEnabled(); }
    public String uuidForUsername(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username)).getUuid();
    }

    private User require(String uuid) {
        return users.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("User", uuid));
    }
}
