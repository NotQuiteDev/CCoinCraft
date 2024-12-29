package org.lch.cCoinCraft.service;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lch.cCoinCraft.CCoinCraft;
import org.lch.cCoinCraft.database.PlayerDAO;

import java.text.DecimalFormat;
import java.util.UUID;

public class OreRewardService {

    private final CCoinCraft plugin;
    private final PlayerDAO playerDAO;

    // 임시 환율 (1 BTC = 30,000,000원 라고 가정)
    // 나중에 Coingecko API 클래스를 만들어, 거기서 받아오면 됨.
    private static final double BTC_TO_KRW_RATE = 140249283.0;

    // 숫자를 소수점 표기로 깔끔하게 표시하기 위한 포맷
    // 원하는 자리수에 맞춰 포맷 변경 가능
    private static final DecimalFormat BTC_FORMAT = new DecimalFormat("0.########");
    private static final DecimalFormat KRW_FORMAT = new DecimalFormat("###,###");

    public OreRewardService(CCoinCraft plugin, PlayerDAO playerDAO) {
        this.plugin = plugin;
        this.playerDAO = playerDAO;
    }

    /**
     * 광물을 캤을 때 보상 로직을 처리.
     * @param player 광물을 캔 플레이어
     * @param blockType 캐진 블록(Material)
     * @param tool 플레이어가 사용한 도구
     */
    public void handleOreBreak(Player player, Material blockType, ItemStack tool) {
        // 1) 실크 터치인지 검사
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        String blockName = blockType.name();
        String configPath = "ore-rewards." + blockName;

        // 2) config.yml에 해당 블록(reward) 설정이 있는지 확인
        if (!plugin.getConfig().contains(configPath)) {
            // 설정이 없는 광물이면 보상 X
            return;
        }

        // 3) 확률 / 지급량 불러오기
        double probability = plugin.getConfig().getDouble(configPath + ".probability");
        double amountBtc = plugin.getConfig().getDouble(configPath + ".amount");

        // 4) 랜덤 체크
        if (Math.random() < probability) {
            // DB 보상 지급
            UUID uuid = player.getUniqueId();
            playerDAO.addBtcBalance(uuid, amountBtc);

            // 5) 메시지 구성
            // blockName: "DIAMOND_ORE" → 필요한 경우 한글로 바꾸거나 그대로 사용
            String oreDisplayName = blockName;
            // 소수점 표기 (e.g. 0.00014 등)
            String btcFormatted = BTC_FORMAT.format(amountBtc);

            // 임시 환산 (향후 CoingeckoAPI → 메소드 대체)
            double krwValue = amountBtc * BTC_TO_KRW_RATE;
            String krwFormatted = KRW_FORMAT.format(krwValue);

            // 최종 메시지
            String message = String.format(
                    "§e[CCC] %s을(를) 채굴하여 %s BTC를 얻었습니다. (~%s원 가치)",
                    oreDisplayName, btcFormatted, krwFormatted
            );

            player.sendMessage(message);
        }
    }
}