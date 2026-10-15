package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.enums.TableType;
import com.example.cinema_middleware.v1.repository.FileRepository;
import com.example.cinema_middleware.v1.service.dto.AddFileForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class S3FileService implements FileService {

    private final FileRepository fileRepository;


    @Override
    public String append(AddFileForm form) {
        return "";
    }

    @Override
    public void remove(Long targetId, TableType tableType) {
        // TODO
    }
}
