package org.lch.cCoinCraft.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.lch.cCoinCraft.CCoinCraft;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CoinGecko API를 통해 실시간 암호화폐 가격을 가져오고, 주기적으로 업데이트하는 클래스
 */
public class CoinGeckoPriceFetcher {

    private final CCoinCraft plugin;
    private final OkHttpClient httpClient;
    private final ConcurrentHashMap<String, Double> coinPrices; // 실시간 가격 저장

    public CoinGeckoPriceFetcher(CCoinCraft plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient();
        this.coinPrices = new ConcurrentHashMap<>();
    }

    /**
     * 가격 업데이트 스케줄 시작
     */
    public void startPriceUpdates() {
        // 초기 가격 가져오기
        fetchPrices();

        // 1분마다 가격 업데이트
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::fetchPrices, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * CoinGecko API를 통해 가격을 가져오는 메서드
     */
    private void fetchPrices() {
        // 관심 있는 코인 IDs (CoinGecko의 고유 ID)
        String ids = "bitcoin,ethereum,dogecoin,tether"; // 필요에 따라 추가

        // 대상 화폐 (KRW)
        String vsCurrencies = "krw";

        String url = String.format("https://api.coingecko.com/api/v3/simple/price?ids=%s&vs_currencies=%s", ids, vsCurrencies);

        Request request = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                plugin.getLogger().severe("CoinGecko API 요청 실패: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    plugin.getLogger().severe("CoinGecko API 응답 실패: " + response.code());
                    return;
                }

                String responseBody = response.body().string();
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                // 각 코인의 가격 업데이트
                for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
                    String coinId = entry.getKey().toUpperCase(); // 예: BITCOIN -> BTC
                    JsonObject priceObject = entry.getValue().getAsJsonObject();
                    double price = priceObject.get("krw").getAsDouble();

                    // CoinGecko ID를 심볼로 변환 (간단히 매핑)
                    String symbol = mapCoinIdToSymbol(coinId);
                    if (symbol != null) {
                        coinPrices.put(symbol, price);
                        plugin.getLogger().info("Updated " + symbol + " price to " + price + " KRW");
                    }
                }
            }
        });
    }

    /**
     * CoinGecko의 코인 ID를 심볼로 매핑하는 메서드
     * 필요에 따라 더 많은 코인을 추가할 수 있습니다.
     */
    private String mapCoinIdToSymbol(String coinId) {
        switch (coinId) {
            case "BITCOIN":
                return "BTC";
            case "ETHEREUM":
                return "ETH";
            case "DOGECOIN":
                return "DOGE";
            case "TETHER":
                return "USDT";
            // 필요 시 추가
            default:
                return null;
        }
    }

    /**
     * 특정 코인의 현재 가격을 반환
     *
     * @param symbol 코인 심볼 (예: BTC)
     * @return 현재 가격 (KRW), 없으면 null
     */
    public Double getPrice(String symbol) {
        return coinPrices.get(symbol.toUpperCase());
    }
}