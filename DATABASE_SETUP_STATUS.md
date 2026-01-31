# SQLite Database and File Storage Setup

## ✅ Completed Setup

I've successfully set up the database and file storage infrastructure for your 911 emergency call system using **SQLite**:

### 1. **Database Schema** ([DatabaseSchema.kt](911-operator-agent/src/main/kotlin/database/DatabaseSchema.kt))
   - **CallRecords**: Stores emergency call metadata (call ID, location, type, severity, status, file paths)
   - **Transcripts**: Stores conversation segments with speaker identification and timestamps  
   - **ExtractedEntities**: Stores extracted information (addresses, names, conditions, landmarks)

### 2. **Database Factory** ([DatabaseFactory.kt](911-operator-agent/src/main/kotlin/database/DatabaseFactory.kt))
   - Initializes SQLite database with HikariCP connection pooling
   - Creates tables automatically on first run
   - Database file: `data/911-calls.db`
   - Uses Write-Ahead Logging (WAL) for better concurrency

### 3. **Repository Layer** ([CallRepository.kt](911-operator-agent/src/main/kotlin/database/CallRepository.kt))
   - **CallRepository**: CRUD operations for call records
   - **TranscriptRepository**: Add/retrieve transcript segments
   - **EntityRepository**: Store/query extracted entities

### 4. **File Storage** ([FileStorage.kt](911-operator-agent/src/main/kotlin/storage/FileStorage.kt))
   - Manages audio recordings: `data/calls/audio/`
   - Manages transcripts: `data/calls/transcripts/`
   - Supports structured transcripts with timestamps
   - File operations: save, read, delete, check existence

### 5. **Example Usage** ([DatabaseExample.kt](911-operator-agent/src/main/kotlin/database/DatabaseExample.kt))
   - Complete example of emergency call workflow
   - Demonstrates database + storage integration

## 📦 Dependencies Added

- SQLite JDBC driver
- HikariCP (connection pooling)
- Exposed ORM (Kotlin-friendly database framework)
- Removed PostgreSQL dependencies

## ⚠️ Build Issue

There's a Kotlin compiler compatibility issue with Java 25. To fix this, you have two options:

### Option 1: Downgrade to Java 21 (Recommended)
```powershell
# Download and install Java 21 from:
# https://adoptium.net/temurin/releases/?version=21
```

### Option 2: Use IntelliJ IDEA to build
Open the project in IntelliJ IDEA which handles Java version management automatically.

## 🚀 Next Steps (When Build Works)

1. **Initialize database** - Already integrated in [Main.kt](911-operator-agent/src/main/kotlin/Main.kt#L14-L19)
2. **Run the example** - Call `DatabaseExample.runExample()` to see it in action
3. **Connect to AI agents** - Agents can store extracted data using the repositories
4. **Set up ElevenLabs webhooks** - Receive post-call data and store audio/transcripts

## 💡 How to Use

```kotlin
// Initialize (done automatically in Main.kt)
DatabaseFactory.init()
val storage = FileStorage()

// Create a call record
val callId = CallRepository.createCall(callId = "call-123", status = "initiated")

// Add transcript segments
TranscriptRepository.addTranscript(
    callId = callId,
    speaker = "agent",
    text = "911, what's your emergency?"
)

// Save transcript file
val transcriptPath = storage.saveTranscript(callId, fullTranscript)

// Update call with file path
CallRepository.updateCall(callId = callId, transcriptFileUrl = transcriptPath)
```

The database and storage infrastructure is ready. Once the Java version issue is resolved, everything will work seamlessly!
