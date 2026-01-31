package example.agents.specialists.location

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels.Haiku_4_5
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor

class LocationAgent(apiKey: String) {

    private val tools = LocationTools()

    // Create the specialized registry for this agent
    private val registry = ToolRegistry {
        tools(tools.asTools())
    }

    private val agent = AIAgent(
        promptExecutor = simpleAnthropicExecutor(apiKey),
        llmModel = Haiku_4_5,
        // Specialized System Prompt: It only cares about location accuracy
        systemPrompt = "You are a Location Specialist. Verify addresses or landmarks. If a location is a known landmark (like 'Tower Bridge'), accept the general coordinates and calculate the ETA to the example.main entrance/center. Do not ask for clarification unless the location is completely unknown.",
        toolRegistry = registry
    )

    // The public method the rest of the app uses to talk to this agent
    suspend fun ask(query: String): String {
        return agent.run(query)
    }
}