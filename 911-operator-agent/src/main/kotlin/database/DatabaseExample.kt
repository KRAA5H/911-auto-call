package org.example.database

import org.example.storage.FileStorage
import org.example.storage.TranscriptSegment
import java.time.LocalDateTime

/**
 * Example usage of the database and storage systems.
 * This demonstrates how to store emergency call data.
 */
object DatabaseExample {
    
    fun runExample() {
        val fileStorage = FileStorage()
        
        // Example: Simulate an emergency call
        val callId = "call-${System.currentTimeMillis()}"
        
        println("\n📞 Simulating Emergency Call: $callId")
        println("=".repeat(60))
        
        // Step 1: Create initial call record
        println("\n1️⃣ Creating call record...")
        CallRepository.createCall(
            callId = callId,
            startTime = LocalDateTime.now(),
            status = "in_progress"
        )
        
        // Step 2: Add transcript segments as conversation happens
        println("\n2️⃣ Adding conversation transcripts...")
        
        TranscriptRepository.addTranscript(
            callId = callId,
            speaker = "agent",
            text = "911, what is your emergency?",
            confidence = 1.0
        )
        
        TranscriptRepository.addTranscript(
            callId = callId,
            speaker = "caller",
            text = "There's a fire at 123 Main Street!",
            confidence = 0.95,
            sentiment = "urgent"
        )
        
        TranscriptRepository.addTranscript(
            callId = callId,
            speaker = "agent",
            text = "Stay calm. Is anyone injured?",
            confidence = 1.0
        )
        
        TranscriptRepository.addTranscript(
            callId = callId,
            speaker = "caller",
            text = "No, everyone is outside safely.",
            confidence = 0.92,
            sentiment = "positive"
        )
        
        // Step 3: Extract entities
        println("\n3️⃣ Extracting entities...")
        
        EntityRepository.addEntity(
            callId = callId,
            entityType = "address",
            entityValue = "123 Main Street",
            confidence = 0.95
        )
        
        EntityRepository.addEntity(
            callId = callId,
            entityType = "emergency_type",
            entityValue = "fire",
            confidence = 0.98
        )
        
        // Step 4: Update call record with extracted information
        println("\n4️⃣ Updating call record with extracted information...")
        
        CallRepository.updateCall(
            callId = callId,
            emergencyType = "fire",
            location = "123 Main Street",
            severity = 4,
            status = "completed",
            endTime = LocalDateTime.now(),
            duration = 120 // 2 minutes
        )
        
        // Step 5: Save transcript to file
        println("\n5️⃣ Saving transcript to file...")
        
        val transcriptSegments = TranscriptRepository.getTranscripts(callId).map { row ->
            TranscriptSegment(
                timestamp = row[Transcripts.timestamp],
                speaker = row[Transcripts.speaker],
                text = row[Transcripts.text],
                confidence = row[Transcripts.confidence]
            )
        }
        
        val transcriptPath = fileStorage.saveStructuredTranscript(callId, transcriptSegments)
        
        // Step 6: Update call record with file path
        CallRepository.updateCall(
            callId = callId,
            transcriptFileUrl = transcriptPath
        )
        
        // Step 7: Retrieve and display final record
        println("\n6️⃣ Final Call Record:")
        println("=".repeat(60))
        
        val callRecord = CallRepository.getCall(callId)
        callRecord?.let { record ->
            println("Call ID: ${record[CallRecords.callId]}")
            println("Status: ${record[CallRecords.status]}")
            println("Emergency Type: ${record[CallRecords.emergencyType]}")
            println("Location: ${record[CallRecords.location]}")
            println("Severity: ${record[CallRecords.severity]}/5")
            println("Duration: ${record[CallRecords.duration]} seconds")
            println("Transcript: ${record[CallRecords.transcriptFileUrl]}")
        }
        
        println("\n✅ Example completed successfully!")
        println("=".repeat(60))
    }
}
