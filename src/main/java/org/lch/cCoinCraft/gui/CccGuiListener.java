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

    // Stores selected coins and amounts per player
    private final Map<UUID, String> playerSelectedCoins;
    private final Map<UUID, Integer> playerIntegerAmounts; // Integer part (0~999999)
    private final Map<UUID, int[]> playerDecimalAmounts; // Decimal part (0~9) per place

    public CccGuiListener(CoinGeckoPriceFetcher priceFetcher, CccGui cccGui) {
        this.priceFetcher = priceFetcher;
        this.cccGui = cccGui;
        this.playerSelectedCoins = new HashMap<>();
        this.playerIntegerAmounts = new HashMap<>();
        this.playerDecimalAmounts = new HashMap<>();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Ignore clicks outside of CCoinCraft GUI
        if (!event.getView().getTitle().equals("CCoinCraft - Coin Order")) {
            return;
        }

        event.setCancelled(true); // Cancel default click behavior

        // Get the clicked slot
        int slot = event.getRawSlot();

        // Ensure the clicker is a player
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        // Reference to the player's inventory
        Inventory inventory = event.getInventory();

        switch (slot) {
            // Coin selection slots
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

            // Quantity adjustment slots
            case 12: // Integer part (slot 12)
                handleIntegerSelection(player, slot, event, inventory);
                break;

            // Decimal adjustment slots (slots 13-17)
            case 13: // First decimal place
            case 14: // Second decimal place
            case 15: // Third decimal place
            case 16: // Fourth decimal place
            case 17: // Fifth decimal place
                handleDecimalSelection(player, slot, event, inventory);
                break;

            // Buy/Sell buttons
            case 31: // Buy All
                executeCommand(player, "buyall", inventory);
                break;

            case 32: // Buy
                executeCommand(player, "buy", inventory);
                break;

            case 33: // Sell
                executeCommand(player, "sell", inventory);
                break;

            case 34: // Sell All
                executeCommand(player, "sellall", inventory);
                break;

            // Ignore other slots
            default:
                break;
        }
    }

    private void selectCoin(Player player, String coin, Inventory inventory) {
        // Deselect other coins
        for (String existingCoin : new String[]{"BTC", "ETH", "DOGE", "USDT"}) {
            if (!existingCoin.equals(coin)) {
                cccGui.setUnselected(inventory, existingCoin);
            }
        }

        // Set the selected coin
        playerSelectedCoins.put(player.getUniqueId(), coin);
        playerIntegerAmounts.put(player.getUniqueId(), 0);
        playerDecimalAmounts.put(player.getUniqueId(), new int[]{0, 0, 0, 0, 0});
        cccGui.setSelected(inventory, coin);
        cccGui.resetAmountDisplay(inventory);
        player.sendMessage(ChatColor.GREEN + "Selected Coin: " + coin);
    }

    private void executeCommand(Player player, String action, Inventory inventory) {
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

        // Update player balance info
        cccGui.updatePlayerInfo(inventory, player);
    }

    private String getSelectedCoin(Player player) {
        return playerSelectedCoins.getOrDefault(player.getUniqueId(), "BTC"); // Default is BTC
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

        // Set minimum and maximum values
        if (currentAmount < 0) {
            currentAmount = 0;
        } else if (currentAmount > 999999) {
            currentAmount = 999999;
        }

        // Update amount
        playerIntegerAmounts.put(playerId, currentAmount);
        cccGui.updateIntegerDisplay(inventory, currentAmount);
    }

    private void handleDecimalSelection(Player player, int slot, InventoryClickEvent event, Inventory inventory) {
        UUID playerId = player.getUniqueId();
        int[] decimalParts = playerDecimalAmounts.getOrDefault(playerId, new int[]{0, 0, 0, 0, 0});

        // Determine the decimal place (slots 13-17: first to fifth decimal place)
        int decimalPlace = slot - 13; // Slot 13 -> 0th decimal place, ..., Slot 17 -> 4th decimal place

        if (decimalPlace < 0 || decimalPlace >= 5) {
            // Invalid slot
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

        // Update the specific decimal place
        decimalParts[decimalPlace] = digit;
        playerDecimalAmounts.put(playerId, decimalParts);

        // Update the decimal button in the GUI
        cccGui.updateDecimalDisplay(inventory, slot, digit);
    }
}