
package io.github.chamoisest.fluxbound.integration.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.ProgressView;

public final class MachineEnergyJadeComponent implements IBlockComponentProvider
{
    public static final MachineEnergyJadeComponent INSTANCE = new MachineEnergyJadeComponent();

    private static final Element ENERGY_OVERLAY = JadeUI.horizontalTiledSprite(
            RenderPipelines.GUI_TEXTURED,
            Identifier.fromNamespaceAndPath("jade", "energy_progress"),
            16, 16
    );

    private MachineEnergyJadeComponent() {}

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
    {
        MachineEnergyJadeProvider.INSTANCE.decodeFromData(accessor).ifPresent(energy -> {
            EnergyView view = EnergyView.read(new EnergyView.Data(energy.stored(), energy.capacity()), "FE");

            if (view == null) {
                return;
            }

            Component text = Component.translatable(
                    "jade.fe",
                    ChatFormatting.WHITE + view.current,
                    view.max
            );

            ProgressView progressView = new ProgressView(
                    ProgressView.Part.of(Math.clamp(view.ratio, 0.0F, 1.0F), ENERGY_OVERLAY),
                    text,
                    JadeUI.progressStyle(),
                    BoxStyle.nestedBox()
            );

            tooltip.add(JadeUI.progress(progressView));
        });
    }

    @Override
    public Identifier getUid()
    {
        return MachineEnergyJadeProvider.UID;
    }
}
