package com.arekalov.islab1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * История операций импорта
 * Хранит информацию ТОЛЬКО об успешных операциях массового добавления объектов
 */
@Entity
@Table(name = "import_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Время выполнения операции
     */
    @NotNull(message = "Время операции не может быть null")
    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;
    
    /**
     * Количество успешно импортированных объектов
     */
    @NotNull(message = "Количество объектов не может быть null")
    @Positive(message = "Количество объектов должно быть больше 0")
    @Column(name = "objects_count", nullable = false)
    private Integer objectsCount;
    
    /**
     * Описание изменений в формате JSON
     * Содержит массив объектов с информацией о созданных сущностях
     */
    @Column(name = "changes_description", columnDefinition = "TEXT")
    private String changesDescription;
}

