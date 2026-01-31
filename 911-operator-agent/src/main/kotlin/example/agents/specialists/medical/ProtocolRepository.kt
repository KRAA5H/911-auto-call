package `medical-protocol-agent`

import kotlinx.serialization.json.Json

object ProtocolRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var protocols: List<Protocol>

    fun initialize() {
        val jsonFile = object {}.javaClass.getResourceAsStream("protocols_seed.json")
            ?: throw IllegalStateException("protocols_seed.json not found in resources")

        val content = jsonFile.bufferedReader().use { it.readText() }
        protocols = json.decodeFromString<List<Protocol>>(content)

        // Assign IDs to protocols
        protocols = protocols.mapIndexed { index, protocol ->
            protocol.copy(id = index + 1)
        }

        println("✓ Loaded ${protocols.size} protocols from JSON")
    }

    fun getProtocolById(id: Int): Protocol? {
        return protocols.find { it.id == id }
    }

    fun getCandidateProtocols(
        ageGroup: String?,
        limit: Int = Config.MAX_PROTOCOLS_FOR_SELECTION
    ): List<Protocol> {
        return protocols
            .filter { it.severityLevel in listOf("life_threatening", "urgent") }
            .filter { ageGroup == null || ageGroup in it.ageGroups }
            .sortedWith(compareByDescending<Protocol> { it.timeCritical }.thenByDescending { it.severityLevel })
            .take(limit)
    }
}