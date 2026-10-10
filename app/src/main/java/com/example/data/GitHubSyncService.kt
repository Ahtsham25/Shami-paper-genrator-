package com.example.data

import android.content.Context
import android.util.Base64
import com.example.BuildConfig
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

data class GitHubRepoConfig(
    val owner: String,
    val repo: String,
    val branch: String = "main",
    val filePath: String = "data/shami_paper_bank.json",
    val token: String = "",
    val customRawUrl: String = ""
)

sealed class GitHubSyncResult {
    data class Success(val message: String, val payload: GitHubPaperBankPayload? = null) : GitHubSyncResult()
    data class Error(val errorMessage: String) : GitHubSyncResult()
}

class GitHubSyncService(private val context: Context) {

    private val prefs = context.getSharedPreferences("github_sync_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun questionToJson(q: QuestionEntity): JSONObject {
        return JSONObject().apply {
            put("id", q.id)
            put("chapterId", q.chapterId)
            put("subjectId", q.subjectId)
            put("classLevel", q.classLevel)
            put("type", q.type)
            put("questionEn", q.questionEn)
            put("questionUr", q.questionUr)
            put("optionAEn", q.optionAEn)
            put("optionBEn", q.optionBEn)
            put("optionCEn", q.optionCEn)
            put("optionDEn", q.optionDEn)
            put("optionAUr", q.optionAUr)
            put("optionBUr", q.optionBUr)
            put("optionCUr", q.optionCUr)
            put("optionDUr", q.optionDUr)
            put("correctOption", q.correctOption)
            put("marks", q.marks)
            put("sortOrder", q.sortOrder)
        }
    }

    private fun jsonToQuestion(obj: JSONObject): QuestionEntity {
        val type = obj.optString("type", "SHORT")
        val defaultMarks = when (type.uppercase()) {
            "MCQ" -> 1
            "LONG" -> 5
            else -> 2
        }
        return QuestionEntity(
            id = obj.optString("id", ""),
            chapterId = obj.optString("chapterId", ""),
            subjectId = obj.optString("subjectId", ""),
            classLevel = obj.optString("classLevel", "9"),
            type = type,
            questionEn = obj.optString("questionEn", ""),
            questionUr = obj.optString("questionUr", ""),
            optionAEn = obj.optString("optionAEn", ""),
            optionBEn = obj.optString("optionBEn", ""),
            optionCEn = obj.optString("optionCEn", ""),
            optionDEn = obj.optString("optionDEn", ""),
            optionAUr = obj.optString("optionAUr", ""),
            optionBUr = obj.optString("optionBUr", ""),
            optionCUr = obj.optString("optionCUr", ""),
            optionDUr = obj.optString("optionDUr", ""),
            correctOption = obj.optString("correctOption", "A").ifBlank { "A" },
            marks = obj.optInt("marks", defaultMarks),
            sortOrder = obj.optInt("sortOrder", 0)
        )
    }

    fun encodeSavedPaperPayload(payload: SavedPaperPayload): String {
        val root = JSONObject()
        val h = payload.header
        val headerObj = JSONObject().apply {
            put("institutionName", h.institutionName)
            put("examTitle", h.examTitle)
            put("studentNameValue", h.studentNameValue)
            put("rollNumberValue", h.rollNumberValue)
            put("classLabel", h.classLabel)
            put("subjectNameEn", h.subjectNameEn)
            put("subjectNameUr", h.subjectNameUr)
            put("timeAllowed", h.timeAllowed)
            put("languageMode", h.languageMode)
            put("paperVersion", h.paperVersion)
            put("paperSize", h.paperSize)
            put("fontSizeScale", h.fontSizeScale)
            put("fontSizePt", h.fontSizePt)
            put("includeAnswerKey", h.includeAnswerKey)
        }
        root.put("header", headerObj)
        root.put("mcqs", JSONArray().apply { payload.mcqs.forEach { put(questionToJson(it)) } })
        root.put("shortQuestions", JSONArray().apply { payload.shortQuestions.forEach { put(questionToJson(it)) } })
        root.put("longQuestions", JSONArray().apply { payload.longQuestions.forEach { put(questionToJson(it)) } })
        return root.toString()
    }

    fun decodeSavedPaperPayload(json: String): SavedPaperPayload? {
        return try {
            val root = JSONObject(json)
            val hObj = root.optJSONObject("header") ?: JSONObject()
            val header = PaperHeaderConfig(
                institutionName = hObj.optString("institutionName", "SHAMI ACADEMY"),
                examTitle = hObj.optString("examTitle", "Term Examination"),
                studentNameValue = hObj.optString("studentNameValue", ""),
                rollNumberValue = hObj.optString("rollNumberValue", ""),
                classLabel = hObj.optString("classLabel", "Class 9th"),
                subjectNameEn = hObj.optString("subjectNameEn", "Chemistry"),
                subjectNameUr = hObj.optString("subjectNameUr", "کیمسٹری"),
                timeAllowed = hObj.optString("timeAllowed", "2:00 Hours"),
                languageMode = hObj.optString("languageMode", PaperLanguage.ENGLISH.code),
                paperVersion = hObj.optInt("paperVersion", 1),
                paperSize = hObj.optString("paperSize", "A4"),
                fontSizeScale = hObj.optString("fontSizeScale", "MEDIUM"),
                fontSizePt = hObj.optInt("fontSizePt", 12),
                includeAnswerKey = hObj.optBoolean("includeAnswerKey", true)
            )
            fun parseQList(arr: JSONArray?): List<QuestionEntity> {
                if (arr == null) return emptyList()
                val list = mutableListOf<QuestionEntity>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    list.add(jsonToQuestion(item))
                }
                return list
            }
            SavedPaperPayload(
                header = header,
                mcqs = parseQList(root.optJSONArray("mcqs")),
                shortQuestions = parseQList(root.optJSONArray("shortQuestions")),
                longQuestions = parseQList(root.optJSONArray("longQuestions"))
            )
        } catch (t: Throwable) {
            null
        }
    }

    fun encodeBankToJson(payload: GitHubPaperBankPayload): String {
        val root = JSONObject()
        root.put("version", payload.version)
        root.put("updatedBy", payload.updatedBy)
        root.put("updatedAt", payload.updatedAt)
        payload.adConfig?.let { ad ->
            val adObj = JSONObject().apply {
                put("adsEnabled", ad.adsEnabled)
                put("useTestAds", ad.useTestAds)
                put("appId", ad.appId)
                put("bannerAdUnitId", ad.bannerAdUnitId)
                put("interstitialAdUnitId", ad.interstitialAdUnitId)
                put("rewardedAdUnitId", ad.rewardedAdUnitId)
                put("updatedAt", ad.updatedAt)
            }
            root.put("adConfig", adObj)
        }
        val subjectsArr = JSONArray()
        for (s in payload.subjects) {
            subjectsArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("classLevel", s.classLevel)
                    put("orderIndex", s.orderIndex)
                    put("nameEn", s.nameEn)
                    put("nameUr", s.nameUr)
                    put("iconKey", s.iconKey)
                    put("colorHex", s.colorHex)
                    put("isFreeByDefault", s.isFreeByDefault)
                }
            )
        }
        root.put("subjects", subjectsArr)

        val chaptersArr = JSONArray()
        for (c in payload.chapters) {
            chaptersArr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("subjectId", c.subjectId)
                    put("classLevel", c.classLevel)
                    put("chapterNumber", c.chapterNumber)
                    put("titleEn", c.titleEn)
                    put("titleUr", c.titleUr)
                    put("isFreeByDefault", c.isFreeByDefault)
                }
            )
        }
        root.put("chapters", chaptersArr)

        val questionsArr = JSONArray()
        for (q in payload.questions) {
            questionsArr.put(questionToJson(q))
        }
        root.put("questions", questionsArr)

        return root.toString(2)
    }

    fun decodeBankFromJson(json: String): GitHubPaperBankPayload? {
        return try {
            val root = JSONObject(json)
            val version = root.optInt("version", 1)
            val updatedBy = root.optString("updatedBy", "Paper Maker by Shami Academy Admin")
            val updatedAt = root.optLong("updatedAt", System.currentTimeMillis())

            val adObj = root.optJSONObject("adConfig")
            val adConfig = if (adObj != null) {
                RemoteAdConfig(
                    adsEnabled = adObj.optBoolean("adsEnabled", true),
                    useTestAds = adObj.optBoolean("useTestAds", true),
                    appId = adObj.optString("appId", "ca-app-pub-3940256099942544~3347511713"),
                    bannerAdUnitId = adObj.optString("bannerAdUnitId", "ca-app-pub-3940256099942544/6300978111"),
                    interstitialAdUnitId = adObj.optString("interstitialAdUnitId", "ca-app-pub-3940256099942544/1033173712"),
                    rewardedAdUnitId = adObj.optString("rewardedAdUnitId", "ca-app-pub-3940256099942544/5224354917"),
                    updatedAt = adObj.optLong("updatedAt", 0L)
                )
            } else null

            val subjects = mutableListOf<SubjectEntity>()
            val sArr = root.optJSONArray("subjects")
            if (sArr != null) {
                for (i in 0 until sArr.length()) {
                    val sObj = sArr.optJSONObject(i) ?: continue
                    val orderIdx = sObj.optInt("orderIndex", i)
                    subjects.add(
                        SubjectEntity(
                            id = sObj.optString("id", ""),
                            classLevel = sObj.optString("classLevel", "9"),
                            orderIndex = orderIdx,
                            nameEn = sObj.optString("nameEn", ""),
                            nameUr = sObj.optString("nameUr", ""),
                            iconKey = sObj.optString("iconKey", "menu_book"),
                            colorHex = sObj.optString("colorHex", "#0D9488"),
                            isFreeByDefault = sObj.optBoolean("isFreeByDefault", orderIdx == 0)
                        )
                    )
                }
            }

            val chapters = mutableListOf<ChapterEntity>()
            val cArr = root.optJSONArray("chapters")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    val cObj = cArr.optJSONObject(i) ?: continue
                    val chNum = cObj.optInt("chapterNumber", i + 1)
                    chapters.add(
                        ChapterEntity(
                            id = cObj.optString("id", ""),
                            subjectId = cObj.optString("subjectId", ""),
                            classLevel = cObj.optString("classLevel", "9"),
                            chapterNumber = chNum,
                            titleEn = cObj.optString("titleEn", ""),
                            titleUr = cObj.optString("titleUr", ""),
                            isFreeByDefault = cObj.optBoolean("isFreeByDefault", chNum == 1)
                        )
                    )
                }
            }

            val questions = mutableListOf<QuestionEntity>()
            val qArr = root.optJSONArray("questions")
            if (qArr != null) {
                for (i in 0 until qArr.length()) {
                    val qObj = qArr.optJSONObject(i) ?: continue
                    questions.add(jsonToQuestion(qObj))
                }
            }

            GitHubPaperBankPayload(
                version = version,
                updatedBy = updatedBy,
                updatedAt = updatedAt,
                adConfig = adConfig,
                subjects = subjects,
                chapters = chapters,
                questions = questions
            )
        } catch (t: Throwable) {
            null
        }
    }

    fun getConfig(): GitHubRepoConfig {
        val defaultOwner = BuildConfig.GITHUB_OWNER.takeUnless { it == "YOUR_GITHUB_USERNAME" } ?: ""
        val defaultRepo = BuildConfig.GITHUB_REPO.takeUnless { it == "YOUR_GITHUB_REPO" } ?: ""
        val defaultBranch = BuildConfig.GITHUB_BRANCH.ifBlank { "main" }
        val defaultPath = BuildConfig.GITHUB_FILE_PATH.ifBlank { "data/shami_paper_bank.json" }
        val defaultToken = BuildConfig.GITHUB_TOKEN.takeUnless { it == "YOUR_GITHUB_TOKEN" } ?: ""

        return GitHubRepoConfig(
            owner = prefs.getString("gh_owner", defaultOwner) ?: defaultOwner,
            repo = prefs.getString("gh_repo", defaultRepo) ?: defaultRepo,
            branch = prefs.getString("gh_branch", defaultBranch) ?: defaultBranch,
            filePath = prefs.getString("gh_path", defaultPath) ?: defaultPath,
            token = prefs.getString("gh_token", defaultToken) ?: defaultToken,
            customRawUrl = prefs.getString("gh_raw_url", "") ?: ""
        )
    }

    fun saveConfig(config: GitHubRepoConfig) {
        prefs.edit()
            .putString("gh_owner", config.owner.trim())
            .putString("gh_repo", config.repo.trim())
            .putString("gh_branch", config.branch.trim().ifBlank { "main" })
            .putString("gh_path", config.filePath.trim().removePrefix("/").ifBlank { "data/shami_paper_bank.json" })
            .putString("gh_token", config.token.trim())
            .putString("gh_raw_url", config.customRawUrl.trim())
            .apply()
    }

    /**
     * Uploads/commits the JSON payload directly to the configured GitHub repository
     * using the GitHub Contents REST API: PUT /repos/{owner}/{repo}/contents/{path}
     */
    suspend fun pushBankToGitHub(
        config: GitHubRepoConfig,
        payload: GitHubPaperBankPayload,
        commitMessage: String = "Update Shami Academy Paper Bank via Admin Panel"
    ): GitHubSyncResult = withContext(Dispatchers.IO) {
        val owner = config.owner.trim()
        val repo = config.repo.trim()
        val branch = config.branch.trim().ifBlank { "main" }
        val path = config.filePath.trim().removePrefix("/").ifBlank { "data/shami_paper_bank.json" }
        val token = config.token.trim()

        if (owner.isBlank() || repo.isBlank()) {
            return@withContext GitHubSyncResult.Error("Please enter your GitHub Username (Owner) and Repository Name.")
        }
        if (token.isBlank()) {
            return@withContext GitHubSyncResult.Error("GitHub Personal Access Token (PAT) is required to upload/commit files to GitHub.")
        }

        val jsonContent = encodeBankToJson(payload)
        return@withContext uploadRawFileToGitHub(
            config = config.copy(owner = owner, repo = repo, branch = branch, filePath = path, token = token),
            fileBytes = jsonContent.toByteArray(Charsets.UTF_8),
            commitMessage = commitMessage
        )
    }

    /**
     * Uploads ANY file (JSON, HTML, TXT, etc.) directly to the GitHub repository.
     */
    suspend fun uploadRawFileToGitHub(
        config: GitHubRepoConfig,
        fileBytes: ByteArray,
        commitMessage: String
    ): GitHubSyncResult = withContext(Dispatchers.IO) {
        try {
            val owner = config.owner.trim()
            val repo = config.repo.trim()
            val branch = config.branch.trim().ifBlank { "main" }
            val path = config.filePath.trim().removePrefix("/")
            val token = config.token.trim()

            val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$path"

            // Step 1: Check if file already exists to retrieve its SHA
            var existingSha: String? = null
            val getReq = Request.Builder()
                .url("$apiUrl?ref=$branch")
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer $token")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .get()
                .build()

            client.newCall(getReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string().orEmpty()
                    if (bodyStr.isNotBlank()) {
                        val jsonObj = JSONObject(bodyStr)
                        if (jsonObj.has("sha")) {
                            existingSha = jsonObj.getString("sha")
                        }
                    }
                }
            }

            // Step 2: PUT base64 content
            val base64Content = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
            val bodyJson = JSONObject().apply {
                put("message", commitMessage)
                put("content", base64Content)
                put("branch", branch)
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putReq = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer $token")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .put(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(putReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    GitHubSyncResult.Success("Successfully uploaded '$path' to GitHub ($owner/$repo@$branch)!")
                } else {
                    val errBody = resp.body?.string().orEmpty()
                    GitHubSyncResult.Error("GitHub API Error (${resp.code}): ${errBody.take(180)}")
                }
            }
        } catch (t: Throwable) {
            GitHubSyncResult.Error("Network error while uploading to GitHub: ${t.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Pulls/fetches the latest paper bank JSON from GitHub (either via Custom Raw URL,
     * GitHub API, or raw.githubusercontent.com).
     */
    suspend fun fetchBankFromGitHub(config: GitHubRepoConfig): GitHubSyncResult = withContext(Dispatchers.IO) {
        try {
            val rawUrl = if (config.customRawUrl.isNotBlank()) {
                config.customRawUrl.trim()
            } else {
                val owner = config.owner.trim()
                val repo = config.repo.trim()
                val branch = config.branch.trim().ifBlank { "main" }
                val path = config.filePath.trim().removePrefix("/").ifBlank { "data/shami_paper_bank.json" }
                if (owner.isBlank() || repo.isBlank()) {
                    return@withContext GitHubSyncResult.Error("Enter GitHub Owner & Repo (or a Raw JSON URL) to sync.")
                }
                "https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
            }

            val reqBuilder = Request.Builder().url(rawUrl).get()
            if (config.token.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer ${config.token.trim()}")
            }

            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext GitHubSyncResult.Error("Failed to fetch from GitHub (${resp.code}). Verify the file exists at $rawUrl")
                }
                val bodyStr = resp.body?.string().orEmpty()
                val decoded = decodeBankFromJson(bodyStr)
                    ?: return@withContext GitHubSyncResult.Error("Invalid JSON format in remote GitHub file.")

                GitHubSyncResult.Success(
                    message = "Synced ${decoded.subjects.size} books, ${decoded.chapters.size} chapters, and ${decoded.questions.size} questions from GitHub!",
                    payload = decoded
                )
            }
        } catch (t: Throwable) {
            GitHubSyncResult.Error("Sync failed: ${t.localizedMessage ?: "Check internet connection"}")
        }
    }

    /**
     * Saves the exported JSON file locally so the admin can share or upload it anywhere.
     */
    suspend fun saveExportedJsonToFile(payload: GitHubPaperBankPayload): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "shami_paper_bank.json")
        file.writeText(encodeBankToJson(payload), Charsets.UTF_8)
        file
    }
}
