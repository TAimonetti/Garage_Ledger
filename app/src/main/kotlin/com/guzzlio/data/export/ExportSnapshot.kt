package com.guzzlio.data.export

import com.guzzlio.data.local.ExpenseRecordEntity
import com.guzzlio.data.local.ExpenseRecordTypeCrossRef
import com.guzzlio.data.local.ExpenseTypeEntity
import com.guzzlio.data.local.FillUpRecordEntity
import com.guzzlio.data.local.FuelTypeEntity
import com.guzzlio.data.local.RecordAttachmentEntity
import com.guzzlio.data.local.ServiceRecordEntity
import com.guzzlio.data.local.ServiceRecordTypeCrossRef
import com.guzzlio.data.local.ServiceReminderEntity
import com.guzzlio.data.local.ServiceTypeEntity
import com.guzzlio.data.local.TripRecordEntity
import com.guzzlio.data.local.TripTypeEntity
import com.guzzlio.data.local.VehicleEntity
import com.guzzlio.data.local.VehiclePartEntity
import com.guzzlio.domain.model.AppPreferenceSnapshot

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
