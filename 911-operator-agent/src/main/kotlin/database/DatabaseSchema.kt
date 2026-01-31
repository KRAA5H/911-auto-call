package org.example.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Database table for storing emergency call records.
 * Stores metadata about each 911 call including location, emergency type, and file paths.
 */
object CallRecords : Table("call_records") {
    val callId = varchar("call_id", 255)
    val callSid = varchar("call_sid", 255).nullable()
    val callerPhone = varchar("caller_phone", 50).nullable()
    val startTime = datetime("start_time")
    val endTime = datetime("end_time").nullable()
    val duration = integer("duration").nullable() // Duration in seconds
    val status = varchar("status", 50) // initiated, in_progress, completed, failed
    val emergencyType = varchar("emergency_type", 50).nullable() // medical, fire, police, other
    val location = varchar("location", 500).nullable() // Extracted location
    val coordinates = varchar("coordinates", 100).nullable() // lat,lon
    val severity = integer("severity").nullable() // 1-5 severity score
    val dispatchStatus = varchar("dispatch_status", 50).nullable() // pending, dispatched, failed
    val audioFileUrl = varchar("audio_file_url", 1000).nullable() // Path to audio recording
    val transcriptFileUrl = varchar("transcript_file_url", 1000).nullable() // Path to transcript
    val metadata = text("metadata").nullable() // JSON metadata
    
    override val primaryKey = PrimaryKey(callId)
}

/**
 * Database table for storing detailed conversation transcripts.
 * Each row represents a segment of the conversation (speaker turn).
 */
object Transcripts : Table("transcripts") {
    val id = integer("id").autoIncrement()
    val callId = varchar("call_id", 255).references(CallRecords.callId)
    val timestamp = datetime("timestamp")
    val speaker = varchar("speaker", 50) // "agent" or "caller"
    val text = text("text")
    val confidence = double("confidence").nullable() // Transcription confidence 0.0-1.0
    val sentiment = varchar("sentiment", 50).nullable() // positive, neutral, negative, urgent
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Database table for storing extracted entities from calls.
 * Stores specific information extracted during the call (names, addresses, conditions, etc.)
 */
object ExtractedEntities : Table("extracted_entities") {
    val id = integer("id").autoIncrement()
    val callId = varchar("call_id", 255).references(CallRecords.callId)
    val entityType = varchar("entity_type", 100) // address, person_name, condition, landmark, etc.
    val entityValue = varchar("entity_value", 500)
    val confidence = double("confidence").nullable()
    val extractedAt = datetime("extracted_at")
    
    override val primaryKey = PrimaryKey(id)
}
