package org.lch.cCoinCraft.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.lch.cCoinCraft.service.BtcTransactionService;
import org.lch.cCoinCraft.service.CoinGeckoPriceFetcher;
import org.lch.cCoinCraft.database.PlayerDAO;
import org.lch.cCoinCraft.gui.CccGui;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CccCommand implements CommandExecutor {

    private final PlayerDAO playerDAO;
    private final BtcTransactionService transactionService;
    private final CoinGeckoPriceFetcher priceFetcher;
    private final CccGui cccGui;

    // 코인 리스트를 클래스 내에서 관리
    private static final List<String> COINS = Arrays.asList("BTC", "ETH", "DOGE", "USDT");

    // 어드민 명령어 리스트
    private static final List<String> ADMIN_ACTIONS = Arrays.asList("give", "set", "take");

    public CccCommand(PlayerDAO playerDAO, BtcTransactionService transactionService, CoinGeckoPriceFetcher priceFetcher, CccGui cccGui) {
        this.playerDAO = playerDAO;
        this.transactionService = transactionService;
        this.priceFetcher = priceFetcher;
        this.cccGui = cccGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "[CCC] Incorrect command format. Usage: /ccc [buy/sell/balance/wallet/price/give/set/take/gui] <coin> <amount>");
            return true;
        }

        String action = args[0].toLowerCase();

        // GUI 열기 명령어 처리
        if (action.equals("gui")) {
            cccGui.openGui(player);
            return true;
        }

        // 어드민 명령어 처리
        if (ADMIN_ACTIONS.contains(action)) {
            if (!player.hasPermission("ccoincraft.admin")) {
                player.sendMessage(ChatColor.RED + "[CCC] You do not have permission to use this command.");
                return true;
            }
            handleAdminCommand(player, action, args);
            return true;
        }

        // 일반 사용자 명령어 처리
        switch (action) {
            case "buy":
                handleBuyCommand(player, args);
                break;

            case "sell":
                handleSellCommand(player, args);
                break;

            case "balance":
            case "bal":
                showBalance(player, args);
                break;

            case "wallet":
                showWallet(player);
                break;

            case "price":
                showPrices(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "[CCC] Invalid action: " + action);
                break;
        }

        return true;
    }

    /**
     * 어드민 명령어를 처리하는 메서드
     */
    private void handleAdminCommand(Player player, String action, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "[CCC] Usage: /ccc " + action + " <player> <coin> <amount>");
            return;
        }

        String targetPlayerName = args[1];
        String coinType = args[2].toUpperCase();
        double amount = parseDoubleSafe(args[3]);

        if (!COINS.contains(coinType)) {
            player.sendMessage(ChatColor.RED + "[CCC] Unsupported coin type: " + coinType);
            return;
        }

        // 타겟 플레이어 가져오기
        Player targetPlayer = player.getServer().getPlayerExact(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "[CCC] Player not found: " + targetPlayerName);
            return;
        }

        UUID targetUUID = targetPlayer.getUniqueId();

        switch (action) {
            case "give":
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
                    return;
                }
                playerDAO.giveCoin(targetUUID, coinType, amount);
                player.sendMessage(ChatColor.GREEN + "[CCC] Gave " + formatCurrency(amount) + " " + coinType + " to " + targetPlayerName + ".");
                break;

            case "set":
                if (amount < 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Amount cannot be negative.");
                    return;
                }
                playerDAO.setCoinBalance(targetUUID, coinType, amount);
                player.sendMessage(ChatColor.GREEN + "[CCC] Set " + targetPlayerName + "'s " + coinType + " to " + formatCurrency(amount) + ".");
                break;

            case "take":
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
                    return;
                }
                playerDAO.takeCoin(targetUUID, coinType, amount);
                player.sendMessage(ChatColor.GREEN + "[CCC] Took " + formatCurrency(amount) + " " + coinType + " from " + targetPlayerName + ".");
                break;

            default:
                player.sendMessage(ChatColor.RED + "[CCC] Invalid admin action: " + action);
                break;
        }
    }

    /**
     * /ccc buy <coin> [amount|all]
     */
    private void handleBuyCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "[CCC] Usage: /ccc buy <coin> <amount|all>");
            return;
        }

        String coinType = args[1].toUpperCase();

        if (!COINS.contains(coinType)) {
            player.sendMessage(ChatColor.RED + "[CCC] Unsupported coin type: " + coinType);
            return;
        }

        if (args.length >= 3) {
            String amountArg = args[2].toLowerCase();
            if (amountArg.equals("all")) {
                transactionService.buyAllCoin(player, coinType);
            } else {
                double amount = parseDoubleSafe(args[2]);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
                    return;
                }
                transactionService.buyCoin(player, coinType, amount);
            }
        } else {
            player.sendMessage(ChatColor.RED + "[CCC] Usage: /ccc buy <coin> <amount|all>");
        }
    }

    /**
     * /ccc sell <coin> [amount|all]
     */
    private void handleSellCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "[CCC] Usage: /ccc sell <coin> <amount|all>");
            return;
        }

        String coinType = args[1].toUpperCase();

        if (!COINS.contains(coinType)) {
            player.sendMessage(ChatColor.RED + "[CCC] Unsupported coin type: " + coinType);
            return;
        }

        if (args.length >= 3) {
            String amountArg = args[2].toLowerCase();
            if (amountArg.equals("all")) {
                transactionService.sellAllCoin(player, coinType);
            } else {
                double amount = parseDoubleSafe(args[2]);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
                    return;
                }
                transactionService.sellCoin(player, coinType, amount);
            }
        } else {
            player.sendMessage(ChatColor.RED + "[CCC] Usage: /ccc sell <coin> <amount|all>");
        }
    }

    /**
     * 특정 코인 잔액을 표시하는 메서드
     *
     * @param player 플레이어
     * @param args   명령어 인자
     */
    private void showBalance(Player player, String[] args) {
        if (args.length == 1) {
            player.sendMessage(ChatColor.RED + "[CCC] Incorrect command format. Usage: /ccc balance <coin>");
            return;
        } else if (args.length == 2) {
            // /ccc balance <coin>
            String coinType = args[1].toUpperCase();
            if (!COINS.contains(coinType)) {
                player.sendMessage(ChatColor.RED + "[CCC] Unsupported coin type: " + coinType);
                return;
            }
            transactionService.showBalance(player, coinType);
        } else {
            player.sendMessage(ChatColor.RED + "[CCC] Incorrect command format. Usage: /ccc balance <coin>");
        }
    }

    /**
     * 모든 코인과 화폐 잔액을 표시하는 메서드 (wallet 명령어용)
     *
     * @param player 플레이어
     */
    private void showWallet(Player player) {
        transactionService.showWallet(player);
    }

    /**
     * 모든 코인의 현재 가격을 표시하는 메서드 (price 명령어용)
     *
     * @param player 플레이어
     */
    private void showPrices(Player player) {
        // 모든 코인의 심볼을 사용하여 가격을 가져옵니다.
        StringBuilder priceMessage = new StringBuilder();
        priceMessage.append(ChatColor.GOLD).append("[CCC] Current Prices:\n");

        for (String coin : COINS) {
            Double price = priceFetcher.getPrice(coin);
            if (price != null) {
                priceMessage.append(ChatColor.YELLOW).append(coin).append(": ").append(formatCurrency(price)).append("\n");
            } else {
                priceMessage.append(ChatColor.RED).append(coin).append(": ").append("Price not available\n");
            }
        }

        player.sendMessage(priceMessage.toString());
    }

    /**
     * 화폐 금액 포맷 (소수점 이하 2자리, 천 단위 콤마 추가)
     */
    private String formatCurrency(double amount) {
        return String.format("%,.2f", amount);
    }

    /**
     * 안전하게 문자열을 double로 변환
     */
    private double parseDoubleSafe(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }
}