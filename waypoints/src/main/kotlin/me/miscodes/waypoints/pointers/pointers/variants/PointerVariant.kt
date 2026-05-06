package me.miscodes.waypoints.pointers.variants

import de.md5lukas.schedulers.AbstractScheduler
import me.miscodes.waypoints.pointers.Pointer
import me.miscodes.waypoints.pointers.PointerManager
import me.miscodes.waypoints.pointers.config.PointerConfiguration
import org.bukkit.entity.Player

enum class PointerVariant(
    val key: String,
    val isEnabled: (PointerConfiguration) -> Boolean,
    internal val create: (PointerManager, Player, AbstractScheduler) -> Pointer,
) {
  HOLOGRAM("hologram", { it.hologram.enabled }, ::HologramPointer);

  internal fun canUse(enabledPointerVariants: Set<PointerVariant>) =
      enabledPointerVariants.isEmpty() || this in enabledPointerVariants
}
