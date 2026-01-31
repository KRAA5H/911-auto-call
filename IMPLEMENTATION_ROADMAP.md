# Implementation Roadmap - 48 Hour MVP

## First 5 Implementation Steps

### Step 1: Project Setup & Environment Configuration (Hours 0-4)

**Objective**: Create the foundational project structure and configure development environment

**Tasks**:
1. Initialize Python project with virtual environment
   ```bash
   python -m venv venv
   source venv/bin/activate
   pip install --upgrade pip
   ```

2. Install core dependencies
   ```bash
   # Create requirements.txt with:
   # - fastapi[all]
   # - elevenlabs
   # - twilio
   # - sqlalchemy
   # - psycopg2-binary
   # - python-dotenv
   # - pydantic-settings
   # - httpx
   pip install -r requirements.txt
   ```

3. Create folder structure (as per FOLDER_STRUCTURE.md)
   ```bash
   mkdir -p src/{api,core,agents,processing,integrations,database,config,utils}
   mkdir -p tests/{unit,integration,fixtures}
   mkdir -p data/{training,recordings,transcripts}
   mkdir -p scripts docs infrastructure/docker monitoring
   ```

4. Configure environment variables
   ```bash
   # Create .env with API keys:
   # - ELEVENLABS_API_KEY
   # - TWILIO_ACCOUNT_SID
   # - TWILIO_AUTH_TOKEN
   # - DATABASE_URL
   # - AWS_ACCESS_KEY_ID (for S3)
   ```

5. Set up PostgreSQL database
   ```bash
   # Local: docker-compose up postgres
   # Or use cloud database
   python scripts/setup_db.py
   ```

**Deliverables**:
- ✅ Working development environment
- ✅ All dependencies installed
- ✅ Database schema initialized
- ✅ Configuration files ready

**Success Criteria**: `python src/main.py` starts without errors

---

### Step 2: Telephony Integration (Hours 4-12)

**Objective**: Implement basic call receiving and routing using Twilio

**Tasks**:
1. Create Twilio client wrapper (`src/integrations/twilio_client.py`)
   ```python
   class TwilioClient:
       def __init__(self):
           self.client = Client(account_sid, auth_token)
       
       def setup_webhook(self, url: str):
           # Configure incoming call webhook
           pass
   ```

2. Implement webhook handler (`src/api/webhooks.py`)
   ```python
   @app.post("/webhooks/twilio/incoming")
   async def handle_incoming_call(request: Request):
       # Receive call from Twilio
       # Create TwiML response
       # Start audio stream to voice agent
       pass
   ```

3. Set up ngrok for local development webhooks
   ```bash
   ngrok http 8000
   # Configure Twilio webhook URL
   ```

4. Implement call session management (`src/core/session.py`)
   ```python
   class CallSession:
       def __init__(self, call_sid: str):
           self.call_sid = call_sid
           self.status = "initiated"
           self.start_time = datetime.now()
   ```

5. Add basic call logging to database
   ```python
   # Store: call_sid, caller_number, start_time, status
   ```

**Deliverables**:
- ✅ Twilio integration working
- ✅ Incoming calls trigger webhooks
- ✅ Call sessions created and tracked
- ✅ Basic call logs in database

**Success Criteria**: Test call to Twilio number triggers webhook and creates session

---

### Step 3: Voice Agent Integration (Hours 12-24)

**Objective**: Integrate ElevenLabs conversational AI for emergency call handling

**Tasks**:
1. Implement ElevenLabs client (`src/agents/elevenlabs.py`)
   ```python
   class ElevenLabsAgent:
       def __init__(self, agent_id: str):
           self.client = ElevenLabs(api_key=settings.ELEVENLABS_API_KEY)
           self.agent_id = agent_id
       
       async def start_conversation(self, audio_stream):
           # Initialize agent conversation
           # Handle bidirectional audio
           pass
   ```

2. Create emergency intake prompts (`src/agents/prompts.py`)
   ```python
   EMERGENCY_INTAKE_PROMPT = """
   You are an emergency 911 operator. Your job is to:
   1. Stay calm and reassuring
   2. Quickly determine: What is the emergency?
   3. Get the exact location
   4. Assess severity and any immediate dangers
   5. Keep caller on the line until help arrives
   
   Ask questions one at a time. Be concise and clear.
   """
   ```

3. Build conversation flow manager (`src/agents/conversation.py`)
   ```python
   class ConversationFlow:
       async def extract_information(self, transcript: str):
           # Extract: location, emergency_type, severity
           return EmergencyInfo(...)
   ```

4. Connect Twilio audio stream to ElevenLabs
   ```python
   # Use WebSocket for bidirectional audio streaming
   # Twilio Media Streams <-> ElevenLabs Conversational AI
   ```

5. Implement conversation state tracking
   ```python
   # Track: greeting_done, location_captured, type_identified, dispatched
   ```

**Deliverables**:
- ✅ ElevenLabs agent responds to calls
- ✅ Natural conversation flow
- ✅ Audio streams working both ways
- ✅ Conversation state tracked

**Success Criteria**: Complete test call with voice agent that asks relevant questions

---

### Step 4: Information Extraction & Classification (Hours 24-36)

**Objective**: Build data processing pipeline to extract critical emergency information

**Tasks**:
1. Implement transcription service (`src/processing/transcription.py`)
   ```python
   class TranscriptionService:
       async def transcribe_audio(self, audio_data: bytes) -> str:
           # Use Whisper or ElevenLabs transcription
           return transcript
   ```

2. Build entity extraction (`src/processing/extraction.py`)
   ```python
   class EntityExtractor:
       def extract_location(self, text: str) -> Location:
           # Use regex + NER to find addresses
           # Examples: "123 Main St", "corner of 5th and Oak"
           pass
       
       def extract_emergency_type(self, text: str) -> EmergencyType:
           # Classify: medical, fire, police, other
           pass
   ```

3. Implement location geocoding (`src/processing/location.py`)
   ```python
   class LocationService:
       async def geocode(self, address: str) -> Coordinates:
           # Use Google Maps Geocoding API
           return Coordinates(lat=..., lng=...)
   ```

4. Create emergency classifier (`src/core/classifier.py`)
   ```python
   class EmergencyClassifier:
       def classify(self, transcript: str) -> EmergencyClassification:
           # Determine: type, severity (1-5), keywords
           return EmergencyClassification(
               type="medical",
               severity=4,
               keywords=["chest pain", "difficulty breathing"]
           )
   ```

5. Build validation logic (`src/processing/validation.py`)
   ```python
   class DataValidator:
       def validate_location(self, location: Location) -> bool:
           # Ensure location is real and within service area
           pass
   ```

**Deliverables**:
- ✅ Automatic transcription of conversations
- ✅ Location extraction working
- ✅ Emergency type classification
- ✅ Data validation in place

**Success Criteria**: Process test call and correctly extract location + emergency type

---

### Step 5: Emergency Dispatch Integration (Hours 36-48)

**Objective**: Implement automated emergency service dispatch

**Tasks**:
1. Create dispatch logic (`src/core/dispatcher.py`)
   ```python
   class EmergencyDispatcher:
       async def dispatch(self, emergency: EmergencyInfo) -> DispatchResult:
           # Route to appropriate service based on type
           service = self.select_service(emergency.type)
           result = await service.dispatch(emergency)
           return result
   ```

2. Build emergency service API integrations (`src/integrations/emergency_services.py`)
   ```python
   class PoliceDispatch:
       async def dispatch(self, info: EmergencyInfo):
           # Call police dispatch API
           pass
   
   class FireDispatch:
       async def dispatch(self, info: EmergencyInfo):
           # Call fire department API
           pass
   
   class AmbulanceDispatch:
       async def dispatch(self, info: EmergencyInfo):
           # Call ambulance service API
           pass
   ```

3. Implement dispatch prioritization
   ```python
   class DispatchPriority:
       def calculate_priority(self, emergency: EmergencyInfo) -> int:
           # Based on severity, type, and other factors
           # Return priority score (1=highest, 5=lowest)
           pass
   ```

4. Add dispatch confirmation to caller
   ```python
   # Voice agent confirms: "Help is on the way. ETA: X minutes."
   ```

5. Complete end-to-end flow testing
   ```bash
   # Test scenarios:
   # 1. Medical emergency (heart attack)
   # 2. Fire emergency (house fire)
   # 3. Police emergency (break-in)
   # 4. Multi-service emergency (car accident)
   ```

**Deliverables**:
- ✅ Automated dispatch working
- ✅ All emergency types supported
- ✅ Confirmation to caller
- ✅ End-to-end system functional

**Success Criteria**: Complete test call results in proper emergency service dispatch

---

## Timeline Summary

| Hours | Phase | Key Milestone |
|-------|-------|---------------|
| 0-4 | Setup | Development environment ready |
| 4-12 | Telephony | Receiving calls via Twilio |
| 12-24 | Voice AI | AI agent conducting conversations |
| 24-36 | Processing | Extracting emergency information |
| 36-48 | Dispatch | Complete automated emergency response |

## Critical Path Items

1. **ElevenLabs API Access** - Must have conversational AI API key
2. **Twilio Account** - Phone number and API credentials
3. **Test Phone Line** - For development testing
4. **Database Setup** - PostgreSQL for call logging
5. **Emergency Service APIs** - Mock or real dispatch integration

## Risk Mitigation

### High Risk Areas
1. **Voice Agent Quality**: ElevenLabs may need prompt tuning
   - **Mitigation**: Prepare multiple prompt variations, test thoroughly

2. **Location Extraction Accuracy**: Addresses vary widely
   - **Mitigation**: Use multiple extraction methods, human fallback

3. **API Rate Limits**: ElevenLabs/Twilio may have limits
   - **Mitigation**: Implement queuing and retry logic

4. **Real-time Audio Streaming**: WebSocket complexity
   - **Mitigation**: Use proven libraries (Twilio Media Streams)

### Medium Risk Areas
1. **Emergency Service API Integration**: May not have real APIs
   - **Mitigation**: Build mock endpoints for MVP

2. **Database Performance**: High write volume
   - **Mitigation**: Use async database operations, connection pooling

## Post-MVP Enhancements (Hour 48+)

1. **Training Pipeline**: Fine-tune models with NHS dataset
2. **Dashboard**: Real-time monitoring interface
3. **Analytics**: Call metrics and performance tracking
4. **Multi-language Support**: Expand beyond English
5. **Mobile App**: Caller companion app
6. **Advanced Features**: Video calls, SMS, automated follow-up

## Testing Strategy

### During Implementation
- Unit tests for each module as it's built
- Integration tests for API endpoints
- Manual testing with real phone calls

### Before Launch
- Load testing (simulate 100+ concurrent calls)
- End-to-end testing (all emergency types)
- Failure scenario testing (network issues, API failures)
- Security testing (data encryption, access control)

## Success Metrics for 48h MVP

1. ✅ System answers 100% of test calls
2. ✅ Voice agent completes intake conversation
3. ✅ Correctly extracts location and emergency type
4. ✅ Successfully dispatches to appropriate service
5. ✅ Records and logs all calls
6. ✅ Response time <2 seconds
7. ✅ Zero crashes during testing

## Next Steps After MVP

1. Deploy to production environment
2. Begin training with NHS ambulance dataset
3. Conduct pilot with limited service area
4. Gather feedback and iterate
5. Scale infrastructure for full deployment
6. Obtain regulatory approvals and certifications
