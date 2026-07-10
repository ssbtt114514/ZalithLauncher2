/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.plugin.renderer_v2

import android.content.Context
import android.content.pm.PackageManager
import com.movtery.zalithlauncher.path.GLOBAL_JSON
import com.movtery.zalithlauncher.utils.logging.Logger

object RendererV2PluginManager {
    private const val TAG = "RendererV2Plugin"
    /** 正在导入中的渲染器插件 */
    private var importingPlugin: RendererV2Plugin? = null

    fun clearPlugin() {
        importingPlugin = null
    }

    /**
     * 验证插件，反序列化插件配置 JSON 并导入
     */
    fun deserialize(context: Context, senderPackageName: String, configJson: String) {
        // 包名是否对应已安装的应用
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(senderPackageName, 0)
        }.getOrNull()

        if (packageInfo == null) {
            Logger.warning(TAG, "Verification failed: Package name $senderPackageName is not installed.")
            return
        }

        // 验证是否声明了 fclPlugin_V2
        val appInfo = runCatching {
            context.packageManager.getApplicationInfo(senderPackageName, PackageManager.GET_META_DATA)
        }.getOrNull()

        val metaData = appInfo?.metaData
        if (metaData == null || !metaData.getBoolean("fclPlugin_V2", false)) {
            Logger.warning(TAG, "Verification failed: $senderPackageName does not declare fclPlugin_V2")
            return
        }

        // 解析渲染器配置
        runCatching {
            GLOBAL_JSON.decodeFromString<RendererConfigList>(configJson)
        }.onFailure { e ->
            Logger.error(TAG, "JSON parsing failed.", e)
        }.getOrNull()?.let { config ->
            importingPlugin = RendererV2Plugin(
                packageName = senderPackageName,
                config = config
            )
        }
    }
}
