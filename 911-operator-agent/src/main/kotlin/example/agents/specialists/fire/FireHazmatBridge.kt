package example.agents.specialists.fire

import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.reflect.ToolSet

@LLMDescription("Interface for contacting the Fire & Hazmat Department (The Marshall).")
class FireHazmatBridge(private val fireHazmatAgent: FireHazmatAgent) : ToolSet {

    @Tool
    @LLMDescription("Delegates a question to the Fire & Hazmat Specialist (The Marshall). Use this for ANY fire, hazmat, explosion, or chemical emergency questions including: identifying hazardous materials from placard numbers, determining correct fire extinguisher types, retrieving building evacuation plans, or calculating blast radius and evacuation zones.")
    suspend fun consultFireHazmatSpecialist(
        @LLMDescription("The full question or request regarding fire/hazmat (e.g. 'What is UN code 1203?', 'How to handle grease fire?', 'Get evacuation plan for City Hospital', 'Calculate blast radius for 500 gallons of propane')")
        request: String
    ): String {
        println("Dispatcher is calling Fire & Hazmat Specialist with: '$request'")
        val response = fireHazmatAgent.ask(request)
        println("Fire & Hazmat Specialist (The Marshall) replied.")
        return response
    }
}