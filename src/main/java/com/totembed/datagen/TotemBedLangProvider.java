package com.totembed.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

public class TotemBedLangProvider extends FabricLanguageProvider {
    public TotemBedLangProvider(FabricDataOutput output) {
        super(output, "en_us");
    }

    @Override
    public void generateTranslations(TranslationBuilder b) {
        b.add("enchantment.totembed.totem_stasis", "Totem Stasis");
        b.add("tooltip.totembed.stasis_linked", "Bound: %s %s, %s, %s");
        b.add("message.totembed.bound", "Totem bound to bed at %s, %s, %s (%s)");
        b.add("message.totembed.returned", "The totem returns you to your bed.");
        b.add("message.totembed.returned_safe", "The bed cannot be slept in here; placed beside it instead.");
        b.add("message.totembed.bed_gone", "Bound bed is gone; totem saved you where you stand.");
        b.add("message.totembed.invalid_bed", "Bound bed is unusable; totem saved you where you stand.");
        b.add("message.totembed.return_failed", "Totem saved you, but the bed return failed.");
    }
}
