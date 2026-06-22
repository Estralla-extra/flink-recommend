package com.recommend.web.service;

import com.recommend.web.dao.AnalyticsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DataMigrationService {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private AnalyticsDao analyticsDao;

    @Scheduled(cron = "0 5 0 * * ?")
    public void migrateYesterdayData() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        String yesterday = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());
        System.out.println("[DataMigration] Starting migration for " + yesterday);

        migrateFunnel(yesterday);
        migrateHeatmap(yesterday);
        migrateCategoryHot(yesterday);
        migrateKpi(yesterday);
        migrateUserActivity(yesterday);

        System.out.println("[DataMigration] " + yesterday + " completed");
    }

    private void migrateFunnel(String date) {
        try (Jedis jedis = jedisPool.getResource()) {
            for (int h = 0; h < 24; h++) {
                String hourKey = String.format("%s%02d", date, h);
                Map<String, String> data = jedis.hgetAll("funnel:" + hourKey);
                if (data == null || data.isEmpty()) continue;
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    analyticsDao.upsertFunnel(hourKey, entry.getKey(), Long.parseLong(entry.getValue()));
                }
            }
        }
    }

    private void migrateHeatmap(String date) {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> data = jedis.hgetAll("heatmap:" + date);
            if (data == null || data.isEmpty()) return;
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String[] parts = entry.getKey().split("_", 2);
                if (parts.length == 2) {
                    analyticsDao.upsertHeatmap(date, parts[0], parts[1], Long.parseLong(entry.getValue()));
                }
            }
        }
    }

    private void migrateCategoryHot(String date) {
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> items = jedis.zrevrange("category:hot:" + date, 0, -1);
            if (items == null || items.isEmpty()) return;
            for (String catId : items) {
                Double score = jedis.zscore("category:hot:" + date, catId);
                if (score != null) {
                    analyticsDao.upsertCategoryHot(date, catId, score);
                }
            }
        }
    }

    private void migrateKpi(String date) {
        try (Jedis jedis = jedisPool.getResource()) {
            String evStr = jedis.get("counter:events:" + date);
            if (evStr == null) return;
            long events = Long.parseLong(evStr);
            long users = jedis.pfcount("counter:users:" + date);
            Map<String, String> bhv = jedis.hgetAll("counter:behavior:" + date);
            long buy = Long.parseLong(bhv.getOrDefault("buy", "0"));
            long pv = Long.parseLong(bhv.getOrDefault("pv", "0"));
            double rate = pv > 0 ? Math.round(buy * 1000.0 / pv) / 10.0 : 0;
            analyticsDao.upsertKpi(date, events, users, buy, rate);
        }
    }

    private void migrateUserActivity(String date) {
        try (Jedis jedis = jedisPool.getResource()) {
            for (int h = 0; h < 24; h++) {
                String hourKey = String.format("%s%02d", date, h);
                Map<String, String> raw = jedis.hgetAll("user:activity:" + hourKey);
                if (raw == null || raw.isEmpty()) continue;
                int light = 0, medium = 0, heavy = 0;
                for (String val : raw.values()) {
                    int count = Integer.parseInt(val);
                    if (count <= 5) light++;
                    else if (count <= 20) medium++;
                    else heavy++;
                }
                if (light > 0) analyticsDao.upsertUserActivity(hourKey, "light", light);
                if (medium > 0) analyticsDao.upsertUserActivity(hourKey, "medium", medium);
                if (heavy > 0) analyticsDao.upsertUserActivity(hourKey, "heavy", heavy);
            }
        }
    }
}