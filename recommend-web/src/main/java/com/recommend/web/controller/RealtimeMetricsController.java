package com.recommend.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

@RestController
public class RealtimeMetricsController {

    @Autowired
    private JedisPool jedisPool;

    @GetMapping("/api/metrics/realtime")
    public Map<String, Object> getRealtime() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Jedis jedis = jedisPool.getResource()) {
            String ev = jedis.get("counter:events");
            long events = ev != null ? Long.parseLong(ev) : 0;
            long users = jedis.pfcount("counter:users");
            Map<String, String> bhv = jedis.hgetAll("counter:behavior");
            long pv = Long.parseLong(bhv.getOrDefault("pv", "0"));
            long buy = Long.parseLong(bhv.getOrDefault("buy", "0"));
            long cart = Long.parseLong(bhv.getOrDefault("cart", "0"));
            long fav = Long.parseLong(bhv.getOrDefault("fav", "0"));
            double rate = pv > 0 ? Math.round(buy * 1000.0 / pv) / 10.0 : 0;

            result.put("events_today", events);
            result.put("users_today", users);
            result.put("qps", 0);
            result.put("conversion_rate", rate);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }
}
