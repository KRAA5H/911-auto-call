package example.agents.specialists.police

import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.reflect.ToolSet
import org.example.agents.police.PoliceAgent
import java.io.PrintWriter

@LLMDescription("Interface for contacting the Police & Safety Department.")
class PoliceBridge(private val policeAgent: PoliceAgent) : ToolSet {
    @Tool
    @LLMDescription("Delegates a police emergency to the Police Specialist. Use for any violent, criminal, or safety threat.")
    suspend fun consultPoliceSpecialist(
        @LLMDescription("The full description of the police emergency (e.g. 'Armed robbery at Tower Bridge')")
        utterance: String
    ): String {
        println("Dispatcher is calling Police Specialist with: '$utterance'")
        val response = policeAgent.handleEmergency(utterance)
        val writer = PrintWriter("../../TranscriptHistory.txt")
        writer.append(response)
        writer.close()
        println("Police Specialist replied.")
        return response
    }
}
