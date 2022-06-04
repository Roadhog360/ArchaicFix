package org.embeddedt.archaicfix.mixins.core.client;

import codechicken.nei.ItemList;
import codechicken.nei.api.ItemInfo;
import com.google.common.collect.ImmutableList;
import cpw.mods.fml.common.Loader;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.embeddedt.archaicfix.ArchaicFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreative extends InventoryEffectRenderer {
    @Shadow private static int selectedTabIndex;

    @Shadow private GuiTextField searchField;

    @Shadow private float currentScroll;

    private final boolean neiPresent = Loader.isModLoaded("NotEnoughItems");

    public MixinGuiContainerCreative(Container p_i1089_1_) {
        super(p_i1089_1_);
    }

    @Inject(method = "updateCreativeSearch", at = @At(value = "HEAD"), cancellable = true)
    private void updateSearchUsingNEI(CallbackInfo ci) {
        if(neiPresent) {
            ci.cancel();
            GuiContainerCreative.ContainerCreative containercreative = (GuiContainerCreative.ContainerCreative)this.inventorySlots;
            containercreative.itemList.clear();
            CreativeTabs tab = CreativeTabs.creativeTabArray[selectedTabIndex];
            if (tab.hasSearchBar() && tab != CreativeTabs.tabAllSearch) {
                tab.displayAllReleventItems(containercreative.itemList);
            } else {
                String search = this.searchField.getText().toLowerCase();
                List<ItemStack> filteredItems;
                if(search.length() > 0) {
                    try {
                        filteredItems = ItemList.forkJoinPool.submit(() ->
                                ArchaicFix.initialCreativeItems.parallelStream()
                                        .filter(stack -> {
                                            String s = ItemInfo.getSearchName(stack);
                                            if(s != null)
                                                return s.contains(search);
                                            else
                                                return false;
                                        })
                                        .collect(Collectors.toList())
                        ).get();
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        filteredItems = ImmutableList.of();
                    }
                } else
                    filteredItems = ArchaicFix.initialCreativeItems;
                containercreative.itemList.addAll(filteredItems);
            }
            this.currentScroll = 0.0F;
            containercreative.scrollTo(0.0F);
        }

    }
}