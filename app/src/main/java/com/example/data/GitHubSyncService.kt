package com.example.data

import android.content.Context
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(GitHubPaperBankPayload::class.java).indent("  ")
    private val savedPaperPayloadAdapter = moshi.adapter(SavedPaperPayload::class.java)

    fun encodeSavedPaperPayload(payload: SavedPaperPayload): String {
        return savedPaperPayloadAdapter.toJson(payload)
    }

    fun decodeSavedPaperPayload(json: String): SavedPaperPayload? {
        return try {
            savedPaperPayloadAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun encodeBankToJson(payload: GitHubPaperBankPayload): String {
        return payloadAdapter.toJson(payload)
    }

    fun decodeBankFromJson(json: String): GitHubPaperBankPayload? {
        return try {
            payloadAdapter.fromJson(json)
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            GitHubSyncResult.Error("Network error while uploading to GitHub: ${e.localizedMessage ?: "Unknown error"}")
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
        } catch (e: Exception) {
            GitHubSyncResult.Error("Sync failed: ${e.localizedMessage ?: "Check internet connection"}")
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
