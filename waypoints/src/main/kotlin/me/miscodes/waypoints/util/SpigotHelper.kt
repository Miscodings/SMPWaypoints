package me.miscodes.waypoints.util

import java.time.Duration
import net.kyori.adventure.sound.Sound
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun parseLocationString(player: Player, input: String): Location? {
  val parts = input.split(' ')

  if (parts.size != 3) {
    return null
  }

  return try {
    Location(player.world, parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
  } catch (_: NumberFormatException) {
    null
  }
}

var ItemStack.amountClamped: Int
  get() = amount
  set(value) {
    amount = value.coerceIn(1, maxStackSize)
  }

fun Player.playSoundSeeded(sound: Sound) {
  playSound(Sound.sound(sound).seed(System.currentTimeMillis()).build())
}

/** One tick takes 50ms at a tick-rate of 20 TPS */
fun Duration.toMinecraftTicks(): Long = toMillis() / 50
