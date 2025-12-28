package com.arekalov.islab1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа с presigned URL для скачивания файла
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadLinkResponseDTO {
    
    /**
     * Presigned URL для прямого скачивания из MinIO
     */
    private String downloadUrl;
    
    /**
     * Время действия ссылки в секундах
     */
    private int expiresIn;
}

