// src/main/java/org/lch/cCoinCraft/gui/CccGuiListener.java
package org.lch.cCoinCraft.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.lch.cCoinCraft.service.CoinGeckoPriceFetcher;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CccGuiListener implements Listener {

    private final CoinGeckoPriceFetcher priceFetcher;
    private final CccGui cccGui;

    // 플레이어별 선택된 코인과 수량을 저장
    private final Map<UUID, String> playerSelectedCoins;
    private final Map<UUID, Integer> playerIntegerAmounts; // 정수 부분 (0~999999)
    private final Map<UUID, int[]> playerDecimalAmounts; // 소수점 부분 (0~9) 각 자리별

    public CccGuiListener(CoinGeckoPriceFetcher priceFetcher, CccGui cccGui) {
        this.priceFetcher = priceFetcher;
        this.cccGui = cccGui;
        this.playerSelectedCoins = new HashMap<>();
        this.playerIntegerAmounts = new HashMap<>();
        this.playerDecimalAmounts = new HashMap<>();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 클릭한 인벤토리가 CCoinCraft GUI가 아니면 무시
        if (!event.getView().getTitle().equals("CCoinCraft - Coin Order")) {
            return;
        }

        event.setCancelled(true); // 기본 클릭 동작 취소

        // 클릭한 슬롯 번호
        int slot = event.getRawSlot();

        // 클릭한 플레이어
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        // 현재 플레이어의 인벤토리 참조
        Inventory inventory = event.getInventory();

        switch (slot) {
            // 코인 선택 슬롯
            case 1: // Bitcoin
                selectCoin(player, "BTC", inventory);
                break;

            case 10: // Ethereum
                selectCoin(player, "ETH", inventory);
                break;

            case 19: // Doge
                selectCoin(player, "DOGE", inventory);
                break;

            case 28: // USDT
                selectCoin(player, "USDT", inventory);
                break;

            // 수량 조정 슬롯
            case 12: // 정수 부분 (슬롯 12)
                handleIntegerSelection(player, slot, event, inventory);
                break;

            // 소수점 조정 슬롯 (슬롯 13-17)
            case 13: // 소수점 첫째 자리
            case 14: // 소수점 둘째 자리
            case 15: // 소수점 셋째 자리
            case 16: // 소수점 넷째 자리
            case 17: // 소수점 다섯째 자리
                handleDecimalSelection(player, slot, event, inventory);
                break;

            // 구매/판매 버튼
            case 31: // Buy All
                executeCommand(player, "buyall");
                break;

            case 32: // Buy
                executeCommand(player, "buy");
                break;

            case 33: // Sell
                executeCommand(player, "sell");
                break;

            case 34: // Sell All
                executeCommand(player, "sellall");
                break;

            // 기타 슬롯 클릭 시 무시
            default:
                break;
        }
    }

    private void selectCoin(Player player, String coin, Inventory inventory) {
        // 기존 선택 해제
        for (String existingCoin : new String[]{"BTC", "ETH", "DOGE", "USDT"}) {
            if (!existingCoin.equals(coin)) {
                cccGui.setUnselected(inventory, existingCoin);
            }
        }

        // 선택된 코인 설정
        playerSelectedCoins.put(player.getUniqueId(), coin);
        playerIntegerAmounts.put(player.getUniqueId(), 0);
        playerDecimalAmounts.put(player.getUniqueId(), new int[]{0, 0, 0, 0, 0});
        cccGui.setSelected(inventory, coin);
        cccGui.resetAmountDisplay(inventory);
        player.sendMessage(ChatColor.GREEN + "Selected Coin: " + coin);
    }

    private void executeCommand(Player player, String action) {
        String coin = getSelectedCoin(player);
        String amount = getAmount(player);

        String command;

        switch (action.toLowerCase()) {
            case "buy":
                command = "buy " + coin + " " + amount;
                break;
            case "buyall":
                command = "buy " + coin + " all";
                break;
            case "sell":
                command = "sell " + coin + " " + amount;
                break;
            case "sellall":
                command = "sell " + coin + " all";
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown action: " + action);
                return;
        }

        // Log command to console
        Bukkit.getLogger().info("Executing command for player " + player.getName() + ": /ccc " + command);

        // Execute command
        player.performCommand("ccc " + command);

        // Provide feedback to player
        player.sendMessage(ChatColor.YELLOW + "Executing: " + ChatColor.GREEN + "/ccc " + command);
    }

    private String getSelectedCoin(Player player) {
        return playerSelectedCoins.getOrDefault(player.getUniqueId(), "BTC"); // 기본값 BTC
    }

    private String getAmount(Player player) {
        int integerPart = playerIntegerAmounts.getOrDefault(player.getUniqueId(), 0);
        int[] decimalParts = playerDecimalAmounts.getOrDefault(player.getUniqueId(), new int[]{0, 0, 0, 0, 0});
        StringBuilder decimalBuilder = new StringBuilder();
        for (int digit : decimalParts) {
            decimalBuilder.append(digit);
        }
        return integerPart + "." + decimalBuilder.toString();
    }

    private void handleIntegerSelection(Player player, int slot, InventoryClickEvent event, Inventory inventory) {
        UUID playerId = player.getUniqueId();
        int currentAmount = playerIntegerAmounts.getOrDefault(playerId, 0);

        int increment = 0;
        if (event.isShiftClick()) {
            if (event.isLeftClick()) {
                increment = 10;
            } else if (event.isRightClick()) {
                increment = -10;
            }
        } else {
            if (event.isLeftClick()) {
                increment = 1;
            } else if (event.isRightClick()) {
                increment = -1;
            }
        }

        currentAmount += increment;

        // 최소값과 최대값 설정
        if (currentAmount < 0) {
            currentAmount = 0;
        } else if (currentAmount > 999999) {
            currentAmount = 999999;
        }

        // 수량 업데이트
        playerIntegerAmounts.put(playerId, currentAmount);
        cccGui.updateIntegerDisplay(inventory, currentAmount);
    }

    private void handleDecimalSelection(Player player, int slot, InventoryClickEvent event, Inventory inventory) {
        UUID playerId = player.getUniqueId();
        int[] decimalParts = playerDecimalAmounts.getOrDefault(playerId, new int[]{0, 0, 0, 0, 0});

        // 소수점 자리를 결정 (슬롯 13-17: 첫째 자리부터 다섯째 자리)
        int decimalPlace = slot - 13; // 슬롯 13 -> 0번째 소수점 자리, ..., 슬롯 17 -> 4번째 자리

        if (decimalPlace < 0 || decimalPlace >= 5) {
            // 유효하지 않은 슬롯
            return;
        }

        int digit = decimalParts[decimalPlace];

        if (event.isLeftClick()) {
            if (digit == 9) {
                digit = 0;
            } else {
                digit += 1;
            }
        } else if (event.isRightClick()) {
            if (digit == 0) {
                digit = 9;
            } else {
                digit -= 1;
            }
        }

        // 소수점 자리의 값을 업데이트
        decimalParts[decimalPlace] = digit;
        playerDecimalAmounts.put(playerId, decimalParts);

        // 소수점 슬롯 업데이트
        cccGui.updateDecimalDisplay(inventory, slot, digit);
    }
}