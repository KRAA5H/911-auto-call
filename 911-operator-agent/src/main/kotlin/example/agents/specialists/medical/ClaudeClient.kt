package `medical-protocol-agent`

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import kotlinx.coroutines.runBlocking

object ClaudeClient {
    fun generate(prompt: String): String = runBlocking {
        // Create a NEW agent for each call
        val agent = AIAgent(
            promptExecutor = simpleAnthropicExecutor(Config.ANTHROPIC_API_KEY),
            llmModel = AnthropicModels.Haiku_4_5
        )
        agent.run(prompt)
    }
}