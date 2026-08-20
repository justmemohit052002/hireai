package com.vionsys.hireai.candidate.storage;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file) throws IOException;

    Resource loadAsResource(String filePath) throws IOException;

    void delete(String filePath) throws IOException;
}
