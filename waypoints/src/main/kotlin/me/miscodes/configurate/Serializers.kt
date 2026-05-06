package me.miscodes.configurate

import java.time.Period
import me.miscodes.configurate.serializers.BlockDataSerializer
import me.miscodes.configurate.serializers.BlockTypeSerializer
import me.miscodes.configurate.serializers.ComponentSerializer
import me.miscodes.configurate.serializers.DurationSerializer
import me.miscodes.configurate.serializers.ItemTypeSerializer
import me.miscodes.configurate.serializers.PeriodSerializer
import me.miscodes.configurate.serializers.SoundSerializer
import me.miscodes.configurate.serializers.StyleSerializer
import net.kyori.adventure.sound.Sound
import org.spongepowered.configurate.kotlin.extensions.addConstraint
import org.spongepowered.configurate.kotlin.extensions.addProcessor
import org.spongepowered.configurate.kotlin.kotlinCommentsProcessor
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.spongepowered.configurate.util.NamingScheme
import org.spongepowered.configurate.util.NamingSchemes

fun commonSerializers(
    namingScheme: NamingScheme = NamingSchemes.CAMEL_CASE
): TypeSerializerCollection =
    TypeSerializerCollection.builder()
        .register(BlockDataSerializer)
        .register(BlockTypeSerializer)
        .register(DurationSerializer)
        .register(ItemTypeSerializer)
        .register(Period::class.java, PeriodSerializer)
        .register(Sound::class.java, SoundSerializer)
        .register(StyleSerializer)
        .register(ComponentSerializer)
        .registerAnnotatedObjects(
            ObjectMapper.factoryBuilder()
                .defaultNamingScheme(namingScheme)
                .addConstraint(Positive.Factory)
                .addConstraint(Min.Factory)
                .addConstraint(Max.Factory)
                .addConstraint(NonEmptyString.Factory)
                .addProcessor(kotlinCommentsProcessor())
                .build()
        )
        .build()
