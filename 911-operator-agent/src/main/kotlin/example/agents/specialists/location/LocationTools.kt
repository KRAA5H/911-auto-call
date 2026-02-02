package example.agents.specialists.location

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@LLMDescription("A collection of tools for the Location Agent (The Navigator).")
class LocationTools : ToolSet {

    private val client = OkHttpClient()

    private val w3wApiKey = System.getenv("W3W_API_KEY")
        ?: error("W3W_API_KEY environment variable is not set. Please set it first.")

    private val userAgent = "KoogAgent/1.0"

    @Tool
    @LLMDescription("Validates a spoken address against a GIS database to ensure it exists.")
    fun verifyAddress(
        @LLMDescription("The street address (e.g., '123 Main St')") streetAddress: String,
        @LLMDescription("The city or region (e.g., 'New York')") city: String
    ): String {
        val query = "$streetAddress, $city"
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Error: GIS Service unavailable (HTTP ${response.code})."

                val responseBody = response.body?.string() ?: return "Error: Empty response."
                val jsonArray = JSONArray(responseBody)

                if (jsonArray.length() == 0) {
                    return "Invalid: Address '$query' could not be found in the database. Please verify the street name."
                }

                val location = jsonArray.getJSONObject(0)
                val displayName = location.getString("display_name")
                val lat = location.getString("lat")
                val lon = location.getString("lon")

                return "Valid: Found '$displayName' at coordinates ($lat, $lon)."
            }
        } catch (e: Exception) {
            return "System Failure: Unable to connect to GIS provider. ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Identifies likely location based on visible landmarks when the user does not know their address.")
    fun reverseGeocodeLandmarks(
        @LLMDescription("Visual description (e.g., 'Near the big blue water tower')") description: String
    ): String {
        // This implementation treats the description
        // as a broad POI search.

        val encodedQuery = URLEncoder.encode(description, StandardCharsets.UTF_8.toString())
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (responseBody == null || responseBody == "[]") {
                    return "Uncertain: No official landmarks found matching '$description'. Try a street name."
                }

                val jsonArray = JSONArray(responseBody)
                val location = jsonArray.getJSONObject(0)
                val name = location.getString("display_name")

                return "Likely Location: Found landmark '$name'. confirm if this is correct."
            }
        } catch (e: Exception) {
            return "Error: Landmark search failed."
        }
    }

    @Tool
    @LLMDescription("Converts a 3-word location code (What3Words) into precise GPS coordinates.")
    fun lookupWhat3Words(
        @LLMDescription("The 3-word address format (e.g., 'table.lamp.spoon')") words: String
    ): String {
        // Requires a real API key to work.
        if (w3wApiKey == "YOUR_WHAT3WORDS_API_KEY") return "Configuration Error: W3W API Key missing."

        val url = "https://api.what3words.com/v3/convert-to-coordinates?words=$words&key=$w3wApiKey"

        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return "Error: Empty W3W response."
                val json = JSONObject(body)

                if (json.has("error")) {
                    val err = json.getJSONObject("error").getString("message")
                    return "Invalid: $err"
                }

                val coords = json.getJSONObject("coordinates")
                val lat = coords.getDouble("lat")
                val lng = coords.getDouble("lng")
                val nearestPlace = json.getString("nearestPlace")

                return "Confirmed: $words maps to ($lat, $lng) near $nearestPlace."
            }
        } catch (e: Exception) {
            return "System Failure: W3W Lookup failed."
        }
    }

    @Tool
    @LLMDescription("Calculates the estimated time of arrival (ETA) for the nearest emergency unit.")
    fun calculateUnitEta(
        @LLMDescription("The confirmed incident location") incidentLocation: String,
        @LLMDescription("The ID of the unit being dispatched (optional)") unitId: String? = "Auto-Assign"
    ): String {
        //To make this "Real", we must geocode the incident location first,
        // then route from a HYPOTHETICAL unit location (since we don't have a real unit database here).
        //We will assume Unit 4 is at a fixed depot for this implementation to actually run.

        val unitLat = 51.5007 // Mock Unit Location (London)
        val unitLon = -0.1246

        // 1. Quick/Dirty geocode of the incident string to get coordinates (Reusing logic is better, but keeping self-contained)
        val encodedDest = URLEncoder.encode(incidentLocation, StandardCharsets.UTF_8.toString())
        val geoUrl = "https://nominatim.openstreetmap.org/search?q=$encodedDest&format=json&limit=1"

        var destLat = 0.0
        var destLon = 0.0

        try {
            client.newCall(Request.Builder().url(geoUrl).header("User-Agent", userAgent).build()).execute()
                .use { resp ->
                    val jsonArr = JSONArray(resp.body!!.string())
                    if (jsonArr.length() == 0) return "Error: Cannot calculate ETA, incident location unknown."
                    destLat = jsonArr.getJSONObject(0).getDouble("lat")
                    destLon = jsonArr.getJSONObject(0).getDouble("lon")
                }
        } catch (e: Exception) {
            return "Error: Geocoding failed for ETA."
        }

        val routeUrl =
            "http://router.project-osrm.org/route/v1/driving/$unitLon,$unitLat;$destLon,$destLat?overview=false"

        try {
            client.newCall(Request.Builder().url(routeUrl).build()).execute().use { response ->
                val json = JSONObject(response.body!!.string())
                if (json.getString("code") != "Ok") return "Error: Routing failed."

                val route = json.getJSONArray("routes").getJSONObject(0)
                val durationSeconds = route.getDouble("duration")
                val distanceMeters = route.getDouble("distance")

                val minutes = (durationSeconds / 60).toInt()
                val km = String.format("%.1f", distanceMeters / 1000)

                return "Unit $unitId is $minutes minutes away ($km km) from $incidentLocation."
            }
        } catch (e: Exception) {
            return "Error: Could not retrieve routing data."
        }
    }
}