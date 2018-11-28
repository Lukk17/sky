package com.basic.interfaceService;

import com.basic.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageServiceInterface {

    /**
     * Create directory in specify by storage properties path.
     *
     */
    void init();

    /**
     * Copy file from input stream to directory created in init method.
     * If file with same name already exist it will REPLACE it.
     *
     * @param file      File which will be copied.
     */
    void store(MultipartFile file);

    /**
     *
     * @return      Stream of uploaded files paths.
     */
    Stream<Path> loadAll();

    /**
     * Return path of given filename.
     *
     * @param filename
     * @return          filename path.
     */
    Path load(String filename);

    /**
     *
     * @param filename
     * @return resource     Given file is loaded as Resource.
     */
    Resource loadAsResource(String filename);

    /**
     * Upload photo which will be given user's profile photo.
     *
     * @param file      Photo to upload.
     * @param user      User which profile photo will be changed.
     */
    void storeProfilePhoto(MultipartFile file, User user);
}
