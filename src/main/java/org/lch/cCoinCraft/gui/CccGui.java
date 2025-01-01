// src/main/java/org/lch/cCoinCraft/gui/CccGui.java
package org.lch.cCoinCraft.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lch.cCoinCraft.service.CoinGeckoPriceFetcher;
import org.lch.cCoinCraft.CCoinCraft;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Manages the CCoinCraft GUI.
 */
public class CccGui {

    private final CoinGeckoPriceFetcher priceFetcher;

    public CccGui(CoinGeckoPriceFetcher priceFetcher) {
        this.priceFetcher = priceFetcher;
    }

    /**
     * Initializes and opens the GUI for the player.
     *
     * @param player The player to open the GUI for.
     */
    public void openGui(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "CCoinCraft - Coin Order"); // 54 slots (6 rows)

        initializeGui(inventory, player);

        // Set GUI to initial state
        resetPlayerGui(inventory, player);
        player.openInventory(inventory);
    }

    private void initializeGui(Inventory inventory, Player player) {
        // Fill all slots with gray stained glass panes
        ItemStack grayPane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, grayPane);
        }

        // BTC Setup
        ItemStack bitcoinTitle = createItem(Material.GOLD_BLOCK, ChatColor.GOLD + "Bitcoin (BTC)");
        ItemMeta btcMeta = bitcoinTitle.getItemMeta();
        if (btcMeta != null) {
            btcMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getFormattedPrice("BTC")));
            bitcoinTitle.setItemMeta(btcMeta);
        }
        inventory.setItem(0, bitcoinTitle);

        ItemStack bitcoinSelected = createItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "Bitcoin Selected");
        inventory.setItem(1, bitcoinSelected);

        // ETH Setup
        ItemStack ethereumTitle = createItem(Material.DIAMOND_BLOCK, ChatColor.AQUA + "Ethereum (ETH)");
        ItemMeta ethMeta = ethereumTitle.getItemMeta();
        if (ethMeta != null) {
            ethMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getFormattedPrice("ETH")));
            ethereumTitle.setItemMeta(ethMeta);
        }
        inventory.setItem(9, ethereumTitle);

        ItemStack ethereumUnselected = createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Ethereum");
        inventory.setItem(10, ethereumUnselected);

        // DOGE Setup
        ItemStack dogeUnselected = createItem(Material.GOLD_INGOT, ChatColor.GOLD + "Doge");
        ItemMeta dogeMeta = dogeUnselected.getItemMeta();
        if (dogeMeta != null) {
            dogeMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getFormattedPrice("DOGE")));
            dogeUnselected.setItemMeta(dogeMeta);
        }
        inventory.setItem(19, dogeUnselected);

        // USDT Setup
        ItemStack usdtUnselected = createItem(Material.EMERALD_BLOCK, ChatColor.AQUA + "USDT");
        ItemMeta usdtMeta = usdtUnselected.getItemMeta();
        if (usdtMeta != null) {
            usdtMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getFormattedPrice("USDT")));
            usdtUnselected.setItemMeta(usdtMeta);
        }
        inventory.setItem(28, usdtUnselected);

        // DOGE Price Display
        ItemStack dogePrice = createItem(Material.GOLD_INGOT, ChatColor.BLUE + "Doge Coin (DOGE)");
        ItemMeta dogePriceMeta = dogePrice.getItemMeta();
        if (dogePriceMeta != null) {
            dogePriceMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getFormattedPrice("DOGE")));
            dogePrice.setItemMeta(dogePriceMeta);
        }
        inventory.setItem(18, dogePrice);

        // USDT Price Display
        ItemStack usdtPrice = createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "USDTether (USDT)");
        ItemMeta usdtPriceMeta = usdtPrice.getItemMeta();
        if (usdtPriceMeta != null) {
            usdtPriceMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getFormattedPrice("USDT")));
            usdtPrice.setItemMeta(usdtPriceMeta);
        }
        inventory.setItem(27, usdtPrice);

        // Integer Button (Slot 12)
        ItemStack integerButton = createIntegerButton(0);
        inventory.setItem(12, integerButton);

        // Decimal Buttons (Slots 13-17)
        for (int i = 13; i <= 17; i++) {
            int decimalPlace = i - 12; // Slot 13 -> 1st decimal place, ..., Slot 17 -> 5th decimal place
            ItemStack decimalButton = createDecimalButton(decimalPlace, 0);
            inventory.setItem(i, decimalButton);
        }

        // Buy/Sell Buttons (Buy All and Sell All remain unchanged)
        ItemStack buyAll = createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Buy All");
        ItemMeta buyAllMeta = buyAll.getItemMeta();
        if (buyAllMeta != null) {
            buyAllMeta.setLore(Arrays.asList(ChatColor.GRAY + "Select to buy all selected coins"));
            buyAll.setItemMeta(buyAllMeta);
        }
        inventory.setItem(31, buyAll);

        ItemStack buy = createItem(Material.EMERALD, ChatColor.GREEN + "Buy");
        ItemMeta buyMeta = buy.getItemMeta();
        if (buyMeta != null) {
            buyMeta.setLore(Arrays.asList(ChatColor.GRAY + "Select to buy the selected amount of the selected coin"));
            buy.setItemMeta(buyMeta);
        }
        inventory.setItem(32, buy);

        ItemStack sell = createItem(Material.REDSTONE, ChatColor.RED + "Sell");
        ItemMeta sellMeta = sell.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setLore(Arrays.asList(ChatColor.GRAY + "Select to sell the selected amount of the selected coin"));
            sell.setItemMeta(sellMeta);
        }
        inventory.setItem(33, sell);

        ItemStack sellAll = createItem(Material.REDSTONE_BLOCK, ChatColor.RED + "Sell All");
        ItemMeta sellAllMeta = sellAll.getItemMeta();
        if (sellAllMeta != null) {
            sellAllMeta.setLore(Arrays.asList(ChatColor.GRAY + "Select to sell all selected coins"));
            sellAll.setItemMeta(sellAllMeta);
        }
        inventory.setItem(34, sellAll);

        // Player Head
        ItemStack playerHead = createPlayerHead(player);
        inventory.setItem(45, playerHead);
    }


    /**
     * Resets the player's GUI to the initial state.
     *
     * @param inventory The player's inventory.
     * @param player    The player.
     */
    private void resetPlayerGui(Inventory inventory, Player player) {
        setSelected(inventory, "BTC"); // Default selected coin (BTC)
        resetAmountDisplay(inventory);
    }

    /**
     * Highlights the selected coin in green and others in red.
     *
     * @param inventory The inventory.
     * @param coin      The selected coin symbol.
     */
    public void setSelected(Inventory inventory, String coin) {
        switch (coin) {
            case "BTC":
                setGlassPane(inventory, 1, Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "Bitcoin Selected");
                setGlassPane(inventory, 10, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Ethereum");
                setGlassPane(inventory, 19, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Doge");
                setGlassPane(inventory, 28, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "USDT");
                break;
            case "ETH":
                setGlassPane(inventory, 1, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Bitcoin");
                setGlassPane(inventory, 10, Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "Ethereum Selected");
                setGlassPane(inventory, 19, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Doge");
                setGlassPane(inventory, 28, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "USDT");
                break;
            case "DOGE":
                setGlassPane(inventory, 1, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Bitcoin");
                setGlassPane(inventory, 10, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Ethereum");
                setGlassPane(inventory, 19, Material.GREEN_STAINED_GLASS_PANE, ChatColor.GOLD + "Doge Selected");
                setGlassPane(inventory, 28, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "USDT");
                break;
            case "USDT":
                setGlassPane(inventory, 1, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Bitcoin");
                setGlassPane(inventory, 10, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Ethereum");
                setGlassPane(inventory, 19, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Doge");
                setGlassPane(inventory, 28, Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "USDT Selected");
                break;
            default:
                break;
        }
    }

    /**
     * Sets a coin as unselected (red glass pane).
     *
     * @param inventory The inventory.
     * @param coin      The coin symbol.
     */
    public void setUnselected(Inventory inventory, String coin) {
        switch (coin) {
            case "BTC":
                setGlassPane(inventory, 1, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Bitcoin");
                break;
            case "ETH":
                setGlassPane(inventory, 10, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Ethereum");
                break;
            case "DOGE":
                setGlassPane(inventory, 19, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Doge");
                break;
            case "USDT":
                setGlassPane(inventory, 28, Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "USDT");
                break;
            default:
                break;
        }
    }

    /**
     * Resets the amount display (integer and decimal parts) to zero.
     *
     * @param inventory The inventory.
     */
    public void resetAmountDisplay(Inventory inventory) {
        // Reset integer part (slot 12)
        setIntegerDisplay(inventory, 0);

        // Reset decimal parts (slots 13-17)
        for (int i = 13; i <= 17; i++) {
            setDecimalDisplay(inventory, i, 0);
        }
    }

    /**
     * Updates the integer display.
     *
     * @param inventory The inventory.
     * @param amount    The integer amount.
     */
    public void updateIntegerDisplay(Inventory inventory, int amount) {
        setIntegerDisplay(inventory, amount);
    }

    /**
     * Updates a specific decimal display.
     *
     * @param inventory The inventory.
     * @param slot      The slot number (13-17).
     * @param digit     The decimal digit (0-9).
     */
    public void updateDecimalDisplay(Inventory inventory, int slot, int digit) {
        setDecimalDisplay(inventory, slot, digit);
    }

    /**
     * Sets the integer display in slot 12 with appropriate lore.
     *
     * @param inventory The inventory.
     * @param amount    The integer amount.
     */
    private void setIntegerDisplay(Inventory inventory, int amount) {
        String displayName = ChatColor.WHITE + "Amount: " + amount;
        ItemStack button = new ItemStack(Material.STONE_BUTTON);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Shift + Left Click: +10",
                    ChatColor.GRAY + "Shift + Right Click: -10",
                    ChatColor.GRAY + "Left Click: +1",
                    ChatColor.GRAY + "Right Click: -1"
            ));
            button.setItemMeta(meta);
        }
        inventory.setItem(12, button);
    }

    /**
     * Sets the decimal display in slots 13-17 with appropriate lore.
     *
     * @param inventory    The inventory.
     * @param slot         The slot number (13-17).
     * @param digit        The decimal digit (0-9).
     */
    private void setDecimalDisplay(Inventory inventory, int slot, int digit) {
        String decimalPlace;
        switch (slot) {
            case 13:
                decimalPlace = "First Decimal Place";
                break;
            case 14:
                decimalPlace = "Second Decimal Place";
                break;
            case 15:
                decimalPlace = "Third Decimal Place";
                break;
            case 16:
                decimalPlace = "Fourth Decimal Place";
                break;
            case 17:
                decimalPlace = "Fifth Decimal Place";
                break;
            default:
                decimalPlace = "Decimal";
                break;
        }

        String displayName = ChatColor.WHITE + "Decimal " + decimalPlace + ": " + digit;
        ItemStack button = new ItemStack(Material.OAK_BUTTON);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Left Click: +1",
                    ChatColor.GRAY + "Right Click: -1"
            ));
            button.setItemMeta(meta);
        }
        inventory.setItem(slot, button);
    }

    /**
     * Sets a glass pane with specified material and display name.
     *
     * @param inventory   The inventory.
     * @param slot        The slot number.
     * @param material    The material of the pane.
     * @param displayName The display name of the pane.
     */
    private void setGlassPane(Inventory inventory, int slot, Material material, String displayName) {
        ItemStack pane = createItem(material, displayName);
        inventory.setItem(slot, pane);
    }

    /**
     * Creates a player head item displaying the player's info.
     *
     * @param player The player.
     * @return The player head item.
     */
    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.BLUE + player.getName() + "'s Info");
            // Fetch player's balance
            double balance = CCoinCraft.getEconomy().getBalance(player);
            meta.setLore(Arrays.asList(ChatColor.YELLOW + "Balance: " + ChatColor.GREEN + String.format("%,.2f", balance) + " KRW"));
            // Set the skull owner to the player
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                ((org.bukkit.inventory.meta.SkullMeta) meta).setOwningPlayer(player);
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Formats the price of a coin to "#,###.## KRW".
     *
     * @param coinSymbol The symbol of the coin (e.g., BTC, ETH).
     * @return The formatted price string.
     */
    private String getFormattedPrice(String coinSymbol) {
        Double price = priceFetcher.getPrice(coinSymbol);
        if (price != null) {
            DecimalFormat formatter = new DecimalFormat("#,###.##");
            return formatter.format(price) + " KRW";
        } else {
            return "N/A";
        }
    }

    /**
     * Creates an item with specified material and name.
     *
     * @param material The material of the item.
     * @param name     The display name of the item.
     * @return The created item.
     */
    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates an integer button with appropriate lore.
     *
     * @param amount The initial integer value.
     * @return The integer button item.
     */
    private ItemStack createIntegerButton(int amount) {
        String displayName = ChatColor.WHITE + "Amount: " + amount;
        ItemStack button = new ItemStack(Material.STONE_BUTTON);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Shift + Left Click: +10",
                    ChatColor.GRAY + "Shift + Right Click: -10",
                    ChatColor.GRAY + "Left Click: +1",
                    ChatColor.GRAY + "Right Click: -1"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    /**
     * Creates a decimal button with appropriate lore.
     *
     * @param decimalPlace The decimal place (1-5).
     * @param digit         The initial decimal digit.
     * @return The decimal button item.
     */
    private ItemStack createDecimalButton(int decimalPlace, int digit) {
        String placeText;
        switch (decimalPlace) {
            case 1:
                placeText = "First Decimal Place";
                break;
            case 2:
                placeText = "Second Decimal Place";
                break;
            case 3:
                placeText = "Third Decimal Place";
                break;
            case 4:
                placeText = "Fourth Decimal Place";
                break;
            case 5:
                placeText = "Fifth Decimal Place";
                break;
            default:
                placeText = "Decimal";
                break;
        }

        String displayName = ChatColor.WHITE + "Decimal " + placeText + ": " + digit;
        ItemStack button = new ItemStack(Material.OAK_BUTTON);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Left Click: +1",
                    ChatColor.GRAY + "Right Click: -1"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    /**
     * Updates the prices of all coins in the GUI.
     *
     * @param inventory The inventory to update.
     */
    public void updatePrices(Inventory inventory) {
        // Slot 0: Bitcoin Price Update
        ItemStack btcPrice = createItem(Material.GOLD_BLOCK, ChatColor.GOLD + "Bitcoin (BTC)");
        ItemMeta btcMeta = btcPrice.getItemMeta();
        if (btcMeta != null) {
            btcMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getFormattedPrice("BTC")));
            btcPrice.setItemMeta(btcMeta);
        }
        inventory.setItem(0, btcPrice);

        // Slot 9: Ethereum Price Update
        ItemStack ethPrice = createItem(Material.DIAMOND_BLOCK, ChatColor.AQUA + "Ethereum (ETH)");
        ItemMeta ethMeta = ethPrice.getItemMeta();
        if (ethMeta != null) {
            ethMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getFormattedPrice("ETH")));
            ethPrice.setItemMeta(ethMeta);
        }
        inventory.setItem(9, ethPrice);

        // Slot 18: Doge Price Update
        ItemStack dogePrice = createItem(Material.GOLD_INGOT, ChatColor.BLUE + "Doge Coin (DOGE)");
        ItemMeta dogePriceMeta = dogePrice.getItemMeta();
        if (dogePriceMeta != null) {
            dogePriceMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getFormattedPrice("DOGE")));
            dogePrice.setItemMeta(dogePriceMeta);
        }
        inventory.setItem(18, dogePrice);

        // Slot 27: USDT Price Update
        ItemStack usdtPrice = createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "USDTether (USDT)");
        ItemMeta usdtPriceMeta = usdtPrice.getItemMeta();
        if (usdtPriceMeta != null) {
            usdtPriceMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getFormattedPrice("USDT")));
            usdtPrice.setItemMeta(usdtPriceMeta);
        }
        inventory.setItem(27, usdtPrice);
    }

    /**
     * Updates the player's balance information on their head.
     *
     * @param inventory The inventory.
     * @param player    The player.
     */
    public void updatePlayerInfo(Inventory inventory, Player player) {
        // Slot 45: Player Head and Balance Info Update
        ItemStack playerHead = createPlayerHead(player);
        inventory.setItem(45, playerHead);
    }
}