# Folder Structure

## Project Organization

```
911-auto-call/
│
├── README.md                    # Project overview
├── FEATURES.md                  # Feature specifications
├── ARCHITECTURE.md              # System architecture
├── FOLDER_STRUCTURE.md          # This file
├── IMPLEMENTATION_ROADMAP.md    # Development roadmap
├── LICENSE                      # License file
├── .env.example                 # Environment variables template
├── .gitignore                   # Git ignore rules
├── requirements.txt             # Python dependencies
├── pyproject.toml              # Project metadata and tools
│
├── src/                        # Source code
│   ├── __init__.py
│   ├── main.py                 # Application entry point
│   │
│   ├── api/                    # API layer
│   │   ├── __init__.py
│   │   ├── routes.py           # API route definitions
│   │   ├── webhooks.py         # Twilio webhook handlers
│   │   └── models.py           # Pydantic request/response models
│   │
│   ├── core/                   # Core business logic
│   │   ├── __init__.py
│   │   ├── orchestrator.py     # Call orchestration
│   │   ├── session.py          # Session management
│   │   ├── dispatcher.py       # Emergency dispatch logic
│   │   └── classifier.py       # Emergency type classification
│   │
│   ├── agents/                 # Voice agent integration
│   │   ├── __init__.py
│   │   ├── elevenlabs.py       # ElevenLabs client
│   │   ├── prompts.py          # Agent prompt templates
│   │   └── conversation.py     # Conversation flow management
│   │
│   ├── processing/             # Data processing pipeline
│   │   ├── __init__.py
│   │   ├── transcription.py    # Speech-to-text
│   │   ├── extraction.py       # Entity extraction (NER)
│   │   ├── location.py         # Geocoding and location services
│   │   └── validation.py       # Data validation
│   │
│   ├── integrations/           # External service integrations
│   │   ├── __init__.py
│   │   ├── twilio_client.py    # Twilio telephony
│   │   ├── emergency_services.py # Emergency dispatch APIs
│   │   └── storage.py          # S3 storage client
│   │
│   ├── database/               # Database layer
│   │   ├── __init__.py
│   │   ├── models.py           # SQLAlchemy ORM models
│   │   ├── connection.py       # Database connection
│   │   └── repositories.py     # Data access layer
│   │
│   ├── config/                 # Configuration
│   │   ├── __init__.py
│   │   ├── settings.py         # Application settings
│   │   └── logging.py          # Logging configuration
│   │
│   └── utils/                  # Utilities
│       ├── __init__.py
│       ├── exceptions.py       # Custom exceptions
│       └── helpers.py          # Helper functions
│
├── tests/                      # Test suite
│   ├── __init__.py
│   ├── conftest.py             # Pytest configuration
│   ├── unit/                   # Unit tests
│   │   ├── test_orchestrator.py
│   │   ├── test_classifier.py
│   │   └── test_extraction.py
│   ├── integration/            # Integration tests
│   │   ├── test_api.py
│   │   └── test_webhooks.py
│   └── fixtures/               # Test fixtures
│       └── sample_calls.py
│
├── data/                       # Data directory (gitignored)
│   ├── training/               # Training data
│   │   └── nhs_calls/          # NHS ambulance dataset
│   ├── recordings/             # Local call recordings
│   └── transcripts/            # Generated transcripts
│
├── scripts/                    # Utility scripts
│   ├── setup_db.py             # Database initialization
│   ├── train_model.py          # Model training
│   └── import_training_data.py # Import NHS dataset
│
├── docs/                       # Additional documentation
│   ├── api.md                  # API documentation
│   ├── deployment.md           # Deployment guide
│   └── training.md             # Model training guide
│
├── infrastructure/             # Infrastructure as Code
│   ├── docker/                 # Docker configurations
│   │   ├── Dockerfile
│   │   └── docker-compose.yml
│   └── terraform/              # Terraform configs (future)
│       └── main.tf
│
└── monitoring/                 # Monitoring and observability
    ├── dashboard.json          # Metrics dashboard config
    └── alerts.yml              # Alert rules

```

## Directory Descriptions

### `/src` - Application Source Code
Core application code organized by functional domains. Each module is self-contained with clear responsibilities.

### `/tests` - Test Suite
Comprehensive testing including unit tests for individual components and integration tests for end-to-end flows.

### `/data` - Data Storage (Local)
Local data storage for development. In production, this is replaced by cloud storage (S3). **This directory is gitignored.**

### `/scripts` - Automation Scripts
Helper scripts for common tasks like database setup, data import, and model training.

### `/docs` - Documentation
Extended documentation for API usage, deployment procedures, and training workflows.

### `/infrastructure` - Infrastructure Configuration
Docker and IaC configurations for consistent development and deployment environments.

### `/monitoring` - Observability
Configuration files for monitoring dashboards and alerting systems.

## Key Files

### Configuration Files
- **`.env.example`**: Template for environment variables (API keys, database URLs)
- **`requirements.txt`**: Python package dependencies
- **`pyproject.toml`**: Project metadata, build system, and tool configurations

### Application Entry Points
- **`src/main.py`**: FastAPI application initialization and startup
- **`src/api/webhooks.py`**: Twilio webhook handlers for call events

### Core Logic
- **`src/core/orchestrator.py`**: Central call orchestration logic
- **`src/agents/elevenlabs.py`**: Voice agent integration
- **`src/processing/extraction.py`**: Information extraction from conversations

## Naming Conventions

- **Files**: `snake_case.py`
- **Classes**: `PascalCase`
- **Functions/Variables**: `snake_case`
- **Constants**: `UPPER_SNAKE_CASE`
- **Test files**: `test_*.py`

## Import Structure

```python
# Standard library imports
import os
from typing import List

# Third-party imports
from fastapi import FastAPI
from pydantic import BaseModel

# Local application imports
from src.core.orchestrator import CallOrchestrator
from src.config.settings import settings
```

## Development Workflow

1. Code changes in `/src`
2. Add tests in `/tests`
3. Run tests: `pytest tests/`
4. Check code quality: `ruff check src/`
5. Format code: `ruff format src/`
6. Commit changes

## Notes

- Keep modules focused and single-responsibility
- Minimize coupling between modules
- Use dependency injection for testability
- Follow Python PEP 8 style guidelines
- Document public APIs with docstrings
