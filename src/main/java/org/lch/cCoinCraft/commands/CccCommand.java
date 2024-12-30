package org.lch.cCoinCraft.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.lch.cCoinCraft.service.BtcTransactionService;
import org.lch.cCoinCraft.database.PlayerDAO;

public class CccCommand implements CommandExecutor {

    private final PlayerDAO playerDAO;
    private final BtcTransactionService transactionService;

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

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "[CCC] Incorrect command format. Usage: /ccc <coin> [buy/sell/balance] <amount>");
            return true;
        }

        String coinType = args[0].toUpperCase();
        String action = args[1].toLowerCase();

        switch (action) {
            case "buy":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "[CCC] Usage: /ccc " + coinType + " buy <amount>");
                    return true;
                }
                double buyAmount = parseDoubleSafe(args[2]);
                if (buyAmount <= 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
                    return true;
                }
                transactionService.buyCoin(player, coinType, buyAmount);
                break;

            case "sell":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "[CCC] Usage: /ccc " + coinType + " sell <amount>");
                    return true;
                }
                double sellAmount = parseDoubleSafe(args[2]);
                if (sellAmount <= 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
                    return true;
                }
                transactionService.sellCoin(player, coinType, sellAmount);
                break;

            case "balance":
                transactionService.showBalance(player, coinType);
                break;

            default:
                player.sendMessage(ChatColor.RED + "[CCC] Invalid action: " + action);
                break;
        }

        return true;
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