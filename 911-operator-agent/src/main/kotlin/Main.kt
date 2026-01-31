package org.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels.Haiku_4_5
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import org.example.agents.location.LocationAgent
import org.example.agents.location.LocationBridge
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val apiKey = System.getenv("ANTHROPIC_API_KEY")
        ?: error("❌ ANTHROPIC_API_KEY environment variable is not set.")

    println("🚀 Starting 911 Dispatch System (Multi-Agent)...")

    // 1. Initialize the Sub-Agent (The Specialist)
    // This agent holds the actual maps and routing logic.
    val locationAgent = LocationAgent(apiKey)

    // 2. Create the Bridge (The Interface)
    // This wraps the LocationAgent so the Main Agent sees it as a "tool".
    val locationBridge = LocationBridge(locationAgent)

    // 3. Register the Bridge Tool for the Main Agent
    val mainRegistry = ToolRegistry {
        tools(locationBridge.asTools())
    }

    // 4. Create the Main Supervisor Agent
    // Notice: It doesn't have 'LocationTools'. It only has 'LocationBridge'.
    val supervisorAgent = AIAgent(
        promptExecutor = simpleAnthropicExecutor(apiKey),
        llmModel = Haiku_4_5,

        systemPrompt = "You are a 911 Supervisor. You do not have direct access to maps. You MUST delegate any location verification or ETA tasks to your 'consultLocationSpecialist' tool. Once they reply, combine their technical data into a final report for the user.",

        toolRegistry = mainRegistry
    )

    // 5. Run the Test Scenario
    val incidentPrompt = "We have a reported fire at 'Tower Bridge, London'. Verify this location and tell me the ETA for the nearest unit."

    println("\n💬 User: '$incidentPrompt'")

    // The Supervisor will receive this, decide it needs help, call the Bridge,
    // which calls the Location Agent, which calls the Tools.
    val response = supervisorAgent.run(incidentPrompt)

    println("\n🤖 Supervisor Final Report:\n$response")
}