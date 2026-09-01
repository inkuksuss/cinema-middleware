package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "common_code_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE common_code_group SET is_delete = 'Y' WHERE common_code_group_id = ?")
@SQLRestriction("is_delete = 'N'")
public class CommonCodeGroup extends BaseEntity {

    @Id
    @Column(name = "common_code_group_id", length = 100)
    private String commonCodeGroupId;

    @Column(name = "group_name", length = 100, nullable = false)
    private String groupName;

    private String description;

    @OneToMany(mappedBy = "commonCodeGroup")
    private List<CommonCodeDetail> commonCodeDetails = new ArrayList<>();

    public void addCommonCodeDetail(CommonCodeDetail detail) {
        this.commonCodeDetails.add(detail);
    }

    public CommonCodeGroup(String commonCodeGroupId, String groupName, String description) {
        this.commonCodeGroupId = commonCodeGroupId;
        this.groupName = groupName;
        this.description = description;
    }
}
