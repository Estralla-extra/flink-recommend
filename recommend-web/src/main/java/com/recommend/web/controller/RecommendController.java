package com.recommend.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class RecommendController {

    @Autowired
    private JedisPool jedisPool;

    // 兜底热门商品(与 Flink 中一致)
    private static final List<String> DEFAULT_HOT = Arrays.asList(
            "1535294", "3122135", "2331370", "3031354", "4091349"
    );

    /**
     * 获取用户推荐结果
     * @param userId       用户ID
     * @param currentItem  当前正在浏览的商品ID(可选)
     * @return 推荐列表 JSON
     */
    @GetMapping("/api/recommend")
    public Map<String, Object> recommend(
            @RequestParam("user_id") String userId,
            @RequestParam(value = "current_item", required = false) String currentItem) {

        Map<String, Double> candidateScores = new LinkedHashMap<>();
        Map<String, Double> flinkScores = new LinkedHashMap<>();
        Map<String, Double> realtimeScores = new LinkedHashMap<>();
        Map<String, Double> hotScores = new LinkedHashMap<>();
        boolean flinkHit = false;
        boolean realtimeHit = false;
        boolean hotUsed = false;

        try (Jedis jedis = jedisPool.getResource()) {
            String cacheKey = "recommend:user:" + userId;
            String cachedValue = jedis.get(cacheKey);
            if (cachedValue != null && !cachedValue.isEmpty()) {
                flinkHit = true;
                String[] items = cachedValue.split(",");
                for (String item : items) {
                    candidateScores.merge(item, 0.5, Double::sum);
                    flinkScores.merge(item, 0.5, Double::sum);
                }
            }

            if (currentItem != null && !currentItem.isEmpty()) {
                String simKey = "sim:" + currentItem;
                String simValue = jedis.get(simKey);
                if (simValue != null && !simValue.isEmpty()) {
                    realtimeHit = true;
                    String[] simItems = simValue.split(",");
                    for (String sim : simItems) {
                        String[] parts = sim.split(":");
                        if (parts.length == 2) {
                            String candidate = parts[0];
                            double score = Double.parseDouble(parts[1]);
                            candidateScores.merge(candidate, score * 1.0, Double::sum);
                            realtimeScores.merge(candidate, score * 1.0, Double::sum);
                        }
                    }
                }
            }

            if (candidateScores.isEmpty()) {
                hotUsed = true;
                for (String hotItem : DEFAULT_HOT) {
                    candidateScores.put(hotItem, 0.1);
                    hotScores.put(hotItem, 0.1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            hotUsed = true;
            for (String hotItem : DEFAULT_HOT) {
                candidateScores.put(hotItem, 0.1);
            }
        }

        List<Map<String, Object>> top5 = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("itemId", entry.getKey());
                    item.put("score", Math.round(entry.getValue() * 100.0) / 100.0);
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> sources = new HashMap<>();
        sources.put("flink", flinkHit);
        sources.put("realtime", realtimeHit);
        sources.put("hot", hotUsed);
        sources.put("current_item", currentItem != null ? currentItem : "");

        Map<String, Object> result = new HashMap<>();
        result.put("user_id", userId);
        result.put("recommendations", top5);
        result.put("sources", sources);
        Map<String, Object> sourceLists = new LinkedHashMap<>();
        sourceLists.put("flink", toItemList(flinkScores));
        sourceLists.put("realtime", toItemList(realtimeScores));
        sourceLists.put("hot", toItemList(hotScores));
        result.put("source_lists", sourceLists);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    private List<Map<String, Object>> toItemList(Map<String, Double> scores) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("itemId", e.getKey());
                    item.put("score", Math.round(e.getValue() * 100.0) / 100.0);
                    return item;
                })
                .collect(Collectors.toList());
    }
    @GetMapping("/api/events/recent")
    public List<String> recentEvents() {
        List<String> events = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> rawEvents = jedis.lrange("recent:events", 0, 19);
            for (String raw : rawEvents) {
                String[] fields = raw.split(",");
                if (fields.length == 5) {
                    String user = fields[0];
                    String item = fields[1];
                    String behavior = fields[3];
                    String behaviorCn = behavior.equals("buy") ? "购买" :
                            behavior.equals("cart") ? "加购" : "收藏";
                    events.add(String.format("用户%s " + behaviorCn + "了商品%s", user, item));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return events.isEmpty() ? Collections.singletonList("暂无行为数据") : events;
    }
}
