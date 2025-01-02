package org.lch.cCoinCraft.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.lch.cCoinCraft.CCoinCraft;
import org.lch.cCoinCraft.database.HistoryDAO;
import org.lch.cCoinCraft.database.PlayerDAO;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.util.Map;

/**
 * 여러 코인의 구매/판매 로직 담당 (클래스 이름은 BtcTransactionService로 유지)
 */
public class BtcTransactionService {

    private final PlayerDAO playerDAO;
    private final HistoryDAO historyDAO;
    private final CoinGeckoPriceFetcher priceFetcher;
    private final double fee; // 수수료 필드

    // 소수점 고정 형식: 최대 8자리 소수점까지 표시 (불필요한 0은 제거)
    private static final DecimalFormat COIN_FORMAT = new DecimalFormat("0.########");

    // 화폐 단위를 상수로 정의 (미래 확장을 위해)
    private static final String CURRENCY_UNIT = ""; // 현재는 빈 문자열, 필요 시 변경 가능

    public BtcTransactionService(PlayerDAO playerDAO, HistoryDAO historyDAO, CoinGeckoPriceFetcher priceFetcher, JavaPlugin plugin) {
        this.playerDAO = playerDAO;
        this.historyDAO = historyDAO;
        this.priceFetcher = priceFetcher;
        double configFee = plugin.getConfig().getDouble("transaction_fee", 0.0);
        if (configFee < 0 || configFee > 1) {
            plugin.getLogger().warning("Transaction fee is out of bounds (0-1). Defaulting to 0.");
            this.fee = 0.0;
        } else {
            this.fee = configFee;
        }
    }

    /**
     * /ccc buy <coin> <amount>
     * 화폐 -> 코인 구매
     */
    public void buyCoin(Player player, String coinType, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
            return;
        }

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

        double grossCost = coinPrice * amount; // 수수료 적용 전 총 비용
        double feeAmount = grossCost * fee;    // 수수료 금액
        double totalCost = grossCost + feeAmount; // 수수료 포함 총 비용

        double balance = economy.getBalance(player);

        if (balance < totalCost) {
            player.sendMessage(ChatColor.RED + "[CCC] Insufficient currency! Need " + formatCurrency(totalCost) + " (including a fee of " + formatCurrency(feeAmount) + ") but you only have " + formatCurrency(balance));
            return;
        }

        // 결제
        economy.withdrawPlayer(player, totalCost);
        // DB에 코인 추가 및 거래 내역 기록
        playerDAO.updateCoinBalance(player.getUniqueId(), upperCoinType, amount);
        historyDAO.insertTransaction(
                player.getUniqueId(),
                player.getName(),
                upperCoinType,
                amount,
                "BUY"
        );

        // 로그
        String amountFormatted = COIN_FORMAT.format(amount);
        String totalCostFormatted = formatCurrency(totalCost);
        String feeFormatted = formatCurrency(feeAmount);
        player.sendMessage(ChatColor.GREEN + "[CCC] Successfully purchased " + amountFormatted + " " + upperCoinType + " for " + totalCostFormatted + " currency!");
        player.sendMessage(ChatColor.YELLOW + "[CCC] A fee of " + feeFormatted + " currency has been applied.");
    }

    /**
     * /ccc buy <coin> all
     * 사용자의 화폐 잔액을 모두 사용하여 최대한의 코인 구매
     */
    public void buyAllCoin(Player player, String coinType) {
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

        double currencyBal = economy.getBalance(player);
        double feePercentage = fee;

        // 수수료를 고려한 최대 구매 가능한 금액 계산
        double maxGrossCost = currencyBal / (1 + feePercentage);
        double maxAmount = maxGrossCost / coinPrice;

        if (maxAmount <= 0) {
            player.sendMessage(ChatColor.RED + "[CCC] Insufficient currency to buy any " + upperCoinType + ".");
            return;
        }

        // 실제 구매 금액 계산 (소수점 8자리로 제한)
        maxAmount = Math.floor(maxAmount * 1e8) / 1e8;

        double feeAmount = maxGrossCost * feePercentage;
        double totalCost = maxGrossCost + feeAmount;

        // 결제
        economy.withdrawPlayer(player, totalCost);
        // DB에 코인 추가 및 거래 내역 기록
        playerDAO.updateCoinBalance(player.getUniqueId(), upperCoinType, maxAmount);
        historyDAO.insertTransaction(
                player.getUniqueId(),
                player.getName(),
                upperCoinType,
                maxAmount,
                "BUY_ALL"
        );

        // 로그
        String amountFormatted = COIN_FORMAT.format(maxAmount);
        String totalCostFormatted = formatCurrency(totalCost);
        String feeFormatted = formatCurrency(feeAmount);
        player.sendMessage(ChatColor.GREEN + "[CCC] Successfully purchased " + amountFormatted + " " + upperCoinType + " for " + totalCostFormatted + " currency!");
        player.sendMessage(ChatColor.YELLOW + "[CCC] A fee of " + feeFormatted + " currency has been applied.");
    }

    /**
     * /ccc sell <coin> <amount>
     * 코인 -> 화폐 판매
     */
    public void sellCoin(Player player, String coinType, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
            return;
        }

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

        playerDAO.getCoinBalance(player.getUniqueId(), upperCoinType, (coinBalance) -> {
            if (coinBalance < amount) {
                String required = COIN_FORMAT.format(amount);
                String available = COIN_FORMAT.format(coinBalance);
                player.sendMessage(ChatColor.RED + "[CCC] Insufficient " + upperCoinType + "! Need " + required + " " + upperCoinType + " but have only " + available + " " + upperCoinType + ".");
                return;
            }

            double grossIncome = coinPrice * amount; // 수수료 적용 전 금액
            double feeAmount = grossIncome * fee;    // 수수료 금액
            double netIncome = grossIncome - feeAmount; // 수수료 차감 후 금액

            // DB에서 코인 차감 및 거래 내역 기록
            playerDAO.updateCoinBalance(player.getUniqueId(), upperCoinType, -amount);
            historyDAO.insertTransaction(
                    player.getUniqueId(),
                    player.getName(),
                    upperCoinType,
                    -amount,
                    "SELL"
            );

            // 화폐 지급
            economy.depositPlayer(player, netIncome);

            // 로그
            String amountFormatted = COIN_FORMAT.format(amount);
            String netIncomeFormatted = formatCurrency(netIncome);
            String feeFormatted = formatCurrency(feeAmount);
            player.sendMessage(ChatColor.GREEN + "[CCC] Successfully sold " + amountFormatted + " " + upperCoinType + " for " + netIncomeFormatted + " currency!");
            player.sendMessage(ChatColor.YELLOW + "[CCC] A fee of " + feeFormatted + " currency has been deducted.");
        });
    }

    /**
     * /ccc sell <coin> all
     * 사용자가 보유한 모든 코인을 판매
     */
    public void sellAllCoin(Player player, String coinType) {
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

        playerDAO.getCoinBalance(player.getUniqueId(), upperCoinType, (coinBalance) -> {
            if (coinBalance <= 0) {
                player.sendMessage(ChatColor.RED + "[CCC] You have no " + upperCoinType + " to sell.");
                return;
            }

            double grossIncome = coinPrice * coinBalance; // 수수료 적용 전 금액
            double feeAmount = grossIncome * fee;         // 수수료 금액
            double netIncome = grossIncome - feeAmount;   // 수수료 차감 후 금액

            // DB에서 코인 차감 및 거래 내역 기록
            playerDAO.updateCoinBalance(player.getUniqueId(), upperCoinType, -coinBalance);
            historyDAO.insertTransaction(
                    player.getUniqueId(),
                    player.getName(),
                    upperCoinType,
                    -coinBalance,
                    "SELL_ALL"
            );

            // 화폐 지급
            economy.depositPlayer(player, netIncome);

            // 로그
            String amountFormatted = COIN_FORMAT.format(coinBalance);
            String netIncomeFormatted = formatCurrency(netIncome);
            String feeFormatted = formatCurrency(feeAmount);
            player.sendMessage(ChatColor.GREEN + "[CCC] Successfully sold " + amountFormatted + " " + upperCoinType + " for " + netIncomeFormatted + " currency!");
            player.sendMessage(ChatColor.YELLOW + "[CCC] A fee of " + feeFormatted + " currency has been deducted.");
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
            String feePercentage = String.format("%.2f%%", fee * 100);

            player.sendMessage(ChatColor.GOLD + "[CCC] Your Currency: " + currencyFormatted);
            player.sendMessage(ChatColor.GOLD + "[CCC] Your " + upperCoinType + ": " + coinFormatted + " " + upperCoinType);
            player.sendMessage(ChatColor.YELLOW + "[CCC] Current Transaction Fee: " + feePercentage);
        });
    }

    /**
     * /ccc wallet
     * 모든 코인 잔액과 화폐 잔액을 표시, 각 코인의 현재 가치와 총 재산도 표시
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

            double totalAsset = currencyBal; // 총 재산 초기화 (화폐 잔액 포함)

            if (coinBalances.isEmpty()) {
                walletMessage.append(ChatColor.YELLOW).append("No coins found.\n");
            } else {
                for (Map.Entry<String, Double> entry : coinBalances.entrySet()) {
                    String coin = entry.getKey().toUpperCase();
                    double balance = entry.getValue();
                    String coinFormatted = COIN_FORMAT.format(balance);

                    // 캐시된 가격 가져오기
                    Double coinPrice = priceFetcher.getPrice(coin);
                    if (coinPrice == null) {
                        walletMessage.append(ChatColor.RED).append(coin).append(": ").append(coinFormatted)
                                .append(" ").append(coin).append(" (Price unavailable)\n");
                        continue;
                    }

                    // 코인의 현재 가치를 계산
                    double coinValue = balance * coinPrice;
                    String coinValueFormatted = formatCurrency(coinValue);
                    totalAsset += coinValue;

                    walletMessage.append(ChatColor.YELLOW).append(coin).append(": ").append(coinFormatted)
                            .append(" ").append(coin).append(" (").append(coinValueFormatted).append(")\n");
                }
            }

            // 총 재산 표시
            String totalAssetFormatted = formatCurrency(totalAsset);
            walletMessage.append(ChatColor.GREEN).append("Total Assets: ").append(totalAssetFormatted);

            player.sendMessage(walletMessage.toString());
        });
    }

    /**
     * 화폐 금액 포맷 (소수점 이하 2자리, 천 단위 콤마 추가)
     */
    private String formatCurrency(double amount) {
        return String.format("%,.2f", amount);
    }
}