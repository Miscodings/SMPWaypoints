package me.miscodes.waypoints.api.gui

import java.time.OffsetDateTime
import me.miscodes.waypoints.api.Type

/** Interface for the internal GUI code providing information on how to sort an item */
interface GUIDisplayable {

  /** The type of the displayable */
  val type: Type

  /** The GUIType of the displayable */
  val guiType: GUIType

  /** The name of the displayable without color codes */
  val name: String

  /**
   * The point in time the displayable has been created.
   *
   * Some displayables like the public/permission holders, player tracking and death folder have a
   * createdAt value of [java.time.Instant.EPOCH]
   */
  val createdAt: OffsetDateTime
}
