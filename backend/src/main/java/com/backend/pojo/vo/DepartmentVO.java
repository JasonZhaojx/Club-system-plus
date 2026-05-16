package com.backend.pojo.vo;

import com.backend.pojo.entity.Department;
import com.backend.pojo.entity.DepartmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentVO {
    private Long id;
    private String name;
    private String description;
    private DepartmentStatus status;

    public static DepartmentVO from(Department department) {
        return new DepartmentVO(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.getStatus()
        );
    }
}
