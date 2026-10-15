package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.FileMaster;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class FileSummary {

    private String originalName;

    private String destination;

    private Integer viewOrder;

    public static FileSummary from(FileMaster fileMaster) {
        FileSummary fileSummary = new FileSummary();
        fileSummary.originalName = fileMaster.getOriginalName();
        fileSummary.destination = fileMaster.getDestination();
        fileSummary.viewOrder = fileMaster.getViewOrder();

        return fileSummary;
    }
}
