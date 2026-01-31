# System Architecture

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Emergency Callers                        │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Telephony Layer (Twilio)                      │
│  - Inbound call routing                                          │
│  - Call queueing                                                 │
│  - Audio streaming                                               │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Call Orchestrator                           │
│  - Session management                                            │
│  - Call state tracking                                           │
│  - Load balancing                                                │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    ▼                       ▼
┌────────────────────────────┐  ┌──────────────────────────────┐
│   Voice Agent Engine       │  │   Data Processing Pipeline   │
│   (ElevenLabs API)         │  │   - Speech-to-Text           │
│   - Conversational AI      │  │   - NLP & Entity Extraction  │
│   - Context management     │  │   - Classification           │
│   - Response generation    │  │   - Validation               │
└────────────┬───────────────┘  └──────────────┬───────────────┘
             │                                  │
             └──────────────┬───────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Emergency Processing Core                     │
│  - Location resolution                                           │
│  - Emergency type detection                                      │
│  - Severity assessment                                           │
│  - Dispatch decision engine                                      │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                    ┌───────────┴────────────┐
                    ▼                        ▼
┌────────────────────────────┐  ┌───────────────────────────────┐
│   Dispatch Integration     │  │   Storage & Logging           │
│   - Police API             │  │   - Call recordings (S3)      │
│   - Fire API               │  │   - PostgreSQL database       │
│   - Ambulance API          │  │   - Transcripts               │
│   - Multi-service routing  │  │   - Audit logs                │
└────────────────────────────┘  └───────────────────────────────┘
```

## System Components

### 1. Telephony Layer
- **Technology**: Twilio Voice API
- **Responsibilities**:
  - Receive incoming emergency calls
  - Queue management
  - Audio stream routing to Voice Agent
  - Call recording initiation

### 2. Call Orchestrator
- **Technology**: Python/FastAPI
- **Responsibilities**:
  - Session lifecycle management
  - WebSocket connections for real-time audio
  - Load distribution across voice agents
  - State management (Redis)

### 3. Voice Agent Engine
- **Technology**: ElevenLabs Conversational AI
- **Responsibilities**:
  - Natural language conversation
  - Context-aware dialogue management
  - Emergency-specific prompts
  - Real-time response generation

### 4. Data Processing Pipeline
- **Technology**: Python + OpenAI Whisper (STT)
- **Responsibilities**:
  - Transcription of caller audio
  - Named entity recognition (addresses, names)
  - Emergency classification (medical/fire/police)
  - Data validation and sanitization

### 5. Emergency Processing Core
- **Technology**: Python business logic
- **Responsibilities**:
  - Geocoding and location verification
  - Emergency type determination
  - Severity scoring algorithm
  - Dispatch prioritization

### 6. Dispatch Integration
- **Technology**: REST API clients
- **Responsibilities**:
  - Interface with emergency service APIs
  - Retry logic and fallback mechanisms
  - Status tracking

### 7. Storage & Logging
- **Technology**: PostgreSQL + AWS S3
- **Responsibilities**:
  - Call recordings storage
  - Metadata persistence
  - Compliance and audit trails
  - Training data repository

## Data Flow

1. **Call Initiation**: Caller dials emergency number → Twilio receives call
2. **Agent Activation**: Orchestrator assigns voice agent → ElevenLabs session starts
3. **Conversation**: Voice agent conducts intake → Extracts information
4. **Processing**: Data pipeline analyzes conversation → Determines emergency type and location
5. **Dispatch**: Core engine routes to appropriate service → Confirms dispatch
6. **Logging**: All data stored → Recording saved → Transcript generated

## Infrastructure

### Development Environment
- Local Python environment
- Docker containers for services
- ngrok for webhooks (Twilio testing)

### Production Environment (48h MVP)
- **Hosting**: AWS EC2 / Cloud Run
- **Database**: AWS RDS PostgreSQL
- **Storage**: AWS S3
- **Cache**: Redis (ElastiCache)
- **Monitoring**: CloudWatch / Datadog

## Security Considerations

1. **Data Encryption**: All calls encrypted in transit (TLS)
2. **HIPAA Compliance**: PHI data handling protocols
3. **Access Control**: Role-based access to recordings
4. **Audit Logging**: Complete audit trail for all operations
5. **Secure Storage**: Encrypted at rest (S3 encryption)

## Scalability Strategy

1. **Horizontal Scaling**: Multiple orchestrator instances
2. **Auto-scaling**: Based on call volume
3. **Load Balancing**: Distribute across voice agent instances
4. **Database**: Read replicas for analytics
5. **CDN**: Static assets and recordings

## Technology Choices Rationale

- **ElevenLabs**: Best-in-class conversational AI with low latency
- **Twilio**: Industry-standard telephony with excellent reliability
- **Python/FastAPI**: Rapid development, excellent async support
- **PostgreSQL**: Robust, ACID-compliant for critical data
- **AWS**: Comprehensive services, reliable infrastructure
