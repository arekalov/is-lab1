package com.arekalov.islab1.service

import com.arekalov.islab1.dto.CreateHouseRequest
import com.arekalov.islab1.dto.HouseDTO
import com.arekalov.islab1.entity.House
import com.arekalov.islab1.repository.HouseRepository
import jakarta.ejb.Stateless
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import kotlinx.coroutines.runBlocking

/**
 * Сервис для работы с домами
 */
@Stateless
@Transactional
open class HouseService {
    
    @Inject
    private lateinit var houseRepository: HouseRepository
    
    /**
     * Получить все дома
     */
    open fun getAllHouses(): List<HouseDTO> = runBlocking {
        houseRepository.findAll().map { it.toDTO() }
    }
    
    /**
     * Получить дом по ID
     */
    open fun getHouseById(id: Long): HouseDTO? = runBlocking {
        houseRepository.findById(id)?.toDTO()
    }
    
    /**
     * Создать новый дом
     */
    open fun createHouse(request: CreateHouseRequest): HouseDTO = runBlocking {
        val house = House(
            name = request.name,
            year = request.year,
            numberOfFlatsOnFloor = request.numberOfFlatsOnFloor
        )
        
        val savedHouse = houseRepository.save(house)
        savedHouse.toDTO()
    }
    
    /**
     * Обновить дом
     */
    open fun updateHouse(id: Long, request: CreateHouseRequest): HouseDTO? = runBlocking {
        val existingHouse = houseRepository.findById(id) ?: return@runBlocking null
        
        val updatedHouse = existingHouse.copy(
            name = request.name,
            year = request.year,
            numberOfFlatsOnFloor = request.numberOfFlatsOnFloor
        )
        
        val savedHouse = houseRepository.save(updatedHouse)
        savedHouse.toDTO()
    }
    
    /**
     * Удалить дом
     */
    open fun deleteHouse(id: Long): Boolean = runBlocking {
        houseRepository.deleteById(id)
    }
    
    /**
     * Поиск домов по названию
     */
    open fun findByNameContaining(substring: String): List<HouseDTO> = runBlocking {
        houseRepository.findByNameContaining(substring).map { it.toDTO() }
    }
}

/**
 * Расширение для конвертации в DTO
 */
private fun House.toDTO(): HouseDTO = HouseDTO(
    id = this.id,
    name = this.name,
    year = this.year,
    numberOfFlatsOnFloor = this.numberOfFlatsOnFloor
)
