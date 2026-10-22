package io.github.chamoisest.fluxbound.client.gui;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// Display names for dimension IDs. Vanilla has no translations for dimension names, so the key
// dimension.<namespace>.<path> is looked up in all loaded language files: ours supplies the vanilla three,
// and other mods' translations are used if they provide this key. Otherwise the raw ID is shown.
public final class DimensionNames
{
    private DimensionNames() {}

    public static Component of(String dimensionId)
    {
        Identifier id = Identifier.tryParse(dimensionId);

        if (id != null)
        {
            String key = "dimension." + id.getNamespace() + "." + id.getPath();

            if (Language.getInstance().has(key))
            {
                return Component.translatable(key);
            }
        }

        return Component.literal(dimensionId);
    }
}
