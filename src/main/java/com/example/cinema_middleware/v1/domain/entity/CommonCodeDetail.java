package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.key.CommonCodeDetailId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;


@Entity
@Table(name = "common_code_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@SQLDelete(sql = "UPDATE common_code_detail SET is_delete = true WHERE common_code_group_id = ? AND common_code_detail_code = ?")
@SQLRestriction("is_delete = false")
public class CommonCodeDetail extends DefaultEntity {

    @EmbeddedId
    private CommonCodeDetailId id;

    @MapsId("commonCodeGroupId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "common_code_group_id")
    private CommonCodeGroup commonCodeGroup;

    @Column(nullable = false, length = 100)
    private String displayName;

    private String description;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    private String attr;

    @Column(nullable = false)
    private boolean isDelete = false;

    public CommonCodeDetail(CommonCodeGroup commonCodeGroup, String commonCodeDetailCode, String displayName) {
        Assert.notNull(commonCodeGroup, "commonCodeGroup can not be null");
        Assert.notNull(commonCodeDetailCode, "commonCodeDetailCode can not be null");
        Assert.notNull(displayName, "displayName can not be null");

        this.id = new CommonCodeDetailId(commonCodeGroup.getCommonCodeGroupId(), commonCodeDetailCode);
        this.commonCodeGroup = commonCodeGroup;
        this.displayName = displayName;
    }
}
