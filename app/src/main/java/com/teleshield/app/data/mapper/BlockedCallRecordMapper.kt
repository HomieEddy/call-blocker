package com.teleshield.app.data.mapper

import com.teleshield.app.data.db.BlockedCallRecordEntity
import com.teleshield.domain.BlockedCallRecord

object BlockedCallRecordMapper {

    fun toDomain(entity: BlockedCallRecordEntity): BlockedCallRecord = BlockedCallRecord(
        id = entity.id,
        callerIdentifier = entity.callerIdentifier,
        timestamp = entity.timestamp,
        matchedRuleId = entity.matchedRuleId,
        matchedPatternSnapshot = entity.matchedPatternSnapshot,
        matchedLabelSnapshot = entity.matchedLabelSnapshot,
    )

    fun toEntity(record: BlockedCallRecord): BlockedCallRecordEntity = BlockedCallRecordEntity(
        id = record.id,
        callerIdentifier = record.callerIdentifier,
        timestamp = record.timestamp,
        matchedRuleId = record.matchedRuleId,
        matchedPatternSnapshot = record.matchedPatternSnapshot,
        matchedLabelSnapshot = record.matchedLabelSnapshot,
    )
}
