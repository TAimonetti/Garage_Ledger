package com.garageledger.data.export

import com.garageledger.data.local.ExpenseRecordEntity
import com.garageledger.data.local.ExpenseRecordTypeCrossRef
import com.garageledger.data.local.ExpenseTypeEntity
import com.garageledger.data.local.FillUpRecordEntity
import com.garageledger.data.local.FuelTypeEntity
import com.garageledger.data.local.RecordAttachmentEntity
import com.garageledger.data.local.ServiceRecordEntity
import com.garageledger.data.local.ServiceRecordTypeCrossRef
import com.garageledger.data.local.ServiceReminderEntity
import com.garageledger.data.local.ServiceTypeEntity
import com.garageledger.data.local.TripRecordEntity
import com.garageledger.data.local.TripTypeEntity
import com.garageledger.data.local.VehicleEntity
import com.garageledger.data.local.VehiclePartEntity
import com.garageledger.domain.model.AppPreferenceSnapshot

data class ExportSnapshot(
    val preferences: AppPreferenceSnapshot,
    val vehicles: List<VehicleEntity>,
    val vehicleParts: List<VehiclePartEntity>,
    val fuelTypes: List<FuelTypeEntity>,
    val serviceTypes: List<ServiceTypeEntity>,
    val expenseTypes: List<ExpenseTypeEntity>,
    val tripTypes: List<TripTypeEntity>,
    val serviceReminders: List<ServiceReminderEntity>,
    val fillUpRecords: List<FillUpRecordEntity>,
    val serviceRecords: List<ServiceRecordEntity>,
    val serviceRecordTypes: List<ServiceRecordTypeCrossRef>,
    val expenseRecords: List<ExpenseRecordEntity>,
    val expenseRecordTypes: List<ExpenseRecordTypeCrossRef>,
    val tripRecords: List<TripRecordEntity>,
    val attachments: List<RecordAttachmentEntity>,
)
