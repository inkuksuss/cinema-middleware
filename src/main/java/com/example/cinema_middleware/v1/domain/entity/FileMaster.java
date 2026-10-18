package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.UsageType;
import com.example.cinema_middleware.v1.domain.entity.enums.TableType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;


@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE file_master SET is_delete = 'Y' WHERE file_id = ?")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class FileMaster extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private TableType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private UsageType usageType;

    private String originalName;

    private String destination;

    private Long size;

    private String contentType;

    private Integer viewOrder = 1;

    public static FileMaster create(
            Long targetId,
            TableType targetType,
            UsageType usageType,
            String destination,
            MultipartFile file
    ) {
        Assert.notNull(targetId, "targetId can not be null");
        Assert.notNull(targetType, "targetType can not be null");
        Assert.notNull(usageType, "usageType can not be null");
        Assert.notNull(file, "file can not be null");
        Assert.hasText(destination, "destination can not be null");

        FileMaster fileMaster = new FileMaster();
        fileMaster.targetId = targetId;
        fileMaster.targetType = targetType;
        fileMaster.usageType = usageType;
        fileMaster.originalName = file.getOriginalFilename();
        fileMaster.size = file.getSize();
        fileMaster.contentType = file.getContentType();
        fileMaster.destination = destination;

        return fileMaster;
    }

    public void setViewOrder(Integer viewOrder) {
        this.viewOrder = viewOrder;
    }
}
