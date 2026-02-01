package example.agents.specialists.nhs111

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Medical Assessment Agent
 * Uses AI to assess symptoms and determine next steps
 */
class MedicalAssessmentAgent(private val apiKey: String) {
        private val doctorExportDir = "./to_doctor"

        private fun exportAssessmentToFile(assessment: MedicalAssessment) {
            try {
                val timestamp = System.currentTimeMillis()
                val safeSummary = assessment.symptomSummary.take(40).replace("[^a-zA-Z0-9]+".toRegex(), "_")
                val fileName = "assessment_${'$'}timestamp_${'$'}safeSummary.txt"
                val filePath = "$doctorExportDir/${'$'}fileName"
                val fileContent = buildString {
                    appendLine("==== NHS 111 Medical Assessment ====")
                    appendLine("Symptom Summary: ${'$'}{assessment.symptomSummary}")
                    appendLine("Severity: ${'$'}{assessment.severity}")
                    appendLine("Recommendation: ${'$'}{assessment.recommendation}")
                    appendLine("\n--- Conversation Transcript ---")
                    assessment.conversationTranscript.forEach { appendLine(it) }
                }
                java.io.File(filePath).writeText(fileContent)
            } catch (e: Exception) {
                println("[ERROR] Could not export assessment: ${'$'}{e.message}")
            }
        }
    
    private var finalAssessment: MedicalAssessment? = null
    private val client = OkHttpClient()
    private val conversationHistory = mutableListOf<JSONObject>()
    
    private val systemPrompt = """
You are an NHS 111 medical triage AI. Your job is to assess symptoms and decide on the appropriate action.

═══════════════════════════════════════════════════════════════════════════════════
CRITICAL RULES:
═══════════════════════════════════════════════════════════════════════════════════

1. Ask ONE focused question at a time to understand their symptoms:
   - Start with the most important question first
   - Wait for their answer before asking the next question
   - NEVER ask multiple questions in one response
   - Keep questions short and conversational
   - Gather information about: what the problem is, how severe, how long, any red flags

2. After gathering enough information, you MUST classify using ONE of these tools:

   🚨 flag_as_critical() - Use for:
   - Chest pain (especially with arm pain, sweating, breathlessness)
   - Difficulty breathing or choking
   - Severe bleeding that won't stop
   - Unconsciousness or confusion
   - Suspected stroke (face drooping, arm weakness, speech problems)
   - Severe allergic reaction
   - Seizures
   - Suspected poisoning
   - Severe abdominal pain (especially right side - appendicitis)
   - Any life-threatening situation

   📋 needs_doctor() - Use for MOST cases:
   - Persistent pain (even if not severe)
   - Fever with other symptoms
   - Infections (UTI, ear infection, etc.)
   - Injuries that might need examination
   - Ongoing symptoms that aren't improving
   - Anything you're unsure about
   - Rashes, swelling, lumps
   - Digestive issues lasting more than a day
   **DEFAULT TO THIS if you're uncertain - better safe than sorry**

   ✅ provide_self_care_advice() - ONLY use for clearly trivial issues:
   - Common cold (mild, no fever)
   - Minor headache
   - Tiredness/fatigue with no other symptoms
   - Minor muscle ache from exercise
   - Very minor scrapes/cuts
   **RARELY use this - when in doubt, escalate to doctor**

3. Be conversational but efficient. Ask ONE question at a time - NEVER list multiple questions.

4. If they mention ANYTHING serious, flag it immediately - don't keep asking questions.

CRITICAL: Always ask ONLY ONE QUESTION per response. Do NOT create bulleted lists of questions.

═══════════════════════════════════════════════════════════════════════════════════
EXAMPLES:
═══════════════════════════════════════════════════════════════════════════════════

EXAMPLE 1 - CRITICAL:
User: "I have really bad chest pain"
You: "Is the chest pain spreading to your arm or jaw?"
User: "Yes, down my left arm, and I'm sweating a lot"
You: [IMMEDIATELY call flag_as_critical()]

EXAMPLE 2 - NEEDS DOCTOR (most common):
User: "My stomach has been hurting for a day"
You: "Where exactly does it hurt?"
User: "Lower right side, about a 7"
You: "How would you rate the pain on a scale of 1-10?"
User: "About a 7"
You: [Call needs_doctor() - could be appendicitis, urgency: "today"]

EXAMPLE 3 - NEEDS DOCTOR (default when unsure):
User: "I have a headache and feel a bit sick"
You: "How long have you had these symptoms?"
User: "Two days"
You: "Do you have a fever?"
User: "Yes, I feel hot"
You: [Call needs_doctor() - could be infection, urgency: "within 24 hours"]

EXAMPLE 4 - SELF CARE (rare):
User: "I'm just feeling a bit tired lately"
You: "Do you have any other symptoms like pain or fever?"
User: "No, just tired after work"
You: [Call provide_self_care_advice() - "Ensure adequate sleep (7-9 hours), stay hydrated..."]

═══════════════════════════════════════════════════════════════════════════════════
Remember: When in doubt, escalate to a doctor. It's better to be cautious.
═══════════════════════════════════════════════════════════════════════════════════
    """.trimIndent()
    
    private val tools = JSONArray().apply {
        put(JSONObject().apply {
            put("name", "flag_as_critical")
            put("description", "Mark this as CRITICAL - life-threatening emergency requiring immediate attention")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("symptomSummary", JSONObject().apply {
                        put("type", "string")
                        put("description", "Summary of the critical symptoms")
                    })
                    put("reason", JSONObject().apply {
                        put("type", "string")
                        put("description", "Why this is critical (e.g., 'possible heart attack', 'severe bleeding')")
                    })
                })
                put("required", JSONArray().apply {
                    put("symptomSummary")
                    put("reason")
                })
            })
        })
        put(JSONObject().apply {
            put("name", "needs_doctor")
            put("description", "Classify this case as needing a doctor - hand off with full summary")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("symptomSummary", JSONObject().apply {
                        put("type", "string")
                        put("description", "Summary of symptoms and key details")
                    })
                    put("reason", JSONObject().apply {
                        put("type", "string")
                        put("description", "Why a doctor is needed")
                    })
                    put("urgency", JSONObject().apply {
                        put("type", "string")
                        put("description", "Recommended urgency: 'today', 'within 24 hours', 'within week'")
                    })
                })
                put("required", JSONArray().apply {
                    put("symptomSummary")
                    put("reason")
                    put("urgency")
                })
            })
        })
        put(JSONObject().apply {
            put("name", "provide_self_care_advice")
            put("description", "Provide self-care advice for minor issues that don't need a doctor")
            put("input_schema", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("symptomSummary", JSONObject().apply {
                        put("type", "string")
                        put("description", "Brief summary of the issue")
                    })
                    put("advice", JSONObject().apply {
                        put("type", "string")
                        put("description", "Specific advice (e.g., 'rest and take paracetamol', 'drink plenty of water')")
                    })
                })
                put("required", JSONArray().apply {
                    put("symptomSummary")
                    put("advice")
                })
            })
        })
    }
    
    /**
     * Process a message from the caller
     */
    suspend fun processMessage(message: String): String {
        // Add user message to history
        conversationHistory.add(JSONObject().apply {
            put("role", "user")
            put("content", message)
        })
        
        // Create API request
        val requestBody = JSONObject().apply {
            put("model", "claude-sonnet-4-20250514")
            put("max_tokens", 2048)
            put("system", systemPrompt)
            put("messages", JSONArray(conversationHistory))
            put("tools", tools)
        }
        
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        // Execute request
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val jsonResponse = JSONObject(responseBody)
        
        // Process response
        var assistantText = ""
        val content = jsonResponse.getJSONArray("content")
        
        for (i in 0 until content.length()) {
            val block = content.getJSONObject(i)
            when (block.getString("type")) {
                "text" -> {
                    assistantText = block.getString("text")
                }
                "tool_use" -> {
                    handleToolUse(block)
                    // Add tool result to conversation
                    conversationHistory.add(JSONObject().apply {
                        put("role", "assistant")
                        put("content", JSONArray().apply {
                            put(block)
                        })
                    })
                    conversationHistory.add(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "tool_result")
                                put("tool_use_id", block.getString("id"))
                                put("content", "Tool executed successfully")
                            })
                        })
                    })
                }
            }
        }
        
        // Add assistant response to history if text
        if (assistantText.isNotEmpty()) {
            conversationHistory.add(JSONObject().apply {
                put("role", "assistant")
                put("content", assistantText)
            })
        }
        
        return if (finalAssessment != null) {
            "Assessment complete."
        } else {
            assistantText
        }
    }
    
    private fun handleToolUse(toolUse: JSONObject) {
        val toolName = toolUse.getString("name")
        val input = toolUse.getJSONObject("input")
        
        when (toolName) {
            "flag_as_critical" -> {
                val symptomSummary = input.getString("symptomSummary")
                val reason = input.getString("reason")
                finalAssessment = MedicalAssessment(
                    symptomSummary = symptomSummary,
                    severity = AssessmentSeverity.CRITICAL,
                    recommendation = "⚠️ CRITICAL: $reason - Immediate medical attention required. Call 999 or go to A&E NOW.",
                    conversationTranscript = extractConversationText()
                )
                exportAssessmentToFile(finalAssessment!!)
                println("\n🚨 CRITICAL CASE FLAGGED 🚨")
                println("Reason: $reason")
                println("Symptoms: $symptomSummary")
            }
            "needs_doctor" -> {
                val symptomSummary = input.getString("symptomSummary")
                val reason = input.getString("reason")
                val urgency = input.getString("urgency")
                finalAssessment = MedicalAssessment(
                    symptomSummary = symptomSummary,
                    severity = AssessmentSeverity.NEEDS_DOCTOR,
                    recommendation = "Doctor review needed ($urgency). Reason: $reason",
                    conversationTranscript = extractConversationText()
                )
                exportAssessmentToFile(finalAssessment!!)
                println("\n📋 DOCTOR HANDOFF REQUIRED")
                println("Urgency: $urgency")
                println("Reason: $reason")
            }
            "provide_self_care_advice" -> {
                val symptomSummary = input.getString("symptomSummary")
                val advice = input.getString("advice")
                
                finalAssessment = MedicalAssessment(
                    symptomSummary = symptomSummary,
                    severity = AssessmentSeverity.SELF_CARE,
                    recommendation = advice,
                    conversationTranscript = extractConversationText()
                )
                
                println("\n✅ SELF-CARE ADVICE PROVIDED")
                println("Advice: $advice")
            }
        }
    }
    
    private fun extractConversationText(): List<String> {
        return conversationHistory.mapNotNull { msg ->
            try {
                val role = msg.getString("role")
                val content = msg.get("content")
                when (content) {
                    is String -> "$role: $content"
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Get the final assessment (if made)
     */
    fun getAssessment(): MedicalAssessment? {
        return finalAssessment
    }
    
    /**
     * Check if assessment is complete
     */
    fun isAssessmentComplete(): Boolean {
        return finalAssessment != null
    }
}
