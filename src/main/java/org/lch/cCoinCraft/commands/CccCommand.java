package org.lch.cCoinCraft.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.lch.cCoinCraft.CCoinCraft;
import org.lch.cCoinCraft.database.PlayerDAO;
import org.lch.cCoinCraft.service.BtcTransactionService;

public class CccCommand implements CommandExecutor {

    private final PlayerDAO playerDAO;
    private final BtcTransactionService transactionService;

    public CccCommand(PlayerDAO playerDAO, BtcTransactionService transactionService) {
        this.playerDAO = playerDAO;
        this.transactionService = transactionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /ccc btc [buy/sell/balance] <amount>
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "[CCC] Incorrect command format. Usage: /ccc btc [buy/sell/balance] <amount>");
            return true;
        }

        String subCmd = args[0].toLowerCase(); // "btc"

        // 예: "/ccc btc buy 5"
        if (subCmd.equals("btc")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "[CCC] Incorrect command format. Usage: /ccc btc [buy/sell/balance] <amount>");
                return true;
            }
            String action = args[1].toLowerCase(); // buy / sell / balance
            if (action.equals("buy")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "[CCC] Incorrect command format. Usage: /ccc btc buy <amount>");
                    return true;
                }
                double amount = parseDoubleSafe(args[2]);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
                    return true;
                }
                transactionService.buyBitcoin(player, amount);

            } else if (action.equals("sell")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "[CCC] Incorrect command format. Usage: /ccc btc sell <amount>");
                    return true;
                }
                double amount = parseDoubleSafe(args[2]);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "[CCC] Invalid amount.");
                    return true;
                }
                transactionService.sellBitcoin(player, amount);

            } else if (action.equals("balance")) {
                transactionService.showBalance(player);

            } else {
                player.sendMessage(ChatColor.RED + "[CCC] Invalid action: " + action);
            }
            return true;
        }

        // 그 외 "/ccc ..." 서브명령들은 여기서 처리
        return true;
    }

    private double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}