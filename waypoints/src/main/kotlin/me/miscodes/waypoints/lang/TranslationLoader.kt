package me.miscodes.waypoints.lang

import me.miscodes.waypoints.WaypointsPlugin
import net.kyori.adventure.text.minimessage.MiniMessage

interface TranslationLoader {

  val plugin: WaypointsPlugin

  val itemMiniMessage: MiniMessage

  operator fun get(key: String): String

  fun getBoolean(key: String): Boolean

  fun registerTranslationWrapper(translation: AbstractTranslation)
}
