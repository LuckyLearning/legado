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
        // 首先尝试使用 GitHub API
        try {
            val lastReleaseUrl = "https://github.com/LuckyLearning/legado/releases/latest"
            
            val res = okHttpClient.newCallResponse {
                url(lastReleaseUrl)
                header("Accept", "application/vnd.github.v3+json")
            }
            
            if (res.isSuccessful) {
                val body = res.body.text()
                if (body.isBlank()) {
                    throw NoStackTraceException("获取新版本出错")
                }
                return GSON.fromJsonObject<GithubRelease>(body)
                    .getOrElse {
                        throw NoStackTraceException("获取新版本出错 " + it.localizedMessage)
                    }
                    .gitReleaseToAppReleaseInfo()
                    .sortedByDescending { it.createdAt }
            }
        } catch (e: Exception) {
            // API 请求失败，尝试从 HTML 页面获取
        }
        
        // 从 HTML 页面获取版本信息
        return getLatestReleaseFromHtml()
    }
    
    private suspend fun getLatestReleaseFromHtml(): List<AppReleaseInfo> {
        val releasePageUrl = "https://github.com/LuckyLearning/legado/releases/latest"
        
        val res = okHttpClient.newCallResponse {
            url(releasePageUrl)
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        }
        
        if (!res.isSuccessful) {
            throw NoStackTraceException("获取新版本出错(${res.code})")
        }
        
        // 从重定向后的 URL 中提取版本号
        val finalUrl = res.request.url.toString()
        val versionRegex = Regex("tag/([^/]+)")
        val versionMatch = versionRegex.find(finalUrl)
        val version = versionMatch?.groupValues?.get(1) ?: throw NoStackTraceException("未找到版本信息")
        
        val releaseInfos = mutableListOf<AppReleaseInfo>()
        
        // 构建 APK 下载链接
        val downloadUrl = "https://github.com/LuckyLearning/legado/releases/download/$version/legado_app_$version.apk"
        val fileName = "legado_app_$version.apk"
        
        // 确定应用变体
        val appVariant = when {
            fileName.contains("beta") -> AppVariant.BETA_RELEASE
            fileName.contains("betaA") -> AppVariant.BETA_RELEASEA
            else -> AppVariant.OFFICIAL
        }
        
        releaseInfos.add(
            AppReleaseInfo(
                appVariant = appVariant,
                createdAt = System.currentTimeMillis(),
                note = "",
                name = fileName,
                downloadUrl = downloadUrl,
                assetUrl = downloadUrl // 使用下载链接作为 assetUrl
            )
        )
        
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
