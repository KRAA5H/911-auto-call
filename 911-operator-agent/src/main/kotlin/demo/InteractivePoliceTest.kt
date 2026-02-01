package org.example.demo

import kotlinx.coroutines.runBlocking
import org.example.agents.police.PoliceAgent

/**
 * Interactive chat mode for testing the Police Agent
 * Type 'exit' or 'quit' to stop
 */
fun main() = runBlocking {
    println("\n🚔 Police & Safety Agent - Interactive Mode")
    println("=" .repeat(60))
    println("Type your emergency message or 'exit' to quit\n")
    
    val apiKey = System.getenv("ANTHROPIC_API_KEY") 
        ?: error("❌ ANTHROPIC_API_KEY not set")
    
    val policeAgent = PoliceAgent(apiKey)
    
    while (true) {
        print("\n📞 Caller: ")
        val input = readLine()?.trim() ?: break
        
        if (input.lowercase() in listOf("exit", "quit", "q")) {
            println("\n👋 Ending session...")
            break
        }
        
        if (input.isEmpty()) continue
        
        println("\n🤖 Operator: ")
        try {
            val response = policeAgent.handleEmergency(input)
            println(response)
        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
        }
        
        println("\n" + "-".repeat(60))
    }
}
