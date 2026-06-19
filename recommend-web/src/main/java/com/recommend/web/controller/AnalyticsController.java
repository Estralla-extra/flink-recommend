package com.recommend.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private JedisPool jedisPool;

    @GetMapping("/funnel")
    public Map<String, Object> funnel(@RequestParam("hour") String hour) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> raw = jedis.hgetAll("funnel:" + hour);
            if (raw == null || raw.isEmpty()) {
                result.put("hour", hour);
                result.put("steps", Collections.emptyList());
                result.put("message", "no data for this hour");
                return result;
            }

            List<String> order = Arrays.asList("pv", "cart", "fav", "buy");
            List<Map<String, Object>> steps = new ArrayList<>();
                        for (String behavior : order) {
                String val = raw.get(behavior);
                if (val == null) continue;
                long count = Long.parseLong(val);
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("behavior", behavior);
                step.put("count", count);
                steps.add(step);
            }
            result.put("hour", hour);
            result.put("steps", steps);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    @GetMapping("/heatmap")
    public Map<String, Object> heatmap(@RequestParam("date") String date) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> raw = jedis.hgetAll("heatmap:" + date);
            if (raw == null || raw.isEmpty()) {
                result.put("date", date);
                result.put("data", Collections.emptyList());
                return result;
            }

            List<Map<String, Object>> hourlyData = new ArrayList<>();
            for (int h = 0; h < 24; h++) {
                String hh = String.format("%02d", h);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("hour", hh);
                long pv = 0, cart = 0, fav = 0, buy = 0;
                if (raw.containsKey(hh + "_pv")) pv = Long.parseLong(raw.get(hh + "_pv"));
                if (raw.containsKey(hh + "_cart")) cart = Long.parseLong(raw.get(hh + "_cart"));
                if (raw.containsKey(hh + "_fav")) fav = Long.parseLong(raw.get(hh + "_fav"));
                if (raw.containsKey(hh + "_buy")) buy = Long.parseLong(raw.get(hh + "_buy"));
                entry.put("pv", pv);
                entry.put("cart", cart);
                entry.put("fav", fav);
                entry.put("buy", buy);
                entry.put("total", pv + cart + fav + buy);
                hourlyData.add(entry);
            }
            result.put("date", date);
            result.put("data", hourlyData);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    @GetMapping("/category/top")
    public Map<String, Object> categoryTop(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> raw = jedis.zrevrange("category:hot", 0, limit - 1);
            List<Map<String, Object>> items = new ArrayList<>();
            for (String catId : raw) {
                Double score = jedis.zscore("category:hot", catId);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("categoryId", catId);
                item.put("count", score != null ? score.intValue() : 0);
                items.add(item);
            }
            result.put("items", items);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    @GetMapping("/user/activity")
    public Map<String, Object> userActivity(@RequestParam("hour") String hour) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> raw = jedis.hgetAll("user:activity:" + hour);
            if (raw == null || raw.isEmpty()) {
                result.put("hour", hour);
                result.put("buckets", Collections.emptyList());
                return result;
            }
            int light = 0, medium = 0, heavy = 0;
            for (String val : raw.values()) {
                int count = Integer.parseInt(val);
                if (count <= 5) light++;
                else if (count <= 20) medium++;
                else heavy++;
            }
            List<Map<String, Object>> buckets = new ArrayList<>();
            Map<String, Object> b1 = new LinkedHashMap<>();
            b1.put("label", "light (<5)"); b1.put("count", light); buckets.add(b1);
            Map<String, Object> b2 = new LinkedHashMap<>();
            b2.put("label", "medium (5-20)"); b2.put("count", medium); buckets.add(b2);
            Map<String, Object> b3 = new LinkedHashMap<>();
            b3.put("label", "heavy (>20)"); b3.put("count", heavy); buckets.add(b3);
            result.put("hour", hour);
            result.put("buckets", buckets);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }
}