package example.agents.specialists.medical

object ProtocolRetriever {

    fun retrieveProtocol(
        utterance: String,
        patientContext: PatientContext
    ): Protocol? {
        val startTime = System.currentTimeMillis()

        // ============ STAGE 1: KEYWORD MATCHING ============
        println("Stage 1: Checking critical keywords...")
        val keywordProtocolId = KeywordMatcher.findProtocolId(utterance)

        if (keywordProtocolId != null) {
            val protocol = ProtocolRepository.getProtocolById(keywordProtocolId)
            val elapsed = System.currentTimeMillis() - startTime
            println("✓ Stage 1 HIT: Found protocol in ${elapsed}ms")
            return protocol
        }

        println("Stage 1 MISS: No critical keyword match")

        // ============ STAGE 2: CLAUDE SELECTION ============
        println("Stage 2: Querying database for candidates...")
        val candidates = ProtocolRepository.getCandidateProtocols(
            ageGroup = patientContext.ageGroup,
            limit = Config.MAX_PROTOCOLS_FOR_SELECTION
        )

        if (candidates.isEmpty()) {
            println("✗ Stage 2 MISS: No candidate protocols found")
            return null
        }

        println("Stage 2: Found ${candidates.size} candidates, asking Claude...")
        val selectedProtocol = ClaudeSelector.selectBestProtocol(
            utterance = utterance,
            patientContext = patientContext,
            candidates = candidates
        )

        val elapsed = System.currentTimeMillis() - startTime
        if (selectedProtocol != null) {
            println("✓ Stage 2 HIT: Found protocol in ${elapsed}ms")
        } else {
            println("✗ Stage 2 MISS: Claude couldn't select a protocol")
        }

        return selectedProtocol
    }

    fun generateInstructions(
        protocol: Protocol,
        utterance: String,
        patientContext: PatientContext
    ): MedicalResponse {
        // Just return the protocol data directly - no Claude needed!
        return MedicalResponse(
            immediateInstruction = protocol.immediateAction,
            stepByStep = protocol.stepByStepInstructions,
            criticalPoints = protocol.criticalWarnings,
            reassurance = "Emergency services have been notified",
            protocolId = protocol.id,
            conditionDetected = protocol.conditionName,
            severity = protocol.severityLevel
        )
    }
}