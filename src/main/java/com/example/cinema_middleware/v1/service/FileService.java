package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.enums.TableType;
import com.example.cinema_middleware.v1.service.dto.AddFileForm;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    String append(AddFileForm form);

    void remove(Long targetId, TableType tableType);
}
