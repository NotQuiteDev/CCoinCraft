package org.lch.cCoinCraft.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CccCommandTabCompleter implements TabCompleter {

    private static final List<String> COINS = Arrays.asList("BTC", "ETH", "DOGE", "USDT");
    private static final List<String> ACTIONS = Arrays.asList("buy", "sell", "balance", "bal", "wallet", "price");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 첫 번째 인자는 액션
            for (String action : ACTIONS) {
                if (action.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(action);
                }
            }
        } else if (args.length == 2) {
            // 두 번째 인자는 코인 타입 (buy/sell/balance/bal 명령어일 경우)
            String action = args[0].toLowerCase();
            if (action.equals("buy") || action.equals("sell") || action.equals("balance") || action.equals("bal")) {
                for (String coin : COINS) {
                    if (coin.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(coin);
                    }
                }
            }
        } else if (args.length == 3) {
            // 세 번째 인자는 수량 (buy/sell 명령어일 경우에만)
            String action = args[0].toLowerCase();
            if (action.equals("buy") || action.equals("sell")) {
                completions.add("<amount>");
            }
        }

        return completions;
    }
}