package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.FileUsage;
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
@SQLRestriction("is_delete = false")
@SQLDelete(sql = "UPDATE file_master SET is_delete = true WHERE file_id = ?")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class FileMaster extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TableType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FileUsage usage;

    private String originalName;

    private String destination;

    private Long size;

    private String contentType;

    private Integer viewOrder = 1;

    @Column(nullable = false)
    private boolean isDelete = false;

    public static FileMaster create(
            Long targetId,
            TableType targetType,
            FileUsage usage,
            String destination,
            MultipartFile file
    ) {
        Assert.notNull(targetId, "targetId can not be null");
        Assert.notNull(targetType, "targetType can not be null");
        Assert.notNull(usage, "usage can not be null");
        Assert.notNull(file, "file can not be null");
        Assert.hasText(destination, "destination can not be null");

        FileMaster fileMaster = new FileMaster();
        fileMaster.targetId = targetId;
        fileMaster.targetType = targetType;
        fileMaster.usage = usage;
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
