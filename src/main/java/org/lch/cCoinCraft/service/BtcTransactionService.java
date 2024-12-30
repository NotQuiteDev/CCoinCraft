package org.lch.cCoinCraft.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.lch.cCoinCraft.CCoinCraft;
import org.lch.cCoinCraft.database.BtcHistoryDAO;
import org.lch.cCoinCraft.database.HistoryDAO;
import org.lch.cCoinCraft.database.PlayerDAO;

import java.text.DecimalFormat;
import java.util.Map;

/**
 * 여러 코인의 구매/판매 로직 담당 (클래스 이름은 BtcTransactionService로 유지)
 */
public class BtcTransactionService {

    private final PlayerDAO playerDAO;
    private final BtcHistoryDAO btcHistoryDAO; // BTC 거래 내역
    private final HistoryDAO historyDAO; // 모든 거래 내역
    private final CoinGeckoPriceFetcher priceFetcher;

    // 소수점 고정 형식: 8자리 소수점까지 표시 (사토시 단위 등)
    private static final DecimalFormat COIN_FORMAT = new DecimalFormat("0.00000000");

    public BtcTransactionService(PlayerDAO playerDAO, BtcHistoryDAO btcHistoryDAO, HistoryDAO historyDAO, CoinGeckoPriceFetcher priceFetcher) {
        this.playerDAO = playerDAO;
        this.btcHistoryDAO = btcHistoryDAO;
        this.historyDAO = historyDAO;
        this.priceFetcher = priceFetcher;
    }

    /**
     * /ccc <coin> buy <amount>
     * 화폐 -> 코인 구매
     */
    public void buyCoin(Player player, String coinType, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
            return;
        }

        // 대문자로 변환
        String upperCoinType = coinType.toUpperCase();
        Double coinPrice = priceFetcher.getPrice(upperCoinType);
        if (coinPrice == null) {
            player.sendMessage(ChatColor.RED + "[CCC] Unsupported coin type: " + upperCoinType);
            return;
        }

        Economy economy = CCoinCraft.getEconomy();
        if (economy == null) {
            player.sendMessage(ChatColor.RED + "[CCC] Vault or Economy is not available.");
            return;
        }

        double totalCost = coinPrice * amount; // 총 비용
        double balance = economy.getBalance(player);

        if (balance < totalCost) {
            // 잔고 부족
            player.sendMessage(ChatColor.RED + "[CCC] Insufficient currency! Need " + formatCurrency(totalCost) + " but you only have " + formatCurrency(balance));
            return;
        }

        // 결제
        economy.withdrawPlayer(player, totalCost);
        // DB에 코인 추가
        playerDAO.updateCoinBalance(player.getUniqueId(), upperCoinType, amount);

        // 로그
        String amountFormatted = COIN_FORMAT.format(amount);
        String priceFormatted = formatCurrency(totalCost);
        player.sendMessage(ChatColor.GREEN + "[CCC] Successfully purchased " + amountFormatted + " " + upperCoinType + " for " + priceFormatted + " currency!");

        // History 테이블 기록 (코인 단위)
        if (upperCoinType.equals("BTC")) {
            btcHistoryDAO.insertHistory(
                    player.getUniqueId().toString(),
                    player.getName(),
                    amount,
                    "BUY_BTC"
            );
        }

        // 모든 거래 내역 기록 (HistoryDAO)
        historyDAO.insertTransaction(
                player.getUniqueId(),
                player.getName(),
                upperCoinType,
                amount,
                "BUY"
        );
    }

    /**
     * /ccc <coin> sell <amount>
     * 코인 -> 화폐 판매
     */
    public void sellCoin(Player player, String coinType, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
            return;
        }

        // 대문자로 변환
        String upperCoinType = coinType.toUpperCase();
        Double coinPrice = priceFetcher.getPrice(upperCoinType);
        if (coinPrice == null) {
            player.sendMessage(ChatColor.RED + "[CCC] Unsupported coin type: " + upperCoinType);
            return;
        }

        Economy economy = CCoinCraft.getEconomy();
        if (economy == null) {
            player.sendMessage(ChatColor.RED + "[CCC] Vault or Economy is not available.");
            return;
        }

        // 플레이어의 코인 잔고 확인 (비동기 호출)
        playerDAO.getCoinBalance(player.getUniqueId(), upperCoinType, (coinBalance) -> {
            if (coinBalance < amount) {
                // 코인 잔고 부족
                String required = COIN_FORMAT.format(amount);
                String available = COIN_FORMAT.format(coinBalance);
                player.sendMessage(ChatColor.RED + "[CCC] Insufficient " + upperCoinType + "! Need " + required + " " + upperCoinType + " but have only " + available + " " + upperCoinType + ".");
                return;
            }

            double income = coinPrice * amount; // 얻을 화폐

            // DB에서 코인 차감
            playerDAO.updateCoinBalance(player.getUniqueId(), upperCoinType, -amount);

            // 화폐 지급
            economy.depositPlayer(player, income);

            // 로그
            String amountFormatted = COIN_FORMAT.format(amount);
            String incomeFormatted = formatCurrency(income);
            player.sendMessage(ChatColor.GREEN + "[CCC] Successfully sold " + amountFormatted + " " + upperCoinType + " for " + incomeFormatted + " currency!");

            // History 테이블 기록 (코인 단위)
            if (upperCoinType.equals("BTC")) {
                btcHistoryDAO.insertHistory(
                        player.getUniqueId().toString(),
                        player.getName(),
                        -amount, // 음수로 기록
                        "SELL_BTC"
                );
            }

            // 모든 거래 내역 기록 (HistoryDAO)
            historyDAO.insertTransaction(
                    player.getUniqueId(),
                    player.getName(),
                    upperCoinType,
                    -amount,
                    "SELL"
            );
        });
    }

    /**
     * /ccc <coin> balance
     * 특정 코인의 잔액과 화폐 잔액을 표시
     */
    public void showBalance(Player player, String coinType) {
        String upperCoinType = coinType.toUpperCase();
        Economy economy = CCoinCraft.getEconomy();
        if (economy == null) {
            player.sendMessage(ChatColor.RED + "[CCC] Vault or Economy is not available.");
            return;
        }

        playerDAO.getCoinBalance(player.getUniqueId(), upperCoinType, (coinBalance) -> {
            if (coinBalance < 0) {
                player.sendMessage(ChatColor.RED + "[CCC] Unsupported coin type: " + upperCoinType);
                return;
            }

            double currencyBal = economy.getBalance(player);

            String currencyFormatted = formatCurrency(currencyBal);
            String coinFormatted = COIN_FORMAT.format(coinBalance);

            player.sendMessage(ChatColor.GOLD + "[CCC] Your Currency: " + currencyFormatted);
            player.sendMessage(ChatColor.GOLD + "[CCC] Your " + upperCoinType + ": " + coinFormatted + " " + upperCoinType);
        });
    }

    /**
     * /ccc wallet
     * 모든 코인 잔액과 화폐 잔액을 표시
     */
    public void showWallet(Player player) {
        Economy economy = CCoinCraft.getEconomy();
        if (economy == null) {
            player.sendMessage(ChatColor.RED + "[CCC] Vault or Economy is not available.");
            return;
        }

        double currencyBal = economy.getBalance(player);
        String currencyFormatted = formatCurrency(currencyBal);

        playerDAO.getAllCoinBalances(player.getUniqueId(), (coinBalances) -> {
            StringBuilder walletMessage = new StringBuilder();
            walletMessage.append(ChatColor.GOLD).append("[CCC] Your Wallet:\n");
            walletMessage.append(ChatColor.GOLD).append("Currency: ").append(currencyFormatted).append("\n");

            if (coinBalances.isEmpty()) {
                walletMessage.append(ChatColor.YELLOW).append("No coins found.");
            } else {
                for (Map.Entry<String, Double> entry : coinBalances.entrySet()) {
                    String coin = entry.getKey().toUpperCase();
                    double balance = entry.getValue();
                    String coinFormatted = COIN_FORMAT.format(balance);
                    walletMessage.append(ChatColor.YELLOW).append(coin).append(": ").append(coinFormatted).append(" ").append(coin).append("\n");
                }
            }

            player.sendMessage(walletMessage.toString());
        });
    }

    /**
     * 화폐 금액 포맷 (천 단위 콤마 추가)
     */
    private String formatCurrency(double amount) {
        return String.format("%,.0f", amount);
    }
}