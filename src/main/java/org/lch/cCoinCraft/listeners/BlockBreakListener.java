package org.lch.cCoinCraft.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.lch.cCoinCraft.service.OreRewardService;
import org.bukkit.inventory.ItemStack;

public class BlockBreakListener implements Listener {

    private final OreRewardService oreRewardService;

    public BlockBreakListener(OreRewardService oreRewardService) {
        this.oreRewardService = oreRewardService;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // 플레이어가 없거나, 크리에이티브 모드는 보상 제외(원하면 변경 가능)
        if (event.getPlayer() == null) return;
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) return;

        Material blockType = event.getBlock().getType();
        // "광물을 깼을 때만" 로직
        // 여기서는 별도의 조건 검사 없이 바로 oreRewardService 호출해도 됨.
        // 다만, 정말 '광물'만 처리하려면 if (isOre(blockType)) 등 별도 검사를 추가할 수도 있음.

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        // OreRewardService에 처리를 위임
        oreRewardService.handleOreBreak(event.getPlayer(), blockType, tool);
    }
}