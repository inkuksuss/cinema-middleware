package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.key.CommonCodeDetailId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;


@Entity
@Table(name = "common_code_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE common_code_detail SET is_delete = 'Y' WHERE common_code_group_id = ? AND common_code_detail_code = ?")
@SQLRestriction("is_delete = 'N'")
public class CommonCodeDetail extends BaseEntity {

    @EmbeddedId
    private CommonCodeDetailId id;

    @MapsId("commonCodeGroupId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "common_code_group_id")
    private CommonCodeGroup commonCodeGroup;

    private String description;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    private String attr;

    public CommonCodeDetail(CommonCodeGroup commonCodeGroup, String commonCodeDetailCode, String description) {
        this.id = new CommonCodeDetailId(commonCodeGroup.getCommonCodeGroupId(), commonCodeDetailCode);
        this.commonCodeGroup = commonCodeGroup;
        this.description = description;
    }

    public CommonCodeDetail(CommonCodeGroup commonCodeGroup, String commonCodeDetailCode, String description, Integer sortOrder) {
        this(commonCodeGroup, commonCodeDetailCode, description);
        this.sortOrder = sortOrder;
    }

    public CommonCodeDetail(CommonCodeGroup commonCodeGroup, String commonCodeDetailCode, String description, Integer sortOrder, String attr) {
        this(commonCodeGroup, commonCodeDetailCode, description, sortOrder);
        this.attr = attr;
    }
}
