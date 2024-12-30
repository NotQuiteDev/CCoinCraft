package org.lch.cCoinCraft.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.lch.cCoinCraft.service.BtcTransactionService;
import org.lch.cCoinCraft.database.PlayerDAO;

import java.util.Arrays;
import java.util.List;

public class CccCommand implements CommandExecutor {

    private final PlayerDAO playerDAO;
    private final BtcTransactionService transactionService;

    // 코인 리스트를 클래스 내에서 관리
    private static final List<String> COINS = Arrays.asList("BTC", "ETH", "DOGE", "USDT");

    public CccCommand(PlayerDAO playerDAO, BtcTransactionService transactionService) {
        this.playerDAO = playerDAO;
        this.transactionService = transactionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "[CCC] Incorrect command format. Usage: /ccc [buy/sell/balance/wallet] <coin> <amount>");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "buy":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "[CCC] Usage: /ccc buy <coin> <amount>");
                    return true;
                }
                String buyCoinType = args[1].toUpperCase();
                if (!COINS.contains(buyCoinType)) {
                    player.sendMessage(ChatColor.RED + "[CCC] Unsupported coin type: " + buyCoinType);
                    return true;
                }
                double buyAmount = parseDoubleSafe(args[2]);
                if (buyAmount <= 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
                    return true;
                }
                transactionService.buyCoin(player, buyCoinType, buyAmount);
                break;

            case "sell":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "[CCC] Usage: /ccc sell <coin> <amount>");
                    return true;
                }
                String sellCoinType = args[1].toUpperCase();
                if (!COINS.contains(sellCoinType)) {
                    player.sendMessage(ChatColor.RED + "[CCC] Unsupported coin type: " + sellCoinType);
                    return true;
                }
                double sellAmount = parseDoubleSafe(args[2]);
                if (sellAmount <= 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
                    return true;
                }
                transactionService.sellCoin(player, sellCoinType, sellAmount);
                break;

            case "balance":
            case "bal":
                showBalance(player, args);
                break;

            case "wallet":
                showWallet(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "[CCC] Invalid action: " + action);
                break;
        }

        return true;
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