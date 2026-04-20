package com.garageledger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        VehicleEntity::class,
        VehiclePartEntity::class,
        FuelTypeEntity::class,
        ServiceTypeEntity::class,
        ExpenseTypeEntity::class,
        TripTypeEntity::class,
        ServiceReminderEntity::class,
        FillUpRecordEntity::class,
        ServiceRecordEntity::class,
        ServiceRecordTypeCrossRef::class,
        ExpenseRecordEntity::class,
        ExpenseRecordTypeCrossRef::class,
        TripRecordEntity::class,
        RecordAttachmentEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class GarageDatabase : RoomDatabase() {
    abstract fun garageDao(): GarageDao
}
