package com.arekalov.islab1.service

import com.arekalov.islab1.dto.*
import com.arekalov.islab1.entity.*
import com.arekalov.islab1.repository.FlatRepository
import com.arekalov.islab1.repository.HouseRepository
import jakarta.ejb.Stateless
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import kotlinx.coroutines.runBlocking

/**
 * Сервис для работы с квартирами
 */
@Stateless
@Transactional
open class FlatService {
    
    @Inject
    private lateinit var flatRepository: FlatRepository
    
    @Inject
    private lateinit var houseRepository: HouseRepository
    
    /**
     * Получить все квартиры с пагинацией
     */
    open fun getAllFlats(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "id",
        nameFilter: String? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null,
        hasBalcony: Boolean? = null,
        minRooms: Int? = null,
        maxRooms: Int? = null
    ): PagedResponse<FlatDTO> = runBlocking {
        
        val flats = if (nameFilter != null || minPrice != null || maxPrice != null || 
                       hasBalcony != null || minRooms != null || maxRooms != null) {
            flatRepository.findWithFilters(
                nameFilter = nameFilter,
                minPrice = minPrice,
                maxPrice = maxPrice,
                hasBalcony = hasBalcony,
                minRooms = minRooms,
                maxRooms = maxRooms,
                page = page,
                size = size,
                sortBy = sortBy
            )
        } else {
            flatRepository.findAll(page, size, sortBy)
        }
        
        val totalElements = flatRepository.count()
        val flatDTOs = flats.map { it.toDTO() }
        
        PagedResponse.of(flatDTOs, page, size, totalElements)
    }
    
    /**
     * Получить квартиру по ID
     */
    open fun getFlatById(id: Long): FlatDTO? = runBlocking {
        flatRepository.findById(id)?.toDTO()
    }
    
    /**
     * Создать новую квартиру
     */
    open fun createFlat(request: CreateFlatRequest): FlatDTO = runBlocking {
        val house = request.houseId?.let { houseRepository.findById(it) }
        
        val coordinates = Coordinates(
            x = request.coordinates.x,
            y = request.coordinates.y
        )
        
        val flat = Flat(
            name = request.name,
            coordinates = coordinates,
            area = request.area,
            price = request.price,
            balcony = request.balcony,
            timeToMetroOnFoot = request.timeToMetroOnFoot,
            numberOfRooms = request.numberOfRooms,
            livingSpace = request.livingSpace,
            furnish = request.furnish,
            view = request.view,
            house = house
        )
        
        val savedFlat = flatRepository.save(flat)
        val flatDTO = savedFlat.toDTO()
        
        flatDTO
    }
    
    /**
     * Обновить квартиру
     */
    open fun updateFlat(id: Long, request: CreateFlatRequest): FlatDTO? = runBlocking {
        val existingFlat = flatRepository.findById(id) ?: return@runBlocking null
        
        val house = request.houseId?.let { houseRepository.findById(it) }
        
        val updatedCoordinates = existingFlat.coordinates.copy(
            x = request.coordinates.x,
            y = request.coordinates.y
        )
        
        val updatedFlat = existingFlat.copy(
            name = request.name,
            coordinates = updatedCoordinates,
            area = request.area,
            price = request.price,
            balcony = request.balcony,
            timeToMetroOnFoot = request.timeToMetroOnFoot,
            numberOfRooms = request.numberOfRooms,
            livingSpace = request.livingSpace,
            furnish = request.furnish,
            view = request.view,
            house = house
        )
        
        val savedFlat = flatRepository.save(updatedFlat)
        val flatDTO = savedFlat.toDTO()
        
        flatDTO
    }
    
    /**
     * Удалить квартиру
     */
    open fun deleteFlat(id: Long): Boolean = runBlocking {
        val deleted = flatRepository.deleteById(id)
        deleted
    }
    
    /**
     * Специальные операции
     */
    
    /**
     * Количество квартир с комнатами больше заданного
     */
    open fun countByRoomsGreaterThan(minRooms: Int): Long = runBlocking {
        flatRepository.countByNumberOfRoomsGreaterThan(minRooms)
    }
    
    /**
     * Квартиры с названием содержащим подстроку
     */
    open fun findByNameContaining(substring: String): List<FlatDTO> = runBlocking {
        flatRepository.findByNameContaining(substring).map { it.toDTO() }
    }
    
    /**
     * Квартиры с жилой площадью меньше заданной
     */
    open fun findByLivingSpaceLessThan(maxSpace: Long): List<FlatDTO> = runBlocking {
        flatRepository.findByLivingSpaceLessThan(maxSpace).map { it.toDTO() }
    }
    
    /**
     * Самая дешевая квартира с балконом
     */
    open fun findCheapestWithBalcony(): FlatDTO? = runBlocking {
        flatRepository.findCheapestWithBalcony()?.toDTO()
    }
    
    /**
     * Квартиры отсортированные по времени до метро
     */
    open fun findAllSortedByMetroTime(): List<FlatDTO> = runBlocking {
        flatRepository.findAllSortedByMetroTime().map { it.toDTO() }
    }
}

/**
 * Расширения для конвертации в DTO
 */
private fun Flat.toDTO(): FlatDTO = FlatDTO(
    id = this.id,
    name = this.name,
    coordinates = this.coordinates.toDTO(),
    creationDate = this.creationDate,
    area = this.area,
    price = this.price,
    balcony = this.balcony,
    timeToMetroOnFoot = this.timeToMetroOnFoot,
    numberOfRooms = this.numberOfRooms,
    livingSpace = this.livingSpace,
    furnish = this.furnish,
    view = this.view,
    house = this.house?.toDTO()
)

private fun Coordinates.toDTO(): CoordinatesDTO = CoordinatesDTO(
    id = this.id,
    x = this.x,
    y = this.y
)

private fun House.toDTO(): HouseDTO = HouseDTO(
    id = this.id,
    name = this.name,
    year = this.year,
    numberOfFlatsOnFloor = this.numberOfFlatsOnFloor
)
