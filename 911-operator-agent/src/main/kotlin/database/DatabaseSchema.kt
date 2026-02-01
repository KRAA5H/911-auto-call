package database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Database table for storing emergency call records.
 * Stores metadata about each 911 call including location, emergency type, and file paths.
 */
object CallRecords : Table("call_records") {
    val callId: Column<String> = varchar("call_id", 255)
    val callSid: Column<String?> = varchar("call_sid", 255).nullable()
    val callerPhone: Column<String?> = varchar("caller_phone", 50).nullable()
    val startTime: Column<LocalDateTime> = datetime("start_time")
    val endTime: Column<LocalDateTime?> = datetime("end_time").nullable()
    val duration: Column<Int?> = integer("duration").nullable() // Duration in seconds
    val status: Column<String> = varchar("status", 50) // initiated, in_progress, completed, failed
    val emergencyType: Column<String?> = varchar("emergency_type", 50).nullable() // medical, fire, police, other
    val location: Column<String?> = varchar("location", 500).nullable() // Extracted location
    val coordinates: Column<String?> = varchar("coordinates", 100).nullable() // lat,lon
    val severity: Column<Int?> = integer("severity").nullable() // 1-5 severity score
    val dispatchStatus: Column<String?> = varchar("dispatch_status", 50).nullable() // pending, dispatched, failed
    val audioFileUrl: Column<String?> = varchar("audio_file_url", 1000).nullable() // Path to audio recording
    val transcriptFileUrl: Column<String?> = varchar("transcript_file_url", 1000).nullable() // Path to transcript
    val metadata: Column<String?> = text("metadata").nullable() // JSON metadata

    override val primaryKey = PrimaryKey(callId)
}

/**
 * Database table for storing detailed conversation transcripts.
 * Each row represents a segment of the conversation (speaker turn).
 */
object Transcripts : Table("transcripts") {
    val id: Column<Int> = integer("id").autoIncrement()
    // Explicitly reference the Column<String> from CallRecords
    val callId: Column<String> = varchar("call_id", 255).references(CallRecords.callId)
    val timestamp: Column<LocalDateTime> = datetime("timestamp")
    val speaker: Column<String> = varchar("speaker", 50) // "agent" or "caller"
    val text: Column<String> = text("text")
    val confidence: Column<Double?> = double("confidence").nullable() // Transcription confidence 0.0-1.0
    val sentiment: Column<String?> = varchar("sentiment", 50).nullable() // positive, neutral, negative, urgent

    override val primaryKey = PrimaryKey(id)
}

/**
 * Database table for storing extracted entities from calls.
 * Stores specific information extracted during the call (names, addresses, conditions, etc.)
 */
object ExtractedEntities : Table("extracted_entities") {
    val id: Column<Int> = integer("id").autoIncrement()
    val callId: Column<String> = varchar("call_id", 255).references(CallRecords.callId)
    val entityType: Column<String> = varchar("entity_type", 100) // address, person_name, condition, landmark, etc.
    val entityValue: Column<String> = varchar("entity_value", 500)
    val confidence: Column<Double?> = double("confidence").nullable()
    val extractedAt: Column<LocalDateTime> = datetime("extracted_at")

    override val primaryKey = PrimaryKey(id)
}