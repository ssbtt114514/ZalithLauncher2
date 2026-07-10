package com.movtery.zalithlauncher.game.plugin.renderer_v2

import com.tencent.mmkv.MMKV

/**
 * V2 渲染器插件配置
 */
fun rendererPluginMMKV(): MMKV = MMKV.mmkvWithID("V2_RPConfig", MMKV.MULTI_PROCESS_MODE)
