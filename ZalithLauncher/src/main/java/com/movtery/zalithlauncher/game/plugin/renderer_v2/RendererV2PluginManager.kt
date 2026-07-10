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
import com.movtery.zalithlauncher.game.plugin.renderer_v2.data.RendererConfigList
import com.movtery.zalithlauncher.path.GLOBAL_JSON
import com.movtery.zalithlauncher.utils.logging.Logger

object RendererV2PluginManager {
    private const val TAG = "RendererV2Plugin"
    private val rendererPluginList: MutableList<RendererV2Plugin> = mutableListOf()

    fun getRendererList(): List<RendererV2Plugin> = rendererPluginList

    fun clearPlugin() {
        rendererPluginList.clear()
    }

    /**
     * 从 MMKV 加载已保存的插件配置
     * 同时清理已卸载插件对应的残留配置
     */
    fun initialize(context: Context) {
        val mmkv = rendererPluginMMKV()
        val keys = mmkv.allKeys() ?: emptyArray()
        var removedCount = 0

        keys.forEach { key ->
            // 检查该插件是否存在
            val isInstalled = runCatching {
                context.packageManager.getPackageInfo(key, 0)
            }.isSuccess
            // 不存在则清除配置
            if (!isInstalled) {
                mmkv.remove(key)
                removedCount++
                Logger.info(TAG, "Removed stale config for uninstalled package: $key")
                return@forEach
            }

            val json = mmkv.getString(key, null) ?: return@forEach
            runCatching {
                val configList = GLOBAL_JSON.decodeFromString<RendererConfigList>(json)
                rendererPluginList.add(
                    RendererV2Plugin(packageName = key, config = configList)
                )
            }.onFailure {
                Logger.error(TAG, "Failed to parse config for $key, removing.", it)
                mmkv.remove(key)
                removedCount++
            }
        }

        Logger.debug(TAG, "Loaded ${keys.size - removedCount} plugin(s), removed $removedCount stale config(s).")
    }

    /**
     * 反序列化插件配置 JSON 并导入
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
            val plugin = RendererV2Plugin(
                packageName = senderPackageName,
                config = config
            )
            addPlugin(plugin)
            save(plugin)
        }
    }

    private fun addPlugin(plugin: RendererV2Plugin) {
        val existingIndex = rendererPluginList.indexOfFirst { it.packageName == plugin.packageName }
        if (existingIndex >= 0) {
            rendererPluginList[existingIndex] = plugin
        } else {
            rendererPluginList.add(plugin)
        }
    }

    private fun save(plugin: RendererV2Plugin) {
        runCatching {
            val json = GLOBAL_JSON.encodeToString(plugin.config)
            rendererPluginMMKV().encode(plugin.packageName, json)
            Logger.info(TAG, "Plugin configuration saved: ${plugin.packageName}")
        }.onFailure {
            Logger.error(TAG, "Failed to save plugin configuration: ${plugin.packageName}", it)
        }
    }
}
