package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.FileMaster;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

@Getter
@ToString
public class FileSummary {

    private String originalName;

    private String destination;

    private Integer viewOrder;

    public static FileSummary from(FileMaster fileMaster) {
        Assert.notNull(fileMaster, "fileMaster can not be null");

        FileSummary fileSummary = new FileSummary();
        fileSummary.originalName = fileMaster.getOriginalName();
        fileSummary.destination = fileMaster.getDestination();
        fileSummary.viewOrder = fileMaster.getViewOrder();

        return fileSummary;
    }
}
