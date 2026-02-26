package io.legado.app.help.update

import androidx.annotation.Keep
import io.legado.app.constant.AppConst
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.newCallResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.coroutines.CoroutineScope

@Keep
@Suppress("unused")
object AppUpdateGitHub : AppUpdate.AppUpdateInterface {

    private val checkVariant: AppVariant
        get() = when (AppConfig.updateToVariant) {
            "official_version" -> AppVariant.OFFICIAL
            "beta_release_version" -> AppVariant.BETA_RELEASE
            "beta_releaseA_version" -> AppVariant.BETA_RELEASEA
            else -> AppConst.appInfo.appVariant
        }

    private suspend fun getLatestRelease(): List<AppReleaseInfo> {
        val releasePageUrl = "https://github.com/LuckyLearning/legado/releases/latest"
        
        val res = okHttpClient.newCallResponse {
            url(releasePageUrl)
        }
        
        if (!res.isSuccessful) {
            throw NoStackTraceException("获取新版本出错(${res.code})")
        }
        
        val body = res.body.text()
        if (body.isBlank()) {
            throw NoStackTraceException("获取新版本出错")
        }
        
        // 解析 HTML 页面获取版本信息
        return parseReleasePage(body)
    }
    
    private fun parseReleasePage(html: String): List<AppReleaseInfo> {
        // 提取版本号
        val versionRegex = Regex("<span class=\"text-bold\">([^<]+)</span>")
        val versionMatch = versionRegex.find(html)
        val version = versionMatch?.groupValues?.get(1) ?: throw NoStackTraceException("未找到版本信息")
        
        // 提取发布时间
        val dateRegex = Regex("<relative-time datetime=\"([^\"]+)\">")
        val dateMatch = dateRegex.find(html)
        val createdAtStr = dateMatch?.groupValues?.get(1) ?: ""
        val createdAt = if (createdAtStr.isNotEmpty()) {
            try {
                val instant = java.time.Instant.parse(createdAtStr)
                instant.toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } else {
            System.currentTimeMillis()
        }
        
        // 提取更新日志
        val changelogRegex = Regex("<div class=\"Box-body\"[^>]*>([\\s\\S]+?)</div>")
        val changelogMatch = changelogRegex.find(html)
        val note = changelogMatch?.groupValues?.get(1)?.replace(Regex("<[^>]+>"), "")?.trim() ?: ""
        
        // 提取下载链接
        val downloadRegex = Regex("<a href=\"(/LuckyLearning/legado/releases/download/[^\"]+\\.apk)\"[^>]*>")
        val downloadMatches = downloadRegex.findAll(html)
        
        val releaseInfos = mutableListOf<AppReleaseInfo>()
        
        downloadMatches.forEach { match ->
            val downloadUrl = "https://github.com" + match.groupValues[1]
            val fileName = downloadUrl.substringAfterLast("/")
            
            // 确定应用变体
            val appVariant = when {
                fileName.contains("beta") -> AppVariant.BETA_RELEASE
                fileName.contains("betaA") -> AppVariant.BETA_RELEASEA
                else -> AppVariant.OFFICIAL
            }
            
            releaseInfos.add(
                AppReleaseInfo(
                    appVariant = appVariant,
                    createdAt = createdAt,
                    note = note,
                    name = fileName,
                    downloadUrl = downloadUrl,
                    assetUrl = downloadUrl // 使用下载链接作为 assetUrl
                )
            )
        }
        
        if (releaseInfos.isEmpty()) {
            throw NoStackTraceException("未找到下载链接")
        }
        
        return releaseInfos.sortedByDescending { it.createdAt }
    }

    override fun check(
        scope: CoroutineScope,
    ): Coroutine<AppUpdate.UpdateInfo> {
        return Coroutine.async(scope) {
            getLatestRelease()
                .filter { it.appVariant == checkVariant }
                .firstOrNull { it.versionName > AppConst.appInfo.versionName }
                ?.let {
                    return@async AppUpdate.UpdateInfo(
                        it.versionName,
                        it.note,
                        it.downloadUrl,
                        it.name
                    )
                }
                ?: throw NoStackTraceException("已是最新版本")
        }.timeout(10000)
    }
}
