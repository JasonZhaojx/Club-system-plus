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
    private UserStatus status;
    private List<String> roles;
    private List<String> permissions;

    public static UserProfileVO from(User user) {
        boolean admin = user.getId() != null && user.getId() == 1L;
        return new UserProfileVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getStatus(),
                admin ? List.of("ADMIN") : List.of("USER"),
                admin ? List.of("dashboard:view", "system:maintain") : List.of()
        );
    }
}
