package me.miscodes.configurate.serializers

import java.lang.reflect.Type
import java.time.Period
import me.miscodes.configurate.nonVirtualNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer

internal object PeriodSerializer : TypeSerializer<Period> {

  private const val YEARS = "years"
  private const val MONTHS = "months"
  private const val DAYS = "days"

  override fun deserialize(type: Type, node: ConfigurationNode): Period {
    return Period.of(
        node.nonVirtualNode(YEARS).int,
        node.nonVirtualNode(MONTHS).int,
        node.nonVirtualNode(DAYS).int,
    )
  }

  override fun serialize(type: Type, obj: Period?, node: ConfigurationNode) {
    val period = obj ?: Period.ZERO

    node.node(YEARS).set(period.years)
    node.node(MONTHS).set(period.months)
    node.node(DAYS).set(period.days)
  }
}
