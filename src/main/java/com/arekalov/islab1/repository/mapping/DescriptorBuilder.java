package com.arekalov.islab1.repository.mapping;

import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.mappings.DirectToFieldMapping;
import org.eclipse.persistence.mappings.OneToOneMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import com.arekalov.islab1.pojo.*;

/**
 * Утилитарный класс для создания дескрипторов EclipseLink
 * Упрощает создание маппинга без дублирования кода
 */
public class DescriptorBuilder {
    
    /**
     * Создает дескриптор для House
     */
    public static RelationalDescriptor buildHouseDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(House.class);
        descriptor.setTableName("houses");
        
        // ID с автогенерацией
        addIdMapping(descriptor, "id", "houses.id");
        descriptor.setSequenceNumberFieldName("houses.id");
        descriptor.setSequenceNumberName("houses_id_seq");
        
        // Простые поля
        addDirectMapping(descriptor, "name", "houses.name", String.class);
        addDirectMapping(descriptor, "year", "houses.year", Integer.class);
        addDirectMapping(descriptor, "numberOfFlatsOnFloor", "houses.number_of_flats_on_floor", Integer.class);
        
        // Primary key
        descriptor.addPrimaryKeyFieldName("houses.id");
        
        return descriptor;
    }
    
    /**
     * Создает дескриптор для Coordinates
     */
    public static RelationalDescriptor buildCoordinatesDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(Coordinates.class);
        descriptor.setTableName("coordinates");
        
        // ID с автогенерацией
        addIdMapping(descriptor, "id", "coordinates.id");
        descriptor.setSequenceNumberFieldName("coordinates.id");
        descriptor.setSequenceNumberName("coordinates_id_seq");
        
        // Простые поля
        addDirectMapping(descriptor, "x", "coordinates.x", Integer.class);
        addDirectMapping(descriptor, "y", "coordinates.y", Integer.class);
        
        // Primary key
        descriptor.addPrimaryKeyFieldName("coordinates.id");
        
        return descriptor;
    }
    
    /**
     * Создает дескриптор для Flat
     */
    public static RelationalDescriptor buildFlatDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(Flat.class);
        descriptor.setTableName("flats");
        
        // ID с автогенерацией
        addIdMapping(descriptor, "id", "flats.id");
        descriptor.setSequenceNumberFieldName("flats.id");
        descriptor.setSequenceNumberName("flats_id_seq");
        
        // Простые поля
        addDirectMapping(descriptor, "name", "flats.name", String.class);
        addDirectMapping(descriptor, "area", "flats.area", Long.class);
        addDirectMapping(descriptor, "price", "flats.price", Long.class);
        addDirectMapping(descriptor, "balcony", "flats.balcony", Boolean.class);
        addDirectMapping(descriptor, "timeToMetroOnFoot", "flats.time_to_metro_on_foot", Long.class);
        addDirectMapping(descriptor, "numberOfRooms", "flats.number_of_rooms", Integer.class);
        addDirectMapping(descriptor, "livingSpace", "flats.living_space", Long.class);
        
        // Enum поля с конвертерами
        addEnumMapping(descriptor, "furnish", "flats.furnish", Furnish.class);
        addEnumMapping(descriptor, "view", "flats.view", View.class);
        
        // ZonedDateTime с конвертером
        addZonedDateTimeMapping(descriptor, "creationDate", "flats.creation_date");
        
        // Связи
        addOneToOneMapping(descriptor, "coordinates", Coordinates.class, "flats.coordinates_id", "coordinates.id", false);
        addOneToOneMapping(descriptor, "house", House.class, "flats.house_id", "houses.id", true);
        
        // Primary key
        descriptor.addPrimaryKeyFieldName("flats.id");
        
        return descriptor;
    }
    
    /**
     * Добавляет ID маппинг
     */
    private static void addIdMapping(RelationalDescriptor descriptor, String attributeName, String fieldName) {
        DirectToFieldMapping mapping = new DirectToFieldMapping();
        mapping.setAttributeName(attributeName);
        mapping.setFieldName(fieldName);
        mapping.setIsReadOnly(false);
        descriptor.addMapping(mapping);
    }
    
    /**
     * Добавляет простой маппинг поля
     */
    private static void addDirectMapping(RelationalDescriptor descriptor, String attributeName, 
                                       String fieldName, Class<?> attributeClass) {
        DirectToFieldMapping mapping = new DirectToFieldMapping();
        mapping.setAttributeName(attributeName);
        mapping.setFieldName(fieldName);
        mapping.setAttributeClassification(attributeClass);
        descriptor.addMapping(mapping);
    }
    
    /**
     * Добавляет маппинг для enum с конвертером
     */
    private static void addEnumMapping(RelationalDescriptor descriptor, String attributeName, 
                                     String fieldName, Class<? extends Enum<?>> enumClass) {
        DirectToFieldMapping mapping = new DirectToFieldMapping();
        mapping.setAttributeName(attributeName);
        mapping.setFieldName(fieldName);
        mapping.setAttributeClassification(enumClass);
        
        // Универсальный конвертер для enum
        mapping.setConverter(new Converter() {
            @Override
            public Object convertObjectValueToDataValue(Object objectValue, org.eclipse.persistence.sessions.Session session) {
                if (objectValue == null) return null;
                if (objectValue instanceof Enum) {
                    return ((Enum<?>) objectValue).name();
                }
                return objectValue;
            }
            
            @Override
            public Object convertDataValueToObjectValue(Object dataValue, org.eclipse.persistence.sessions.Session session) {
                if (dataValue == null) return null;
                if (dataValue instanceof String) {
                    try {
                        // Используем рефлексию для безопасного вызова valueOf
                        String enumName = (String) dataValue;
                        Object[] enumConstants = enumClass.getEnumConstants();
                        for (Object enumConstant : enumConstants) {
                            if (((Enum<?>) enumConstant).name().equals(enumName)) {
                                return enumConstant;
                            }
                        }
                        throw new IllegalArgumentException("No enum constant " + enumClass.getSimpleName() + "." + enumName);
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot convert " + dataValue + " to " + enumClass.getSimpleName(), e);
                    }
                }
                return dataValue;
            }
            
            @Override
            public boolean isMutable() {
                return false;
            }
            
            @Override
            public void initialize(org.eclipse.persistence.mappings.DatabaseMapping mapping, org.eclipse.persistence.sessions.Session session) {
                // Инициализация не требуется
            }
        });
        
        descriptor.addMapping(mapping);
    }
    
    /**
     * Добавляет маппинг для ZonedDateTime
     */
    private static void addZonedDateTimeMapping(RelationalDescriptor descriptor, String attributeName, String fieldName) {
        DirectToFieldMapping mapping = new DirectToFieldMapping();
        mapping.setAttributeName(attributeName);
        mapping.setFieldName(fieldName);
        mapping.setAttributeClassification(java.time.ZonedDateTime.class);
        
        // Конвертер для ZonedDateTime
        mapping.setConverter(new Converter() {
            @Override
            public Object convertObjectValueToDataValue(Object objectValue, org.eclipse.persistence.sessions.Session session) {
                if (objectValue == null) return null;
                if (objectValue instanceof java.time.ZonedDateTime) {
                    return java.sql.Timestamp.valueOf(((java.time.ZonedDateTime) objectValue).toLocalDateTime());
                }
                return objectValue;
            }
            
            @Override
            public Object convertDataValueToObjectValue(Object dataValue, org.eclipse.persistence.sessions.Session session) {
                if (dataValue == null) return null;
                if (dataValue instanceof java.sql.Timestamp) {
                    return ((java.sql.Timestamp) dataValue).toLocalDateTime().atZone(java.time.ZoneId.systemDefault());
                }
                return dataValue;
            }
            
            @Override
            public boolean isMutable() {
                return false;
            }
            
            @Override
            public void initialize(org.eclipse.persistence.mappings.DatabaseMapping mapping, org.eclipse.persistence.sessions.Session session) {
                // Инициализация не требуется
            }
        });
        
        descriptor.addMapping(mapping);
    }
    
    /**
     * Добавляет OneToOne маппинг
     */
    private static void addOneToOneMapping(RelationalDescriptor descriptor, String attributeName, 
                                         Class<?> referenceClass, String foreignKey, String targetKey, boolean optional) {
        OneToOneMapping mapping = new OneToOneMapping();
        mapping.setAttributeName(attributeName);
        mapping.setReferenceClass(referenceClass);
        mapping.addForeignKeyFieldName(foreignKey, targetKey);
        mapping.setIsOptional(optional);
        mapping.dontUseIndirection(); // Отключаем ленивую загрузку
        descriptor.addMapping(mapping);
    }
}
