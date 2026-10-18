package io.github.chamoisest.fluxbound.client;

import guideme.Guide;
import io.github.chamoisest.fluxbound.Fluxbound;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

// Client-only entry point. NeoForge constructs this class only on the physical client,
// so client-only setup such as building GuideME guides never runs on a dedicated server.
@Mod(value = Fluxbound.MODID, dist = Dist.CLIENT)
public final class FluxboundClient
{
    public FluxboundClient()
    {
        Guide.builder(Identifier.parse("fluxbound:guide")).build();
    }
}
