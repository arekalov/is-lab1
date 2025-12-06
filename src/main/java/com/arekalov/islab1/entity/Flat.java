package com.arekalov.islab1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Квартира (Entity)
 */
@Entity
@Table(name = "flats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название не может быть пустым")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Координаты не могут быть null")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;

    @NotNull(message = "Дата создания не может быть null")
    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @NotNull(message = "Площадь не может быть null")
    @Positive(message = "Площадь должна быть больше 0")
    @Column(nullable = false)
    private Long area;

    @NotNull(message = "Цена не может быть null")
    @Positive(message = "Цена должна быть больше 0")
    @Max(value = 581208244, message = "Максимальная цена: 581208244")
    @Column(nullable = false)
    private Long price;

    @Column(name = "balcony")
    private Boolean balcony;

    @NotNull(message = "Время до метро не может быть null")
    @Positive(message = "Время до метро должно быть больше 0")
    @Column(name = "time_to_metro_on_foot", nullable = false)
    private Long timeToMetroOnFoot;

    @NotNull(message = "Количество комнат не может быть null")
    @Min(value = 1, message = "Количество комнат должно быть больше 0")
    @Max(value = 13, message = "Максимальное количество комнат: 13")
    @Column(name = "number_of_rooms", nullable = false)
    private Integer numberOfRooms;

    @NotNull(message = "Жилая площадь не может быть null")
    @Positive(message = "Жилая площадь должна быть больше 0")
    @Column(name = "living_space", nullable = false)
    private Long livingSpace;

    @NotNull(message = "Тип мебели не может быть null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Furnish furnish;

    @NotNull(message = "Вид из окна не может быть null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private View view;

    @NotNull(message = "Этаж не может быть null")
    @Positive(message = "Этаж должен быть больше 0")
    @Column(nullable = false)
    private Integer floor;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "house_id")
    private House house;
}