package com.example.me.code.individual_assignment.service;

import com.example.me.code.individual_assignment.exceptions.ByteConversionException;
import com.example.me.code.individual_assignment.exceptions.FailedImageUploadException;
import com.example.me.code.individual_assignment.exceptions.FolderNotFoundException;
import com.example.me.code.individual_assignment.exceptions.ImageSizeTooLargeException;
import com.example.me.code.individual_assignment.model.Folder;
import com.example.me.code.individual_assignment.model.Image;
import com.example.me.code.individual_assignment.model.User;
import com.example.me.code.individual_assignment.repository.FolderRepository;
import com.example.me.code.individual_assignment.repository.ImageRepository;
import com.example.me.code.individual_assignment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class ImageService {

    private ImageRepository imageRepository;
    private FolderRepository folderRepository;
    private UserRepository userRepository;

    @Autowired
    public ImageService(ImageRepository imageRepository, FolderRepository folderRepository, UserRepository userRepository) {
        this.imageRepository = imageRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
    }

    public String uploadImage(MultipartFile file, int userId, int folderId)
            throws ImageSizeTooLargeException, ByteConversionException, FailedImageUploadException {
        try {
            if (isValidImageSize(file)) {
                Image image;
                Folder folder;
                try {
                    folder = findFolder(userId, folderId);
                    image = new Image(file, folder);
                } catch (Exception e) {
                    throw new ByteConversionException("Failed to convert image to bytes");
                }
                folder.getImages().add(image);
                imageRepository.save(image);
                return "uploaded successfully";

            } else {
                throw new ImageSizeTooLargeException("File size exceeds the allowed limit of 2 megabytes");
            }
        } catch (Exception e) {
            throw new FailedImageUploadException("Could not upload image: " + e.getMessage());
        }
    }

    public boolean isValidImageSize(MultipartFile file) throws ImageSizeTooLargeException {
        long imageSize = file.getSize();

        long maxSize = 2 * 1024 * 1024; // 2mb

        return imageSize <= maxSize;
    }

    private Folder findFolder(int userId, int folderId) throws FolderNotFoundException {
        boolean isFolderPresent = folderRepository.existsByUserIdAndFolderId(userId, folderId);

        if(isFolderPresent) {
            Optional<Folder> folder = folderRepository.findById(folderId);
            return folder.get();
        }
        else {
            throw new FolderNotFoundException("Folder not found for folderId: " + folderId);
        }
    }

    public ResponseEntity<String> deleteImage(int userId, int imageId) {
        boolean isImageExisting = doesImageExist(imageId);
        boolean doesImageBelongToUser = imageRepository.existsByFolderUserIdAndImageId(userId, imageId);

        if (isImageExisting && doesImageBelongToUser) {
            imageRepository.deleteById(imageId);
            return ResponseEntity.ok("Image deleted");
        } else {
            throw new IllegalArgumentException("Image could not be deleted. The image either does not belong to the user who's trying to delete the image, or the image does not exist.");
        }
    }

    public boolean doesImageExist(int imageId) {
        return imageRepository.existsById(imageId);
    }
}
