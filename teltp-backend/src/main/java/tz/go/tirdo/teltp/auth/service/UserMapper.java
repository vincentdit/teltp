package tz.go.tirdo.teltp.auth.service;

import org.springframework.stereotype.Component;
import tz.go.tirdo.teltp.auth.dto.AuthDtos.UserResponse;
import tz.go.tirdo.teltp.auth.entity.Role;
import tz.go.tirdo.teltp.auth.entity.User;

import java.util.stream.Collectors;

@Component
public class UserMapper {
    public UserResponse toResponse(User u) {
        return new UserResponse(
                u.getUuid(),
                u.getUsername(),
                u.getEmail(),
                u.fullName(),
                u.getProfession(),
                u.getOrganization() == null ? null : u.getOrganization().getUuid(),
                u.isActive(),
                u.getRoles().stream().map(Role::getName).map(Enum::name).collect(Collectors.toSet()));
    }
}
