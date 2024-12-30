package org.lch.cCoinCraft.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CccCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // 첫 번째 인자: "btc", "eth", "doge", "usdt"
            return Arrays.asList("btc", "eth", "doge", "usdt");
        } else if (args.length == 2 && Arrays.asList("btc", "eth", "doge", "usdt").contains(args[0].toLowerCase())) {
            // 두 번째 인자: "buy", "sell", "balance"
            return Arrays.asList("buy", "sell", "balance");
        } else if (args.length == 3 && args[1].equalsIgnoreCase("buy")) {
            // 세 번째 인자: 구매 수량 (빈 리스트 반환)
            return new ArrayList<>();
        } else if (args.length == 3 && args[1].equalsIgnoreCase("sell")) {
            // 세 번째 인자: 판매 수량
            return new ArrayList<>();
        }

        return new ArrayList<>(); // 기본값: 빈 리스트
    }
}