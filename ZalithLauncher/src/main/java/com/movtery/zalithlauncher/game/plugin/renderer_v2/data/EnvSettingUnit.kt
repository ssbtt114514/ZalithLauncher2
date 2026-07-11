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

import com.movtery.zalithlauncher.setting.unit.AbstractSettingUnit

/**
 * 渲染器可配置环境变量的设置单元
 * @param mmkvKey MMKV 存储键，格式为 packageName:envKey
 * @param rawEnv 原始环境变量配置
 * @param defaultValue 默认环境变量值
 * @param values 该环境变量所有支持的配置
 * @param summary 该可配置环境变量的描述文本，由插件提供
 */
class EnvSettingUnit(
    mmkvKey: String,
    val rawEnv: RendererConfig.Env.EditableEnv,
    defaultValue: String,
    val values: List<String>,
    val summary: String? = null,
) : AbstractSettingUnit<String>(mmkvKey, defaultValue) {

    override fun getValue(): String {
        return rendererEnvMMKV().getString(key, defaultValue)!!
            .also { state = it }
    }

    override fun saveValue(v: String): String {
        rendererEnvMMKV().putString(key, v).apply()
        return v
    }
}
