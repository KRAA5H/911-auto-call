package org.example.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * Data class representing a call record.
 */
@Serializable
data class CallRecordData(
    val callId: String,
    val callSid: String? = null,
    val callerPhone: String? = null,
    val startTime: String, // ISO-8601 format
    val endTime: String? = null,
    val duration: Int? = null,
    val status: String,
    val emergencyType: String? = null,
    val location: String? = null,
    val coordinates: String? = null,
    val severity: Int? = null,
    val dispatchStatus: String? = null,
    val audioFileUrl: String? = null,
    val transcriptFileUrl: String? = null,
    val metadata: String? = null
)

/**
 * Repository for managing call records in the database.
 * Provides CRUD operations for emergency call data.
 */
object CallRepository {
    
    /**
     * Create a new call record in the database.
     */
    fun createCall(
        callId: String,
        startTime: LocalDateTime = LocalDateTime.now(),
        status: String = "initiated"
    ): String = transaction {
        CallRecords.insert {
            it[CallRecords.callId] = callId
            it[CallRecords.startTime] = startTime
            it[CallRecords.status] = status
        }
        callId
    }
    
    /**
     * Update an existing call record.
     */
    fun updateCall(
        callId: String,
        callSid: String? = null,
        callerPhone: String? = null,
        endTime: LocalDateTime? = null,
        duration: Int? = null,
        status: String? = null,
        emergencyType: String? = null,
        location: String? = null,
        coordinates: String? = null,
        severity: Int? = null,
        dispatchStatus: String? = null,
        audioFileUrl: String? = null,
        transcriptFileUrl: String? = null,
        metadata: String? = null
    ): Int = transaction {
        CallRecords.update({ CallRecords.callId eq callId }) {
            callSid?.let { value -> it[CallRecords.callSid] = value }
            callerPhone?.let { value -> it[CallRecords.callerPhone] = value }
            endTime?.let { value -> it[CallRecords.endTime] = value }
            duration?.let { value -> it[CallRecords.duration] = value }
            status?.let { value -> it[CallRecords.status] = value }
            emergencyType?.let { value -> it[CallRecords.emergencyType] = value }
            location?.let { value -> it[CallRecords.location] = value }
            coordinates?.let { value -> it[CallRecords.coordinates] = value }
            severity?.let { value -> it[CallRecords.severity] = value }
            dispatchStatus?.let { value -> it[CallRecords.dispatchStatus] = value }
            audioFileUrl?.let { value -> it[CallRecords.audioFileUrl] = value }
            transcriptFileUrl?.let { value -> it[CallRecords.transcriptFileUrl] = value }
            metadata?.let { value -> it[CallRecords.metadata] = value }
        }
    }
    
    /**
     * Get a call record by ID.
     */
    fun getCall(callId: String): ResultRow? = transaction {
        CallRecords.select { CallRecords.callId eq callId }.firstOrNull()
    }
    
    /**
     * Get all call records.
     */
    fun getAllCalls(): List<ResultRow> = transaction {
        CallRecords.selectAll().toList()
    }
}

/**
 * Repository for managing transcripts in the database.
 */
object TranscriptRepository {
    
    /**
     * Add a transcript segment to the database.
     */
    fun addTranscript(
        callId: String,
        speaker: String,
        text: String,
        timestamp: LocalDateTime = LocalDateTime.now(),
        confidence: Double? = null,
        sentiment: String? = null
    ): Int = transaction {
        Transcripts.insert {
            it[Transcripts.callId] = callId
            it[Transcripts.timestamp] = timestamp
            it[Transcripts.speaker] = speaker
            it[Transcripts.text] = text
            it[Transcripts.confidence] = confidence
            it[Transcripts.sentiment] = sentiment
        } get Transcripts.id
    }
    
    /**
     * Get all transcript segments for a call.
     */
    fun getTranscripts(callId: String): List<ResultRow> = transaction {
        Transcripts.select { Transcripts.callId eq callId }
            .orderBy(Transcripts.timestamp)
            .toList()
    }
}

/**
 * Repository for managing extracted entities.
 */
object EntityRepository {
    
    /**
     * Add an extracted entity to the database.
     */
    fun addEntity(
        callId: String,
        entityType: String,
        entityValue: String,
        confidence: Double? = null,
        extractedAt: LocalDateTime = LocalDateTime.now()
    ): Int = transaction {
        ExtractedEntities.insert {
            it[ExtractedEntities.callId] = callId
            it[ExtractedEntities.entityType] = entityType
            it[ExtractedEntities.entityValue] = entityValue
            it[ExtractedEntities.confidence] = confidence
            it[ExtractedEntities.extractedAt] = extractedAt
        } get ExtractedEntities.id
    }
    
    /**
     * Get all entities for a call.
     */
    fun getEntities(callId: String): List<ResultRow> = transaction {
        ExtractedEntities.select { ExtractedEntities.callId eq callId }.toList()
    }
    
    /**
     * Get entities by type for a call.
     */
    fun getEntitiesByType(callId: String, entityType: String): List<ResultRow> = transaction {
        ExtractedEntities.select { 
            (ExtractedEntities.callId eq callId) and (ExtractedEntities.entityType eq entityType)
        }.toList()
    }
}
