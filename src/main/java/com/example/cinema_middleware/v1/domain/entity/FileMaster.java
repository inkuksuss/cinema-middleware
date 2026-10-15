package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.UsageType;
import com.example.cinema_middleware.v1.domain.entity.enums.TableType;
import com.example.cinema_middleware.v1.service.dto.AddFileForm;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.io.File;

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

    public static FileMaster create(AddFileForm form, File file) {
        if (form == null || file == null) throw new IllegalStateException();
        FileMaster fileMaster = new FileMaster();
        fileMaster.targetId = form.getTargetId();
        fileMaster.targetType = form.getTargetType();
        fileMaster.usageType = form.getUsageType();
        fileMaster.originalName = form.getFile().getOriginalFilename();
        fileMaster.size = form.getFile().getSize();
        fileMaster.contentType = form.getFile().getContentType();
        fileMaster.viewOrder = form.getViewOrder() == null ? 1 : form.getViewOrder();
        fileMaster.destination = file.getAbsolutePath();

        return fileMaster;
    }
}
