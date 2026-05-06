package me.miscodes.waypoints.gui

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import me.miscodes.waypoints.api.Type
import me.miscodes.waypoints.api.gui.GUIDisplayable
import me.miscodes.waypoints.api.gui.GUIType

object SharedDisplayable : GUIDisplayable {
  override val type: Type = Type.PUBLIC
  override val guiType: GUIType = GUIType.PUBLIC_HOLDER
  override val name: String = guiType.name
  override val createdAt: OffsetDateTime = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))
}
