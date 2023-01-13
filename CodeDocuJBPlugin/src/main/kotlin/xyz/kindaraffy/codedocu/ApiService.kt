package xyz.kindaraffy.codedocu

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL


// Plugin conflict with IntelliJ API.
// Ignore till JB fixes this.
@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class Completion(
    val text: String,
    val index: Long,
    val logprobs: Long?,
    val finish_reason: String
)

// Plugin conflict with IntelliJ API.
// Ignore till JB fixes this.
@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class ApiResponse(
    val choices: List<Completion>,
    val usage: HashMap<String, Int>
)

data class ApiServiceOptions(val temperature: Float)

class ApiService(
    val comment: String,
    val options: ApiServiceOptions? = null
) {
    fun getBaseDomain(): String = "https://code-docu-api.kindaraffy.xyz"
    fun getBaseApi(): String = getBaseDomain() + "/api"

    fun getCompletion(): String {
        val response: String = this.sendAndReceive()
        val responseJson: ApiResponse = this.decodeResponse(response)
        return responseJson.choices[0].text
    }

    fun sendAndReceive(): String {
        val url = URL(getBaseApi())
        val connection = url.openConnection()
        connection.setRequestProperty("Comment", comment)
        return connection.inputStream.use { it.reader().readText() }
    }

    fun decodeResponse(response: String): ApiResponse {
        return Json.decodeFromString(ApiResponse.serializer(), response)
    }
}