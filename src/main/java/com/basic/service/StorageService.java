package com.basic.service;

import com.basic.app.StorageProperties;
import com.basic.entity.User;
import com.basic.exception.StorageException;
import com.basic.exception.StorageFileNotFoundException;
import com.basic.interfaceService.StorageServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class StorageService implements StorageServiceInterface {

    private final Path rootLocation;

    @Autowired
    public StorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void init() {

        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }

    }

    @Override
    public void store(MultipartFile file) {

        String filename = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + filename);
            }

            if (filename.contains("..")) {
                // For security check purpose
                throw new StorageException("Cannot store file with relative path outside current directory " + filename);
            }

            // when uploading new file it will replace existing one
            Files.copy(file.getInputStream(), this.rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new StorageException("Failed to store file "+filename, e);
        }


    }

    @Override
    public Stream<Path> loadAll() {

        try {
            return Files.walk(this.rootLocation, 1).filter(path -> !path.equals(this.rootLocation)).map(path -> this.rootLocation.relativize(path));
        }
        catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {

        Path file = load(filename);
        try {
            Resource resource = new UrlResource((file.toUri()));

            if (resource.exists() || resource.isReadable())
            {
                return resource;
            }

            else
            {
                throw new StorageFileNotFoundException("Could not read file: " + filename);
            }
        }
        catch (MalformedURLException e) {

            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void storeProfilePhoto(MultipartFile file, User user) {

        String filename = user.getEmail();
        try
        {
            if (file.isEmpty())
            {

                throw new StorageException("Failed to store empty file " + filename);
            }
            if (filename.contains(".."))
            {
                // This is a security check
                throw new StorageException("Cannot store file with relative path outside current directory " + filename);
            }
            Files.copy(file.getInputStream(), this.rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new StorageException("Failed to store file " + filename, e);
        }

        //TODO refactor duplicated code


    }
}
