package me.miscodes.waypoints.util

import de.md5lukas.commons.paper.placeholder
import de.md5lukas.commons.paper.placeholderIgnoringArguments
import kotlin.math.roundToInt
import me.miscodes.waypoints.WaypointsPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Location
import org.bukkit.entity.Player

private val subscriptDigits = charArrayOf('₀', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉')

fun Int.toSubscript(): String =
    toString().map { c -> if (c.isDigit()) subscriptDigits[c - '0'] else c }.joinToString("")

fun Location.getResolvers(
    plugin: WaypointsPlugin,
    player: Player,
    translatedTarget: Location,
): Array<TagResolver> {
  val base = ArrayList<TagResolver>(9)
  base.add("world" placeholder plugin.worldTranslations.getWorldName(world!!))
  if (player.world === translatedTarget.world) {
    val dist = player.location.distance(translatedTarget)
    base.add("distance" placeholder dist)
    base.add(
        "hologram_distance" placeholderIgnoringArguments
            Component.text(dist.roundToInt().toSubscript())
    )
  } else {
    base.add(
        "distance" placeholderIgnoringArguments plugin.translations.TEXT_DISTANCE_OTHER_WORLD.text
    )
    base.add(
        "hologram_distance" placeholderIgnoringArguments
            plugin.translations.TEXT_DISTANCE_OTHER_WORLD.text
    )
  }
  base.add("x" placeholder x)
  base.add("y" placeholder y)
  base.add("z" placeholder z)
  base.add("block_x" placeholder blockX)
  base.add("block_y" placeholder blockY)
  base.add("block_z" placeholder blockZ)
  return base.toTypedArray()
}

fun Component.asPlainText(): String = PlainTextComponentSerializer.plainText().serialize(this)
