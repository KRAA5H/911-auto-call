package org.example.demo

import kotlinx.coroutines.runBlocking
import org.example.agents.police.PoliceAgent

fun main() = runBlocking {
    val apiKey = System.getenv("ANTHROPIC_API_KEY") 
        ?: error("❌ ANTHROPIC_API_KEY not set")
    
    val policeAgent = PoliceAgent(apiKey)
    
    // Test different scenarios - change this message
    val testMessage = "someone shot me"
    
    println("\n📞 Caller: \"$testMessage\"\n")
    
    if (policeAgent.shouldTrigger(testMessage)) {
        println("🤖 Operator Response:")
        val response = policeAgent.handleEmergency(testMessage)
        println(response)
    } else {
        println("⚠️  Message does not trigger police agent")
    }
}