package com.backend.pojo.vo;

import com.backend.pojo.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {
    private Long id;
    private String code;
    private String name;
    private String description;

    public static RoleVO from(Role role) {
        return new RoleVO(role.getId(), role.getCode(), role.getName(), role.getDescription());
    }
}
