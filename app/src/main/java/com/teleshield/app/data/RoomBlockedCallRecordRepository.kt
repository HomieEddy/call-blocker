package com.teleshield.app.data

import com.teleshield.app.data.db.BlockedCallRecordDao
import com.teleshield.app.data.mapper.BlockedCallRecordMapper
import com.teleshield.domain.BlockedCallRecord
import com.teleshield.ports.BlockedCallRecordRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class RoomBlockedCallRecordRepository @Inject constructor(
    private val dao: BlockedCallRecordDao,
) : BlockedCallRecordRepository {

    override fun getAllRecords(limit: Int, offset: Int): List<BlockedCallRecord> =
        runBlocking { dao.getAll(limit, offset).map(BlockedCallRecordMapper::toDomain) }

    override fun save(record: BlockedCallRecord): String {
        runBlocking { dao.insert(BlockedCallRecordMapper.toEntity(record)) }
        return record.id
    }

    override fun delete(id: String): Boolean = runBlocking { dao.deleteById(id) } > 0

    override fun purgeOlderThan(cutoffTimestamp: Long): Int =
        runBlocking { dao.purgeOlderThan(cutoffTimestamp) }
}
