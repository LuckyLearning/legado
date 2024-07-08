package io.legado.app.help

import androidx.annotation.Keep
import io.legado.app.constant.AppConst
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.channel
import io.legado.app.utils.jsonPath
import io.legado.app.utils.readString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import splitties.init.appCtx

@Keep
@Suppress("unused")
object AppUpdateGitHub : AppUpdate.AppUpdateInterface {

    private const val GITHUB_RELEASE_URL =
        "https://api.github.com/repos/LuckyLearning/legado/releases/latest"
    private const val TIMEOUT_DURATION = 10_000L

    override fun check(scope: CoroutineScope): Coroutine<AppUpdate.UpdateInfo> {
        return Coroutine.async(scope) {
            val body = withTimeout(TIMEOUT_DURATION) {
                okHttpClient.newCallStrResponse {
                    url(GITHUB_RELEASE_URL)
                }.body
            }

            if (body.isNullOrBlank()) {
                throw NoStackTraceException("获取新版本失败，响应体为空")
            }

            val rootDoc = jsonPath.parse(body)
            val tagName = rootDoc.readString("$.tag_name")
                ?: throw NoStackTraceException("获取新版本失败，标签名为空")

            if (tagName > AppConst.appInfo.versionName) {
                val updateBody = rootDoc.readString("$.body")
                    ?: throw NoStackTraceException("获取新版本失败，更新内容为空")

                val path = "\$.assets[?(@.name =~ /legado_${appCtx.channel}_.*?apk\$/)]"

                val downloadUrl =
                    "https://cdn.jsdelivr.net/gh/LuckyLearning/legado@latest/apk/legado_app_$tagName.apk"

                val fileName = rootDoc.read<List<String>>("${path}.name").firstOrNull()
                    ?: throw NoStackTraceException("获取新版本失败，文件名为空")

                return@async AppUpdate.UpdateInfo(tagName, updateBody, downloadUrl, fileName)
            } else {
                val currentVersion = AppConst.appInfo.versionName
                throw NoStackTraceException("已是最新版本\n当前版本: $currentVersion\n最新版本: $tagName")
            }
        }
    }
}
