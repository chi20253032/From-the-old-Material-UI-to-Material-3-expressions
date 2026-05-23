package com.example.converter

import com.example.ui.theme.GlassColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object TranslationEngine {

    // Helper to run deterministic XML translating locally
    fun translateXmlLocally(xmlContent: String): String {
        if (xmlContent.isBlank()) return "// [Empty input xml]"
        
        var compose = xmlContent.trim()
        
        // Remove XML Declaration
        compose = compose.replace(Regex("<\\?xml[^>]*>"), "")
        
        // Match layouts to respective containers
        compose = compose.replace(Regex("<LinearLayout[^>]*android:orientation=\"vertical\"[^>]*>"), "Column(\n    modifier = Modifier.fillMaxWidth().padding(16.dp),\n    verticalArrangement = Arrangement.spacedBy(12.dp)\n) {")
        compose = compose.replace(Regex("<LinearLayout[^>]*android:orientation=\"horizontal\"[^>]*>"), "Row(\n    modifier = Modifier.fillMaxWidth().padding(16.dp),\n    horizontalArrangement = Arrangement.spacedBy(12.dp),\n    verticalAlignment = Alignment.CenterVertically\n) {")
        compose = compose.replace(Regex("<LinearLayout[^>]*>"), "Column(\n    modifier = Modifier.fillMaxWidth().padding(16.dp),\n    verticalArrangement = Arrangement.spacedBy(12.dp)\n) {")
        compose = compose.replace(Regex("<RelativeLayout[^>]*>|<FrameLayout[^>]*>|<androidx.constraintlayout.widget.ConstraintLayout[^>]*>"), "Box(\n    modifier = Modifier.fillMaxSize().padding(16.dp)\n) {")
        
        // Convert common widgets (Material 2 and standard XML widgets to Liquid Glass M3)
        compose = compose.replace(Regex("<TextView[^>]*android:text=\"([^\"]*)\"[^>]*/>"), "Text(\n    text = \"$1\",\n    style = MaterialTheme.typography.bodyLarge,\n    color = Color.White\n)")
        compose = compose.replace(Regex("<TextView[^>]*android:text=\"([^\"]*)\"[^>]*>.*?</TextView>", RegexOption.DOT_MATCHES_ALL), "Text(\n    text = \"$1\",\n    style = MaterialTheme.typography.titleMedium,\n    color = Color.White\n)")
        
        // CardView -> GlassCard
        compose = compose.replace(Regex("<androidx.cardview.widget.CardView[^>]*>|<CardView[^>]*>"), "GlassCard(\n    modifier = Modifier.fillMaxWidth()\n) {")
        
        // Button -> GlassButton
        compose = compose.replace(Regex("<Button[^>]*android:text=\"([^\"]*)\"[^>]*/>"), "GlassButton(onClick = { /* Handle Click */ }) {\n    Text(\"$1\", color = Color.White)\n}")
        compose = compose.replace(Regex("<Button[^>]*android:text=\"([^\"]*)\"[^>]*>.*?</Button>", RegexOption.DOT_MATCHES_ALL), "GlassButton(onClick = { /* Handle Click */ }) {\n    Text(\"$1\", color = Color.White)\n}")
        
        // EditText -> GlassTextField
        compose = compose.replace(Regex("<EditText[^>]*android:hint=\"([^\"]*)\"[^>]*/>"), "var textState by remember { mutableStateOf(\"\") }\nGlassTextField(value = textState, onValueChange = { textState = it }, placeholder = \"$1\", modifier = Modifier.fillMaxWidth())")
        compose = compose.replace(Regex("<EditText[^>]*android:hint=\"([^\"]*)\"[^>]*>.*?</EditText>", RegexOption.DOT_MATCHES_ALL), "var textState by remember { mutableStateOf(\"\") }\nGlassTextField(value = textState, onValueChange = { textState = it }, placeholder = \"$1\", modifier = Modifier.fillMaxWidth())")
        
        // RecyclerView -> LazyColumn
        compose = compose.replace(Regex("<androidx.recyclerview.widget.RecyclerView[^>]*/>|<RecyclerView[^>]*/>"), "LazyColumn(\n    modifier = Modifier.fillMaxSize(),\n    verticalArrangement = Arrangement.spacedBy(8.dp)\n) {\n    items(10) { index ->\n        GlassCard(modifier = Modifier.fillMaxWidth()) {\n            Text(\"Sample Item #\$index\", color = Color.White)\n        }\n    }\n}")
        
        // BottomNavigationView -> GlassBottomBar
        compose = compose.replace(Regex("<com.google.android.material.bottomnavigation.BottomNavigationView[^>]*/>"), "var selectedTab by remember { mutableStateOf(0) }\nGlassBottomBar {\n    GlassBottomNavigationItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, contentDescription = \"Home\") }, label = \"Home\")\n    GlassBottomNavigationItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Settings, contentDescription = \"Settings\") }, label = \"Settings\")\n}")

        // Replace closing tags
        compose = compose.replace("</LinearLayout>", "}")
        compose = compose.replace("</RelativeLayout>", "}")
        compose = compose.replace("</FrameLayout>", "}")
        compose = compose.replace("</androidx.constraintlayout.widget.ConstraintLayout>", "}")
        compose = compose.replace("</androidx.cardview.widget.CardView>", "}")
        compose = compose.replace("</CardView>", "}")

        // Final cleanup of unmatched layout tags
        compose = compose.replace(Regex("</[a-zA-Z0-9.-_]+>"), "}")
        
        // Formatting wrapper
        return """
// [Offline XML Conversion Engine]
// Automatically generated via rule-base
@Composable
fun ConvertedComponent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        $compose
    }
}
        """.trimIndent()
    }

    // Call Gemini v1beta model for advanced code conversion
    suspend fun translateWithGemini(
        xmlContent: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext """
// [No Valid Gemini API Key Found]
// Falling back to Local Rule-Based parser. To enable advanced Generative AI translation,
// save your API key in the Secrets panel in AI Studio with key 'GEMINI_API_KEY'.

${translateXmlLocally(xmlContent)}
            """.trimIndent()
        }

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            // System prompt instructing conversion to custom Liquid Glass components
            val systemPrompt = """
You are an expert Android compiler and Jetpack Compose migration tool.
Your job is to decompile and translate legacy Material Design 2 XML layouts, full activities, or old Java layouts into gorgeous, interactive Jetpack Compose components styled in "Material 3 Expressive + Liquid Glass" theme.

The app uses a set of custom Liquid Glass components. You MUST translate XML tag structures to use these EXACT components:
1. GlassCard(modifier: Modifier) { ... }
2. GlassButton(onClick: () -> Unit, modifier: Modifier) { Text("...") }
3. GlassTextField(value: String, onValueChange: (String) -> Unit, placeholder: String)
4. GlassBottomBar { GlassBottomNavigationItem(...) ... }
5. LiquidFlowBackground { ... } (gorgeous dynamic animated fluid rendering)

Example translating:
- `<Button android:text="Submit" />` translates to:
  `GlassButton(onClick = { /* action */ }) { Text("Submit", fontWeight = FontWeight.Bold, color = Color.White) }`
- `<EditText android:hint="Search" />` translates to:
  `var text by remember { mutableStateOf("") }\nGlassTextField(value = text, onValueChange = { text = it }, placeholder = "Search")`
- Layout Containers (LinearLayout, RelativeLayout, FrameLayout, ScrollView) translate to standard Compose Layouts (`Column`, `Row`, `Box`, `LazyColumn`).
- Apply beautiful visual design accents: use colorful glass borders, semi-transparent overlays, and dynamic visual rhythms.

Return ONLY valid Kotlin Jetpack Compose code. Use Kotlin's state management (`remember { mutableStateOf }`) where mutable parameters are required. Don't write any Markdown formatting markers (like ```kotlin) around your code, just return raw Kotlin text.
            """.trim()

            val requestBody = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partText = JSONObject()
            partText.put("text", "Decompile/translate this legacy code into high-fidelity Compose code using custom Liquid Glass components:\n\n$xmlContent")
            partsArray.put(partText)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestBody.put("contents", contentsArray)

            // System instructions
            val systemInstructionObj = JSONObject()
            val sysPartsArray = JSONArray()
            sysPartsArray.put(JSONObject().put("text", systemPrompt))
            systemInstructionObj.put("parts", sysPartsArray)
            requestBody.put("systemInstruction", systemInstructionObj)

            // Generation config
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.3)
            requestBody.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBody.toString().toRequestBody(mediaType)

            // gemini-3.5-flash is resolved for code generation
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        var textResult = parts.getJSONObject(0).optString("text")
                        // Clean code markdown tags just in case
                        if (textResult.startsWith("```kotlin")) {
                            textResult = textResult.removePrefix("```kotlin")
                        } else if (textResult.startsWith("```")) {
                            textResult = textResult.removePrefix("```")
                        }
                        if (textResult.endsWith("```")) {
                            textResult = textResult.removeSuffix("```")
                        }
                        return@withContext textResult.trim()
                    }
                }
                "// Generated response structure was not understood. Fallback to Local conversion:\n\n${translateXmlLocally(xmlContent)}"
            } else {
                "// Gemini API Error (${response.code}). Fallback to Local conversion:\n\n${translateXmlLocally(xmlContent)}"
            }
        } catch (e: Exception) {
            "// API Connection failed: ${e.message}. Fallback to Local conversion:\n\n${translateXmlLocally(xmlContent)}"
        }
    }
}
