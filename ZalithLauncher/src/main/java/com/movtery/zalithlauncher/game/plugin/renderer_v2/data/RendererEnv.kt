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

package com.movtery.zalithlauncher.game.plugin.renderer_v2.data

/**
 * 渲染器配置项状态、存储
 * @param packageName 插件包名，用于 MMKV 存储命名空间隔离
 * @param envs 渲染器环境变量配置项
 * @param genSummary 将 [RendererConfig.MetaString] 转换为本地化文本
 */
class RendererEnv(
    val packageName: String,
    val envs: List<RendererConfig.Env>,
    genSummary: (metaString: String) -> String?,
) {
    /**
     * 可配置环境变量的设置单元，键为原始环境变量键名
     */
    private val editableUnits: Map<String, EnvSettingUnit>

    init {
        val mmkv = rendererEnvMMKV()
        val prefix = "$packageName:"

        // 收集当前渲染器支持的所有可配置环境变量键
        val currentEditableKeys = envs
            .filterIsInstance<RendererConfig.Env.EditableEnv>()
            .map { it.key }
            .toSet()

        // 清理插件更新后不再受支持的环境变量
        mmkv.allKeys()
            ?.filter { it.startsWith(prefix) }
            ?.forEach { storedKey ->
                val envKey = storedKey.removePrefix(prefix)
                if (envKey !in currentEditableKeys) {
                    mmkv.remove(storedKey)
                }
        }

        // 为每个可配置环境变量创建设置单元
        val units = mutableMapOf<String, EnvSettingUnit>()
        for (env in envs) {
            if (env !is RendererConfig.Env.EditableEnv) continue

            val mmkvKey = "$prefix${env.key}"
            val unit = EnvSettingUnit(
                mmkvKey = mmkvKey,
                rawEnv = env,
                defaultValue = env.values.defaultValue,
                values = buildList {
                    // 将默认值作为配置项之一
                    add(env.values.defaultValue)
                    addAll(env.values.values)
                },
                summary = env.title?.key?.let { genSummary(it) }
            )
            unit.init()

            // 校验已保存的值是否仍在当前可选值列表中，不在则重置为默认值
            if (unit.state !in env.values.values) {
                unit.save(env.values.defaultValue)
            }

            units[env.key] = unit
        }
        editableUnits = units
    }

    /**
     * 获取该渲染器当前的环境变量配置
     */
    fun getEnv(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (env in envs) {
            when (env) {
                is RendererConfig.Env.NormalEnv -> result[env.key] = env.value
                is RendererConfig.Env.EditableEnv -> {
                    val unit = editableUnits[env.key]
                    if (unit != null) {
                        result[env.key] = unit.state
                    }
                }
            }
        }
        return result
    }

    /**
     * 获取所有可配置环境变量的设置单元列表
     */
    fun getEditableUnits(): List<EnvSettingUnit> = editableUnits.values.toList()
}
