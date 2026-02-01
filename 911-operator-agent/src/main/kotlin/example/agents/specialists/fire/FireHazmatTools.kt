package example.agents.specialists.fire

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlin.math.sqrt

@LLMDescription("A collection of tools for the Fire & Hazmat Agent (The Marshall) using JSON data.")
class FireHazmatTools : ToolSet {

    private val dataLoader = FireHazmatDataLoader()

    @Tool
    @LLMDescription("Looks up the UN number on a chemical truck/container to identify the hazardous material.")
    fun identifyHazmatCode(
        @LLMDescription("The UN/NA placard number (e.g., '1203', '1075')") placardNumber: String
    ): String {
        val hazmat = dataLoader.getHazmatInfo(placardNumber)

        return if (hazmat != null) {
            val evacDist = if (hazmat.evacuationDistanceFeet >= 1000) {
                "${hazmat.evacuationDistanceFeet / 5280.0} miles"
            } else {
                "${hazmat.evacuationDistanceFeet} ft"
            }

            buildString {
                append("${hazmat.name}. ${hazmat.hazardType}. ")
                append("Evacuate $evacDist. ")
                append("UN Class: ${hazmat.unClass}. ")

                // Add specific warnings based on material type
                when {
                    hazmat.hazardType.contains("Toxic") -> append("TOXIC - Avoid inhalation. Upwind evacuation required.")
                    hazmat.hazardType.contains("Flammable") -> append("FLAMMABLE - Keep ignition sources away. Ventilate area.")
                    hazmat.hazardType.contains("Corrosive") -> append("CORROSIVE - Avoid skin/eye contact. Neutralization may be needed.")
                    hazmat.hazardType.contains("Oxidizer") -> append("OXIDIZER - Keep away from combustibles.")
                }
            }
        } else {
            "Unknown placard number: $placardNumber. Unable to identify material. Treat as hazardous and evacuate 500 ft minimum. Request HAZMAT team for identification."
        }
    }

    @Tool
    @LLMDescription("Determines the fire classification and appropriate extinguisher type based on burning material.")
    fun classifyFireType(
        @LLMDescription("Description of what is burning") burningMaterial: String
    ): String {
        val fireType = dataLoader.getFireClassification(burningMaterial)

        return if (fireType != null) {
            buildString {
                append("${fireType.fireClass} Fire. ")
                append("Use: ${fireType.extinguisherTypes}. ")
                append("${fireType.warning}")

                // Add critical safety warnings
                if (fireType.fireClass == "Class K" || burningMaterial.contains("grease")) {
                    append(" CRITICAL: Water will cause explosive splatter!")
                }
                if (fireType.fireClass == "Class C" || burningMaterial.contains("electrical")) {
                    append(" CRITICAL: Ensure power is OFF before using water-based agents!")
                }
                if (fireType.fireClass == "Class D") {
                    append(" CRITICAL: Standard extinguishers ineffective - specialized response required!")
                }
            }
        } else {
            "Unable to classify fire type for '$burningMaterial'. DEFAULT: Treat as Class A (water/foam) unless involving electrical, grease, or chemicals. When in doubt, evacuate and call fire department."
        }
    }

    @Tool
    @LLMDescription("Retrieves floor plans and exit information for commercial buildings.")
    fun getBuildingSchematic(
        @LLMDescription("The name or address of the building") buildingName: String
    ): String {
        val schematic = dataLoader.getBuildingSchematic(buildingName)

        return if (schematic != null) {
            buildString {
                append("Plan retrieved. ")
                append("Structure: ${schematic.floors}. ")
                append("Main exit: ${schematic.mainExit}. ")
                append("Fire escapes: ${schematic.fireEscapes}. ")
                if (schematic.hazardousAreas.isNotEmpty() && schematic.hazardousAreas != "None") {
                    append("CAUTION: Hazardous areas - ${schematic.hazardousAreas}.")
                }
            }
        } else {
            "Building '$buildingName' not found in database. No pre-plan available. Advise occupants to use nearest exit. Firefighters should conduct size-up on arrival."
        }
    }

    @Tool
    @LLMDescription("Estimates the evacuation zone radius for potential explosions based on material type and quantity.")
    fun calculateBlastRadius(
        @LLMDescription("Type of material (e.g., 'propane', 'gasoline', 'ammonia', 'fertilizer')") materialType: String,
        @LLMDescription("Estimated quantity (e.g., '500 gallons', '2 tons', '100 kg')") quantityEst: String,
        @LLMDescription("Current wind direction (e.g., 'North', 'Southeast') - optional") windDirection: String? = "Unknown"
    ): String {
        val material = materialType.lowercase().trim()
        val quantity = quantityEst.lowercase().trim()

        // Extract numeric value from quantity
        val numberRegex = Regex("(\\d+\\.?\\d*)")
        val quantityValue = numberRegex.find(quantity)?.value?.toDoubleOrNull() ?: 100.0

        // Determine material category and base radius
        val (category, baseRadius, notes) = when {
            material.contains("propane") || material.contains("lpg") ->
                Triple("Flammable Gas", 0.5, "BLEVE risk if heated")
            material.contains("gasoline") || material.contains("fuel") || material.contains("diesel") ->
                Triple("Flammable Liquid", 0.3, "Vapor cloud potential")
            material.contains("ammonia") ->
                Triple("Toxic Gas", 1.0, "Toxic plume - upwind evacuation critical")
            material.contains("chlorine") ->
                Triple("Toxic Gas", 1.5, "Highly toxic plume")
            material.contains("fertilizer") || material.contains("ammonium nitrate") ->
                Triple("Explosive", 0.8, "Detonation risk if contaminated")
            material.contains("acetylene") ->
                Triple("Flammable Gas", 0.6, "Unstable - detonation risk")
            material.contains("oxygen") ->
                Triple("Oxidizer", 0.4, "Accelerates combustion")
            else ->
                Triple("Unknown Material", 0.5, "Conservative estimate")
        }

        // Scale radius based on quantity (rough estimation)
        val scaleFactor = sqrt(quantityValue / 100.0)
        val finalRadius = baseRadius * scaleFactor

        return buildString {
            append("Zone: ${String.format("%.1f", finalRadius)} miles. ")
            append("Material: $category. ")
            if (windDirection != null && windDirection != "Unknown") {
                append("Wind blowing $windDirection - prioritize downwind evacuation. ")
            } else {
                append("Wind direction unknown - establish 360° perimeter. ")
            }
            append("Special notes: $notes. ")

            // Add tactical recommendations
            if (category.contains("Toxic")) {
                append("SHELTER-IN-PLACE for those unable to evacuate upwind.")
            }
            if (category.contains("Explosive")) {
                append("CLEAR AREA IMMEDIATELY - potential for sudden escalation.")
            }
        }
    }
}