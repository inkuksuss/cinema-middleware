package com.example.cinema_middleware.v1.service.dto.forms;

import com.example.cinema_middleware.v1.domain.entity.enums.UsageType;
import com.example.cinema_middleware.v1.domain.entity.enums.TableType;
import lombok.Getter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@ToString
public class AddFileForm {

    private Long targetId;

    private TableType targetType;

    private UsageType usageType;

    private Integer viewOrder;

    private MultipartFile file;

    public AddFileForm(Long targetId, TableType targetType, UsageType usageType, MultipartFile file) {
        this.targetId = targetId;
        this.targetType = targetType;
        this.usageType = usageType;
        this.file = file;
    }

    public AddFileForm(Long targetId, TableType targetType, UsageType usageType, Integer viewOrder, MultipartFile file) {
        this(targetId, targetType, usageType, file);
        this.viewOrder = viewOrder;
    }
}
