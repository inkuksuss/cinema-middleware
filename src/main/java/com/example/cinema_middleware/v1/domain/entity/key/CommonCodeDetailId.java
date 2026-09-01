package com.example.cinema_middleware.v1.domain.entity.key;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCodeDetailId implements Serializable {

    @Column(name = "common_code_group_id", length = 100)
    private String commonCodeGroupId;

    @Column(name = "common_code_detail_code", length = 100)
    private String commonCodeDetailCode;

    public CommonCodeDetailId(String commonCodeGroupId, String commonCodeDetailCode) {
        this.commonCodeGroupId = commonCodeGroupId;
        this.commonCodeDetailCode = commonCodeDetailCode;
    }
}
