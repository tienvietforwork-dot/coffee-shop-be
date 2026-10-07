package com.coffeeshop.service;

import com.coffeeshop.dto.response.ImageResponse;
import com.coffeeshop.entity.Image;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CoffeeRepository;
import com.coffeeshop.repository.ImageRepository;
import com.coffeeshop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final long MAX_BYTES = 3 * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ImageRepository imageRepository;
    private final CoffeeRepository coffeeRepository;

    @Transactional
    public ImageResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Chưa chọn ảnh");
        if (!TYPES.contains(file.getContentType())) throw new BadRequestException("Chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF");
        if (file.getSize() > MAX_BYTES) throw new BadRequestException("Ảnh quá lớn (tối đa 3MB)");
        try {
            Image image = imageRepository.save(Image.builder()
                    .contentType(file.getContentType())
                    .sizeBytes((int) file.getSize())
                    .data(file.getBytes())
                    .build());
            return new ImageResponse(image.getId(), Image.URL_PREFIX + image.getId(), image.getContentType(), image.getSizeBytes());
        } catch (IOException e) {
            throw new BadRequestException("Không đọc được file ảnh");
        }
    }

    /** Fails unless the image exists (not deleted). */
    public void requireExists(Long id) {
        if (!imageRepository.existsById(id)) throw new BadRequestException("Ảnh không tồn tại: " + id);
    }

    /** Soft-deletes a replaced image unless another coffee still uses it. */
    public void discardIfUnused(Long id, Long exceptCoffeeId) {
        if (!coffeeRepository.existsByImageIdAndIdNot(id, exceptCoffeeId)) imageRepository.softDelete(id, CurrentUser.username());
    }

    @Transactional(readOnly = true)
    public Image get(Long id) {
        return imageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh: " + id));
    }
}
