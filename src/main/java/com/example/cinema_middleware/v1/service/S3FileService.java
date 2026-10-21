package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.enums.TableType;
import com.example.cinema_middleware.v1.repository.FileRepository;
import com.example.cinema_middleware.v1.service.dto.forms.AddFileForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
