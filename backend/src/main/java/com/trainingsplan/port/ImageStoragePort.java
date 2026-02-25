package com.trainingsplan.port;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStoragePort {

    String store(MultipartFile file);

    Resource load(String filename);

    void delete(String filename);
}
