package com.backend.pojo.vo;

import com.backend.pojo.entity.User;
import com.backend.pojo.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private UserStatus status;
    private List<String> roles;
    private List<String> permissions;
    private UserMembershipVO membership;

    public static UserProfileVO from(User user, List<String> roles, List<String> permissions) {
        return from(user, roles, permissions, null);
    }

    public static UserProfileVO from(
            User user,
            List<String> roles,
            List<String> permissions,
            UserMembershipVO membership
    ) {
        return new UserProfileVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getStatus(),
                roles,
                permissions,
                membership
        );
    }
}
