package org.lch.cCoinCraft.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CccCommandTabCompleter implements TabCompleter {

    private static final List<String> COINS = Arrays.asList("BTC", "ETH", "DOGE", "USDT");
    private static final List<String> ACTIONS = Arrays.asList("buy", "sell", "balance", "bal", "wallet", "price");
    private static final List<String> ADMIN_ACTIONS = Arrays.asList("give", "set", "take");

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
            for (String adminAction : ADMIN_ACTIONS) {
                if (sender.hasPermission("ccoincraft.admin") && adminAction.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(adminAction);
                }
            }
        } else if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (ACTIONS.contains(action)) {
                // 두 번째 인자는 코인 타입 (buy/sell/balance/bal 명령어일 경우)
                if (action.equals("buy") || action.equals("sell") || action.equals("balance") || action.equals("bal")) {
                    for (String coin : COINS) {
                        if (coin.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(coin);
                        }
                    }
                }
            } else if (ADMIN_ACTIONS.contains(action)) {
                // 어드민 명령어 두 번째 인자는 플레이어 이름
                String playerArg = args[1].toLowerCase();
                List<String> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(playerArg))
                        .collect(Collectors.toList());
                completions.addAll(onlinePlayers);
            }
        } else if (args.length == 3) {
            String action = args[0].toLowerCase();
            if (ACTIONS.contains(action) && (action.equals("buy") || action.equals("sell"))) {
                // 세 번째 인자는 수량 또는 'all'
                completions.add("all");
                completions.add("<amount>");
            } else if (ADMIN_ACTIONS.contains(action)) {
                // 어드민 명령어 세 번째 인자는 코인 타입
                for (String coin : COINS) {
                    if (coin.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(coin);
                    }
                }
            }
        } else if (args.length == 4) {
            String action = args[0].toLowerCase();
            if (ADMIN_ACTIONS.contains(action)) {
                // 네 번째 인자는 수량
                completions.add("<amount>");
            }
        }

        return completions;
    }
}