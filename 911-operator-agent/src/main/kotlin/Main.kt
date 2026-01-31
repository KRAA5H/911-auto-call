package org.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels.Haiku_4_5
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import org.example.agents.location.LocationTools
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🚀 Starting Koog Agent Demo with OpenAI...")

    // Get API key from environment variable
    val apiKey = System.getenv("OPENAI_API_KEY")
        ?: error("❌ OPENAI_API_KEY environment variable is not set. Please set it first.")

    // Create a simple AI agent using OpenAI
    val agent = AIAgent(
        // 1. MUST use the Anthropic executor for Anthropic models
        promptExecutor = simpleAnthropicExecutor(apiKey),
        // 2. Use the correct, raw model ID string.
        // There is no "Haiku 4.5". The current model is "claude-3-haiku-20240307".
        llmModel = Haiku_4_5,

        systemPrompt = "You are a helpful AI assistant. Be concise and friendly in your responses."
    )
    val myLocationTools = LocationTools()

    // Test the agent with a simple query
    println("\n💬 Asking agent: 'Hello! Can you introduce yourself?'")
    val response = agent.run("Hello! Can you introduce yourself?")

    println("\n✅ Agent Response:")
    println(response)

    println("\n🎉 Koog is working with OpenAI!")

    val tools = LocationTools()

    println("--- Testing Verify Address ---")
    // Test a known valid location
    val result1 = tools.verifyAddress("10 Downing Street", "London")
    println(result1)

    println("\n--- Testing Reverse Geocode (Landmark) ---")
    // Test a famous landmark
    val result2 = tools.reverseGeocodeLandmarks("Eiffel Tower")
    println(result2)

    println("\n--- Testing ETA Calculation ---")
    // Test routing from the hardcoded unit location (London) to another London spot
    val result3 = tools.calculateUnitEta("Tower Bridge, London")
    println(result3)
}