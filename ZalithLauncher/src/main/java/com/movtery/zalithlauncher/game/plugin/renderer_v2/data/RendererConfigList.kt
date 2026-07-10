package com.movtery.zalithlauncher.game.plugin.renderer_v2.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RendererConfigList(
    @SerialName("data")
    val data: List<RendererConfig>
)