package com.example.cinema_middleware.v1.service;

import com.example.cinema_middleware.v1.domain.entity.FileMaster;
import com.example.cinema_middleware.v1.domain.entity.enums.TableType;
import com.example.cinema_middleware.v1.repository.FileRepository;
import com.example.cinema_middleware.v1.service.dto.AddFileForm;
import com.example.cinema_middleware.v1.support.exception.FileSaveFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Primary
@Service
@RequiredArgsConstructor
public class LocalFileService implements FileService {

    private final FileRepository fileRepository;
    private static final String UPLOAD_DIR = "/";

    @Override
    public String append(AddFileForm form) {
        Assert.notNull(form, "파일 저장 정보가 존재하지 않습니다.");

        try {
            String originalFilename = form.getFile().getOriginalFilename();
            String uuid = UUID.randomUUID().toString();
            String newFileName = uuid + "_" + originalFilename;

            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(UPLOAD_DIR + newFileName);
            form.getFile().transferTo(dest);

            FileMaster savedFile = fileRepository.save(FileMaster.create(
                    form.getTargetId(),
                    form.getTargetType(),
                    form.getUsageType(),
                    dest.getAbsolutePath(),
                    form.getFile()
            ));

            return savedFile.getDestination();

        }
        catch (IOException e) {
            throw new FileSaveFailedException();
        }
    }

    @Override
    public void remove(Long targetId, TableType tableType) {
        // TODO
    }
}
