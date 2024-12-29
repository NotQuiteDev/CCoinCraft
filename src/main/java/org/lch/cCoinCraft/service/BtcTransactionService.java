package org.lch.cCoinCraft.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.lch.cCoinCraft.CCoinCraft;
import org.lch.cCoinCraft.database.BtcHistoryDAO;
import org.lch.cCoinCraft.database.PlayerDAO;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * BTC 구매/판매 로직 담당
 */
public class BtcTransactionService {

    private final PlayerDAO playerDAO;
    private final BtcHistoryDAO btcHistoryDAO; // 거래 내역 DB or 파일 기록에도 재사용 가능

    // 임시로 정한 BTC 가격 (1 BTC 당 140,000,000 화폐) - 실제로는 변동 환율, Coingecko 연동 등 가능
    private static final double BTC_PRICE = 140000000.0;

    // 소수점 고정 형식: 8자리 소수점까지 표시 (사토시 단위)
    private static final DecimalFormat BTC_FORMAT = new DecimalFormat("0.00000000");

    public BtcTransactionService(PlayerDAO playerDAO, BtcHistoryDAO btcHistoryDAO) {
        this.playerDAO = playerDAO;
        this.btcHistoryDAO = btcHistoryDAO;
    }

    /**
     * /ccc btc buy <amount>
     * 화폐 -> BTC 구매
     */
    public void buyBitcoin(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
            return;
        }
        Economy economy = CCoinCraft.getEconomy();
        if (economy == null) {
            player.sendMessage(ChatColor.RED + "[CCC] Vault or Economy is not available.");
            return;
        }

        double price = BTC_PRICE * amount; // 총 비용
        double balance = economy.getBalance(player);

        if (balance < price) {
            // 잔고 부족
            player.sendMessage(ChatColor.RED + "[CCC] Insufficient currency! Need " + formatCurrency(price) + " but you only have " + formatCurrency(balance));
            return;
        }

        // 결제
        economy.withdrawPlayer(player, price);
        // DB에 BTC 추가
        playerDAO.addBtcBalance(player.getUniqueId(), amount);

        // 로그
        String amountFormatted = BTC_FORMAT.format(amount);
        String priceFormatted = formatCurrency(price);
        player.sendMessage(ChatColor.GREEN + "[CCC] Successfully purchased " + amountFormatted + " BTC for " + priceFormatted + " currency!");

        // History 테이블 기록
        btcHistoryDAO.insertHistory(
                player.getUniqueId().toString(),
                player.getName(),
                amount,
                "BUY_BTC" // reason
        );

        // 거래 내역 파일 기록
        logTransactionToFile(player.getName(), "Buy", amount, price);
    }

    /**
     * /ccc btc sell <amount>
     * BTC -> 화폐 판매
     */
    public void sellBitcoin(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
            return;
        }
        Economy economy = CCoinCraft.getEconomy();
        if (economy == null) {
            player.sendMessage(ChatColor.RED + "[CCC] Vault or Economy is not available.");
            return;
        }

        // 플레이어의 BTC 잔고 확인 (DB에서 SELECT)
        double btcBalance = playerDAO.getBtcBalance(player.getUniqueId());
        if (btcBalance < amount) {
            // 비트코인 잔고 부족
            String required = BTC_FORMAT.format(amount);
            String available = BTC_FORMAT.format(btcBalance);
            player.sendMessage(ChatColor.RED + "[CCC] Insufficient Bitcoin! Need " + required + " BTC but have only " + available + " BTC.");
            return;
        }

        double income = BTC_PRICE * amount; // 얻을 화폐
        // DB에서 BTC 차감
        playerDAO.addBtcBalance(player.getUniqueId(), -amount);

        // 화폐 지급
        economy.depositPlayer(player, income);

        // 로그
        String amountFormatted = BTC_FORMAT.format(amount);
        String incomeFormatted = formatCurrency(income);
        player.sendMessage(ChatColor.GREEN + "[CCC] Successfully sold " + amountFormatted + " BTC for " + incomeFormatted + " currency!");

        // History 테이블 기록
        btcHistoryDAO.insertHistory(
                player.getUniqueId().toString(),
                player.getName(),
                -amount, // 음수로 기록
                "SELL_BTC"
        );

        // 거래 내역 파일 기록
        logTransactionToFile(player.getName(), "Sell", amount, income);
    }

    /**
     * /ccc btc balance
     */
    public void showBalance(Player player) {
        Economy economy = CCoinCraft.getEconomy();
        if (economy == null) {
            player.sendMessage(ChatColor.RED + "[CCC] Vault or Economy is not available.");
            return;
        }
        double currencyBal = economy.getBalance(player);
        double btcBal = playerDAO.getBtcBalance(player.getUniqueId());

        String currencyFormatted = formatCurrency(currencyBal);
        String btcFormatted = BTC_FORMAT.format(btcBal);

        player.sendMessage(ChatColor.GOLD + "[CCC] Your Currency: " + currencyFormatted);
        player.sendMessage(ChatColor.GOLD + "[CCC] Your Bitcoin: " + btcFormatted + " BTC");
    }

    /**
     * 거래 내역 파일 (transactions.txt)에 기록
     */
    private void logTransactionToFile(String playerName, String action, double amount, double price) {
        try {
            // 플러그인 폴더에 transactions.txt 생성 (없으면)
            java.io.File file = new java.io.File(CCoinCraft.getPlugin(CCoinCraft.class).getDataFolder(), "transactions.txt");
            if (!file.exists()) {
                file.createNewFile();
            }

            java.io.FileWriter writer = new java.io.FileWriter(file, true); // append mode

            // 시간 포맷
            String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String amountFormatted = BTC_FORMAT.format(amount);
            String priceFormatted = formatCurrency(price);
            String logLine = "[" + timeStr + "] Player: " + playerName
                    + " | Action: " + action
                    + " | Amount: " + amountFormatted + " BTC"
                    + " | Price: " + priceFormatted + " currency\n";

            writer.write(logLine);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 화폐 금액 포맷 (천 단위 콤마 추가)
     */
    private String formatCurrency(double amount) {
        return String.format("%,.0f", amount);
    }
}