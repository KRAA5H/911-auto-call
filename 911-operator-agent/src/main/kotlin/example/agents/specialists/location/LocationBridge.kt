package example.agents.specialists.location

import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.reflect.ToolSet

@LLMDescription("Interface for contacting the Location and Routing Department.")
class LocationBridge(private val locationAgent: LocationAgent) : ToolSet {

    @Tool
    @LLMDescription("Delegates a question to the Location Specialist. Use this for ANY verification of addresses, coordinates, or ETA calculations.")
    suspend fun consultLocationSpecialist(
        @LLMDescription("The full question or request regarding location (e.g. 'Verify 10 Downing St and get ETA for Unit 4')")
        request: String
    ): String {
        println("Dispatcher is calling Location Specialist with: '$request'")

        val response = locationAgent.ask(request)

        println("Location Specialist replied.")
        println("_------------------------------_")
        println("Response: $response")
        return response
    }
}