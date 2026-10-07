package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import com.county.vision.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_account")
public class UserAccount extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String username;

    private String password;
    private String realName;

    @Enumerated(EnumType.STRING)
    private Role role;

    /** 所属学校（学校角色使用），县级/医疗机构为空 */
    @ManyToOne(fetch = FetchType.LAZY)
    private School school;

    /** 所属机构名称（医院、门诊、筛查机构） */
    private String orgName;

    private String phone;
}
