package com.backend.pojo.vo;

import com.backend.pojo.entity.Permission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionVO {
    private Long id;
    private String code;
    private String name;
    private String description;

    public static PermissionVO from(Permission permission) {
        return new PermissionVO(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription()
        );
    }
}
