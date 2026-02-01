package example.agents.specialists.fire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class HazmatInfoDto(
    val name: String,
    @SerialName("hazard_type") val hazardType: String,
    @SerialName("evacuation_distance_feet") val evacuationDistanceFeet: Int,
    @SerialName("un_class") val unClass: String
)

@Serializable
data class FireTypeDto(
    @SerialName("fire_class") val fireClass: String,
    @SerialName("extinguisher_types") val extinguisherTypes: String,
    val warning: String
)

@Serializable
data class BuildingSchematicDto(
    val floors: String,
    @SerialName("main_exit") val mainExit: String,
    @SerialName("fire_escapes") val fireEscapes: String,
    @SerialName("hazardous_areas") val hazardousAreas: String
)

@Serializable
data class EmergencyData(
    @SerialName("hazmat_materials") val hazmatMaterials: Map<String, HazmatInfoDto>,
    @SerialName("fire_classifications") val fireClassifications: Map<String, FireTypeDto>,
    @SerialName("building_schematics") val buildingSchematics: Map<String, BuildingSchematicDto>
)

class FireHazmatDataLoader {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var data: EmergencyData

    init {
        loadData()
    }

    private fun loadData() {
        // Ensure the file path is correct relative to where you run the agent
        val file = File("src/main/resources/fireHazmatData.json")
        if (!file.exists()) {
            throw RuntimeException("CRITICAL: fireHazmatData.json not found at ${file.absolutePath}")
        }
        val jsonString = file.readText()
        data = json.decodeFromString<EmergencyData>(jsonString)
    }

    fun getHazmatInfo(unCode: String): HazmatInfoDto? {
        val cleanCode = unCode.trim().replace(Regex("[^0-9]"), "")
        return data.hazmatMaterials[cleanCode]
    }

    fun getFireClassification(material: String): FireTypeDto? {
        val materialLower = material.lowercase().trim()

        // Try exact match first
        data.fireClassifications[materialLower]?.let { return it }

        // Try partial match
        return data.fireClassifications.entries.firstOrNull {
            materialLower.contains(it.key) || it.key.contains(materialLower)
        }?.value
    }

    fun getBuildingSchematic(buildingName: String): BuildingSchematicDto? {
        val nameLower = buildingName.lowercase().trim()

        // Try exact match first
        data.buildingSchematics[nameLower]?.let { return it }

        // Try partial match
        return data.buildingSchematics.entries.firstOrNull {
            nameLower.contains(it.key) || it.key.contains(nameLower)
        }?.value
    }
}