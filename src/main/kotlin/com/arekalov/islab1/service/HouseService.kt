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
class HouseService @Inject constructor(
    private val houseRepository: HouseRepository
) {
    
    /**
     * Получить все дома
     */
    fun getAllHouses(): List<HouseDTO> = runBlocking {
        houseRepository.findAll().map { it.toDTO() }
    }
    
    /**
     * Получить дом по ID
     */
    fun getHouseById(id: Long): HouseDTO? = runBlocking {
        houseRepository.findById(id)?.toDTO()
    }
    
    /**
     * Создать новый дом
     */
    fun createHouse(request: CreateHouseRequest): HouseDTO = runBlocking {
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
    fun updateHouse(id: Long, request: CreateHouseRequest): HouseDTO? = runBlocking {
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
    fun deleteHouse(id: Long): Boolean = runBlocking {
        houseRepository.deleteById(id)
    }
    
    /**
     * Поиск домов по названию
     */
    fun findByNameContaining(substring: String): List<HouseDTO> = runBlocking {
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
