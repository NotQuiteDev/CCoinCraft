package org.lch.cCoinCraft.service;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lch.cCoinCraft.CCoinCraft;
import org.lch.cCoinCraft.database.BtcHistoryDAO;
import org.lch.cCoinCraft.database.PlayerDAO;

import java.text.DecimalFormat;
import java.util.UUID;

public class OreRewardService {

    private final CCoinCraft plugin;
    private final PlayerDAO playerDAO;
    private final BtcHistoryDAO btcHistoryDAO;
    private final CoinGeckoPriceFetcher priceFetcher;

    // 숫자를 소수점 표기로 깔끔하게 표시하기 위한 포맷
    private static final DecimalFormat BTC_FORMAT = new DecimalFormat("0.########");
    private static final DecimalFormat KRW_FORMAT = new DecimalFormat("###,###");

    public OreRewardService(CCoinCraft plugin, PlayerDAO playerDAO, BtcHistoryDAO btcHistoryDAO, CoinGeckoPriceFetcher priceFetcher) {
        this.plugin = plugin;
        this.playerDAO = playerDAO;
        this.btcHistoryDAO = btcHistoryDAO;
        this.priceFetcher = priceFetcher;
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

            // 잔고 추가: updateCoinBalance 메서드 사용
            playerDAO.updateCoinBalance(uuid, "BTC", amountBtc);

            // 5) 메시지 구성
            String oreDisplayName = blockName; // 필요한 경우 한글로 바꾸거나 그대로 사용
            String btcFormatted = BTC_FORMAT.format(amountBtc);

            // 실시간 환율을 사용하여 KRW 가치 계산
            Double btcPriceKRW = priceFetcher.getPrice("BTC"); // BTC의 KRW 가격
            if (btcPriceKRW == null) {
                player.sendMessage(ChatColor.RED + "[CCC] 현재 BTC 환율을 가져올 수 없습니다.");
                return;
            }
            double krwValue = amountBtc * btcPriceKRW;
            String krwFormatted = KRW_FORMAT.format(krwValue);

            // 최종 메시지
            String message = String.format(
                    "§e[CCC] %s을(를) 채굴하여 %s BTC를 얻었습니다. (~%s원 가치)",
                    oreDisplayName, btcFormatted, krwFormatted
            );

            // DB: 획득 기록 남기기
            btcHistoryDAO.insertHistory(
                    uuid.toString(),
                    player.getName(),
                    amountBtc,
                    blockType.name()  // 여기서 reason을 "DIAMOND_ORE" 등으로
            );

            player.sendMessage(message);
        }
    }
}