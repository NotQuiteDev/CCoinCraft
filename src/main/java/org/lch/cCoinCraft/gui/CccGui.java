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

import java.util.Arrays;

/**
 * CCoinCraft GUI를 관리하는 클래스
 */
public class CccGui {

    private final CoinGeckoPriceFetcher priceFetcher;

    public CccGui(CoinGeckoPriceFetcher priceFetcher) {
        this.priceFetcher = priceFetcher;
    }

    /**
     * GUI의 초기 레이아웃을 설정하고 플레이어에게 인벤토리를 엽니다.
     *
     * @param player 플레이어
     */
    public void openGui(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "CCoinCraft - Coin Order"); // 54 슬롯 (6행)

        initializeGui(inventory, player);

        // GUI를 초기 상태로 설정
        resetPlayerGui(inventory, player);
        player.openInventory(inventory);
    }

    private void initializeGui(Inventory inventory, Player player) {
        // 모든 슬롯을 회색 유리창으로 채움
        ItemStack grayPane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, grayPane);
        }

        // BTC 설정
        ItemStack bitcoinTitle = createItem(Material.GOLD_BLOCK, ChatColor.GOLD + "Bitcoin (BTC)");
        ItemMeta btcMeta = bitcoinTitle.getItemMeta();
        if (btcMeta != null) {
            btcMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getPrice("BTC")));
            bitcoinTitle.setItemMeta(btcMeta);
        }
        inventory.setItem(0, bitcoinTitle);

        ItemStack bitcoinSelected = createItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "Bitcoin Selected");
        inventory.setItem(1, bitcoinSelected);

        // ETH 설정
        ItemStack ethereumTitle = createItem(Material.DIAMOND_BLOCK, ChatColor.AQUA + "Ethereum (ETH)");
        ItemMeta ethMeta = ethereumTitle.getItemMeta();
        if (ethMeta != null) {
            ethMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getPrice("ETH")));
            ethereumTitle.setItemMeta(ethMeta);
        }
        inventory.setItem(9, ethereumTitle);

        ItemStack ethereumUnselected = createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Ethereum");
        inventory.setItem(10, ethereumUnselected);

        // DOGE 통일성 설정
        ItemStack dogeUnselected = createItem(Material.GOLD_INGOT, ChatColor.GOLD + "Doge");
        inventory.setItem(19, dogeUnselected);

        // USDT 통일성 설정
        ItemStack usdtUnselected = createItem(Material.EMERALD_BLOCK, ChatColor.AQUA + "USDT");
        inventory.setItem(28, usdtUnselected);

        // DOGE 가격 표시
        ItemStack dogePrice = createItem(Material.GOLD_INGOT, ChatColor.YELLOW + "Doge Price");
        ItemMeta dogePriceMeta = dogePrice.getItemMeta();
        if (dogePriceMeta != null) {
            dogePriceMeta.setLore(Arrays.asList(ChatColor.GREEN + getPrice("DOGE")));
            dogePrice.setItemMeta(dogePriceMeta);
        }
        inventory.setItem(18, dogePrice);

        // USDT 가격 표시
        ItemStack usdtPrice = createItem(Material.EMERALD_BLOCK, ChatColor.YELLOW + "USDT Price");
        ItemMeta usdtPriceMeta = usdtPrice.getItemMeta();
        if (usdtPriceMeta != null) {
            usdtPriceMeta.setLore(Arrays.asList(ChatColor.GREEN + getPrice("USDT")));
            usdtPrice.setItemMeta(usdtPriceMeta);
        }
        inventory.setItem(27, usdtPrice);

        // 정수 버튼 (슬롯 12)
        ItemStack integerButton = createIntegerButton(0);
        inventory.setItem(12, integerButton);

        // 소수점 버튼들 (슬롯 13-17)
        for (int i = 13; i <= 17; i++) {
            int decimalPlace = i - 12; // 슬롯 13 -> 1번째 자리, ..., 슬롯 17 -> 5번째 자리
            ItemStack decimalButton = createDecimalButton(decimalPlace, 0);
            inventory.setItem(i, decimalButton);
        }

        // 구매/판매 버튼 (Buy All과 Sell All은 기존대로 유지)
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

        // 플레이어 헤드
        ItemStack playerHead = createPlayerHead(player);
        inventory.setItem(45, playerHead);
    }


    /**
     * 플레이어의 GUI를 초기 상태로 설정하는 메서드
     *
     * @param inventory 인벤토리
     * @param player    플레이어
     */
    private void resetPlayerGui(Inventory inventory, Player player) {
        setSelected(inventory, "BTC"); // 기본 선택 코인 (BTC)
        resetAmountDisplay(inventory);
    }

    /**
     * 선택된 코인을 초록색으로 설정하고 나머지는 빨간색으로 설정
     *
     * @param inventory 인벤토리
     * @param coin      선택된 코인 심볼
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
                setGlassPane(inventory, 19, Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "Doge Selected");
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
     * 선택되지 않은 코인을 빨간색으로 설정
     *
     * @param inventory 인벤토리
     * @param coin      코인 심볼
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
     * 플레이어의 수량 표시를 초기화하는 메서드
     *
     * @param inventory 인벤토리
     */
    public void resetAmountDisplay(Inventory inventory) {
        // 정수 부분 초기화 (슬롯 12)
        setIntegerDisplay(inventory, 0);

        // 소수점 부분 초기화 (슬롯 13-17)
        for (int i = 13; i <= 17; i++) {
            setDecimalDisplay(inventory, i, 0);
        }
    }

    /**
     * 정수 부분의 수량을 업데이트하는 메서드
     *
     * @param inventory 인벤토리
     * @param amount    정수 수량
     */
    public void updateIntegerDisplay(Inventory inventory, int amount) {
        // 슬롯 12에 정수 수량을 표시
        String amountStr = String.valueOf(amount);
        if (amountStr.length() > 6) {
            amountStr = amountStr.substring(amountStr.length() - 6);
        } else {
            amountStr = String.format("%6s", amountStr).replace(' ', '0');
        }
        setIntegerDisplay(inventory, amount);
    }

    /**
     * 소수점 부분의 수량을 업데이트하는 메서드
     *
     * @param inventory 인벤토리
     * @param slot      소수점 슬롯 번호 (13-17)
     * @param digit     소수점 숫자 (0~9)
     */
    public void updateDecimalDisplay(Inventory inventory, int slot, int digit) {
        setDecimalDisplay(inventory, slot, digit);
    }

    /**
     * 정수 부분의 수량을 표시하는 메서드
     *
     * @param inventory 인벤토리
     * @param amount    정수 수량
     */
    private void setIntegerDisplay(Inventory inventory, int amount) {
        String displayName = ChatColor.WHITE + "Amount: " + amount;
        ItemStack button = createItem(Material.STONE_BUTTON, displayName);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Shift + 좌클릭: +10",
                    ChatColor.GRAY + "Shift + 우클릭: -10",
                    ChatColor.GRAY + "좌클릭: +1",
                    ChatColor.GRAY + "우클릭: -1"
            ));
            button.setItemMeta(meta);
        }
        inventory.setItem(12, button);
    }

    /**
     * 소수점 부분의 수량을 표시하는 메서드
     *
     * @param inventory 인벤토리
     * @param slot      소수점 슬롯 번호 (13-17)
     * @param digit     소수점 숫자 (0~9)
     */
    private void setDecimalDisplay(Inventory inventory, int slot, int digit) {
        String decimalPlace;
        switch (slot) {
            case 13:
                decimalPlace = "첫째 자리";
                break;
            case 14:
                decimalPlace = "둘째 자리";
                break;
            case 15:
                decimalPlace = "셋째 자리";
                break;
            case 16:
                decimalPlace = "넷째 자리";
                break;
            case 17:
                decimalPlace = "다섯째 자리";
                break;
            default:
                decimalPlace = "소수점";
                break;
        }

        String displayName = ChatColor.WHITE + "소수점 " + decimalPlace + ": " + digit;
        ItemStack button = createItem(Material.OAK_BUTTON, displayName);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "좌클릭: +1",
                    ChatColor.GRAY + "우클릭: -1"
            ));
            button.setItemMeta(meta);
        }
        inventory.setItem(slot, button);
    }

    /**
     * 슬롯에 따라 아이템을 설정하는 메서드
     *
     * @param inventory   인벤토리
     * @param slot        슬롯 번호
     * @param material    아이템 재질
     * @param displayName 아이템 이름
     */
    private void setGlassPane(Inventory inventory, int slot, Material material, String displayName) {
        ItemStack pane = createItem(material, displayName);
        inventory.setItem(slot, pane);
    }

    /**
     * 플레이어의 머리를 표시하는 아이템을 생성하는 메서드
     *
     * @param player 플레이어
     * @return 플레이어 머리 아이템
     */
    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.BLUE + player.getName() + "'s Info");
            // Fetch player's balance
            double balance = CCoinCraft.getEconomy().getBalance(player);
            meta.setLore(Arrays.asList(ChatColor.YELLOW + "Balance: " + ChatColor.GREEN + String.format("%.2f", balance) + " KRW"));
            // Set the skull owner to the player
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                ((org.bukkit.inventory.meta.SkullMeta) meta).setOwningPlayer(player);
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * 코인 가격을 가져오는 메서드
     *
     * @param coinSymbol 코인 심볼 (예: BTC, ETH)
     * @return 가격 문자열
     */
    private String getPrice(String coinSymbol) {
        Double price = priceFetcher.getPrice(coinSymbol);
        if (price != null) {
            return String.format("%.2f KRW", price);
        } else {
            return "N/A";
        }
    }

    /**
     * 버튼 생성 메서드 업데이트 (정수 및 소수점 초기값 설정)
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
     * 정수 버튼을 생성하는 메서드
     *
     * @param amount 초기 정수 값
     * @return 정수 버튼 아이템
     */
    private ItemStack createIntegerButton(int amount) {
        String displayName = ChatColor.WHITE + "Amount: " + amount;
        ItemStack button = new ItemStack(Material.STONE_BUTTON);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Shift + 좌클릭: +10",
                    ChatColor.GRAY + "Shift + 우클릭: -10",
                    ChatColor.GRAY + "좌클릭: +1",
                    ChatColor.GRAY + "우클릭: -1"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    /**
     * 소수점 버튼을 생성하는 메서드
     *
     * @param decimalPlace 소수점 자리 (1-5)
     * @param digit         초기 소수점 값
     * @return 소수점 버튼 아이템
     */
    private ItemStack createDecimalButton(int decimalPlace, int digit) {
        String placeText;
        switch (decimalPlace) {
            case 1:
                placeText = "첫째 자리";
                break;
            case 2:
                placeText = "둘째 자리";
                break;
            case 3:
                placeText = "셋째 자리";
                break;
            case 4:
                placeText = "넷째 자리";
                break;
            case 5:
                placeText = "다섯째 자리";
                break;
            default:
                placeText = "소수점";
                break;
        }

        String displayName = ChatColor.WHITE + "소수점 " + placeText + ": " + digit;
        ItemStack button = new ItemStack(Material.OAK_BUTTON);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "좌클릭: +1",
                    ChatColor.GRAY + "우클릭: -1"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    /**
     * 코인 가격을 업데이트하는 메서드 (실시간 반영 필요 시 호출)
     *
     * @param inventory 인벤토리
     */
    public void updatePrices(Inventory inventory) {
        // 슬롯 0: Bitcoin 가격 업데이트
        ItemStack btcPrice = createItem(Material.GOLD_BLOCK, ChatColor.GOLD + "Bitcoin (BTC)");
        ItemMeta btcMeta = btcPrice.getItemMeta();
        if (btcMeta != null) {
            btcMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getPrice("BTC")));
            btcPrice.setItemMeta(btcMeta);
        }
        inventory.setItem(0, btcPrice);

        // 슬롯 9: Ethereum 가격 업데이트
        ItemStack ethPrice = createItem(Material.DIAMOND_BLOCK, ChatColor.AQUA + "Ethereum (ETH)");
        ItemMeta ethMeta = ethPrice.getItemMeta();
        if (ethMeta != null) {
            ethMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + getPrice("ETH")));
            ethPrice.setItemMeta(ethMeta);
        }
        inventory.setItem(9, ethPrice);

        // 슬롯 18: Doge 가격 업데이트
        ItemStack dogePrice = createItem(Material.GOLD_INGOT, ChatColor.YELLOW + "Doge Price");
        ItemMeta dogePriceMeta = dogePrice.getItemMeta();
        if (dogePriceMeta != null) {
            dogePriceMeta.setLore(Arrays.asList(ChatColor.GREEN + getPrice("DOGE")));
            dogePrice.setItemMeta(dogePriceMeta);
        }
        inventory.setItem(18, dogePrice);

        // 슬롯 27: USDT 가격 업데이트
        ItemStack usdtPrice = createItem(Material.EMERALD_BLOCK, ChatColor.YELLOW + "USDT Price");
        ItemMeta usdtPriceMeta = usdtPrice.getItemMeta();
        if (usdtPriceMeta != null) {
            usdtPriceMeta.setLore(Arrays.asList(ChatColor.GREEN + getPrice("USDT")));
            usdtPrice.setItemMeta(usdtPriceMeta);
        }
        inventory.setItem(27, usdtPrice);
    }

    /**
     * 플레이어의 잔고 정보를 업데이트하는 메서드 (추후 구현 가능)
     *
     * @param inventory 인벤토리
     * @param player    플레이어
     */
    public void updatePlayerInfo(Inventory inventory, Player player) {
        // 슬롯 45: 플레이어 머리 및 로어 업데이트
        ItemStack playerHead = createPlayerHead(player);
        inventory.setItem(45, playerHead);
    }
}