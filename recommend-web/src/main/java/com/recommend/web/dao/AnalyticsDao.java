package com.recommend.web.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void upsertFunnel(String dateHour, String behavior, long count) {
        jdbcTemplate.update(
            "INSERT INTO analytics_funnel (date_hour, behavior, count) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE count = VALUES(count)",
            dateHour, behavior, count);
    }

    public void upsertHeatmap(String dateKey, String hour, String behavior, long count) {
        jdbcTemplate.update(
            "INSERT INTO analytics_heatmap (date_key, hour_key, behavior, count) VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE count = VALUES(count)",
            dateKey, hour, behavior, count);
    }

    public void upsertCategoryHot(String dateKey, String categoryId, double score) {
        jdbcTemplate.update(
            "INSERT INTO analytics_category_hot (date_key, category_id, score) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE score = VALUES(score)",
            dateKey, categoryId, score);
    }

    public void upsertKpi(String dateKey, long events, long users, long buyCount, double rate) {
        jdbcTemplate.update(
            "INSERT INTO analytics_kpi (date_key, events_today, users_today, buy_count, conversion_rate) " +
            "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
            "events_today = VALUES(events_today), users_today = VALUES(users_today), " +
            "buy_count = VALUES(buy_count), conversion_rate = VALUES(conversion_rate)",
            dateKey, events, users, buyCount, rate);
    }

    public void upsertUserActivity(String dateHour, String bucket, long count) {
        jdbcTemplate.update(
            "INSERT INTO analytics_user_activity (date_hour, bucket, count) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE count = VALUES(count)",
            dateHour, bucket, count);
    }
    // ===== Query methods for historical data =====

    public List<Map<String, Object>> queryFunnel(String dateHour) {
        return jdbcTemplate.queryForList(
            "SELECT behavior, count FROM analytics_funnel WHERE date_hour = ?", dateHour);
    }

    public List<Map<String, Object>> queryHeatmap(String dateKey) {
        return jdbcTemplate.queryForList(
            "SELECT hour_key, behavior, count FROM analytics_heatmap WHERE date_key = ? ORDER BY hour_key", dateKey);
    }

    public List<Map<String, Object>> queryCategoryHot(String dateKey, int limit) {
        return jdbcTemplate.queryForList(
            "SELECT category_id, score FROM analytics_category_hot WHERE date_key = ? ORDER BY score DESC LIMIT ?",
            dateKey, limit);
    }

    public List<Map<String, Object>> queryUserActivity(String dateHour) {
        return jdbcTemplate.queryForList(
            "SELECT bucket, count FROM analytics_user_activity WHERE date_hour = ?", dateHour);
    }
}