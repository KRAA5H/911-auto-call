package org.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels.Haiku_4_5
import org.example.agents.location.LocationTools
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🚀 Starting Koog Agent Demo with OpenAI...")

    // Get API key from environment variable
    val apiKey = System.getenv("OPENAI_API_KEY")
        ?: error("❌ OPENAI_API_KEY environment variable is not set. Please set it first.")

    // Create a simple AI agent using OpenAI
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(apiKey),
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
}