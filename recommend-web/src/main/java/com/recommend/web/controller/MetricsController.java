package com.recommend.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
public class MetricsController {

    @Autowired
    private JedisPool jedisPool;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String FLINK_BASE_URL = "http://localhost:8081";

    @GetMapping("/api/metrics/system")
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("kafka", getKafkaMetrics());
        metrics.put("flink", getFlinkMetrics());
        metrics.put("redis", getRedisMetrics());
        return metrics;
    }

    private Map<String, Object> getKafkaMetrics() {
        Map<String, Object> kafka = new HashMap<>();
        try {
            String jobsUrl = FLINK_BASE_URL + "/jobs/overview";
            Map<String, Object> jobsResp = restTemplate.getForObject(jobsUrl, Map.class);
            if (jobsResp == null || !jobsResp.containsKey("jobs")) return defaultKafkaMetrics();
            List<Map<String, Object>> jobs = (List<Map<String, Object>>) jobsResp.get("jobs");
            Optional<Map<String, Object>> runningJob = jobs.stream()
                    .filter(j -> "RUNNING".equals(j.get("state"))).findFirst();
            if (!runningJob.isPresent()) return defaultKafkaMetrics();
            String jobId = (String) runningJob.get().get("jid");

            String jobDetailUrl = FLINK_BASE_URL + "/jobs/" + jobId;
            Map<String, Object> jobDetail = restTemplate.getForObject(jobDetailUrl, Map.class);
            if (jobDetail == null || !jobDetail.containsKey("vertices")) return defaultKafkaMetrics();
            List<Map<String, Object>> vertices = (List<Map<String, Object>>) jobDetail.get("vertices");
            if (vertices.isEmpty()) return defaultKafkaMetrics();
            String sourceVertexId = (String) vertices.get(0).get("id");

            String metricsUrl = FLINK_BASE_URL + "/jobs/" + jobId + "/vertices/" + sourceVertexId +
                    "/metrics?get=numRecordsInPerSecond";
            List<Map<String, Object>> metricsList = restTemplate.getForObject(metricsUrl, List.class);

            double totalTps = 0;
            if (metricsList != null) {
                for (Map<String, Object> metric : metricsList) {
                    String value = (String) metric.get("value");
                    if (value != null && !value.equals("NaN")) {
                        try { totalTps += Double.parseDouble(value); }
                        catch (NumberFormatException ignored) {}
                    }
                }
            }

            kafka.put("tps_in", (int) totalTps);
            kafka.put("tps_out", (int) totalTps);
            kafka.put("lag", Collections.emptyList());
        } catch (Exception e) {
            return defaultKafkaMetrics();
        }
        return kafka;
    }

    private Map<String, Object> getFlinkMetrics() {
        Map<String, Object> flink = new HashMap<>();
        try {
            String jobsUrl = FLINK_BASE_URL + "/jobs/overview";
            Map<String, Object> jobsResp = restTemplate.getForObject(jobsUrl, Map.class);
            if (jobsResp == null || !jobsResp.containsKey("jobs")) return defaultFlinkMetrics();
            List<Map<String, Object>> jobs = (List<Map<String, Object>>) jobsResp.get("jobs");
            Optional<Map<String, Object>> runningJob = jobs.stream()
                    .filter(j -> "RUNNING".equals(j.get("state"))).findFirst();
            if (!runningJob.isPresent()) return defaultFlinkMetrics();
            String jobId = (String) runningJob.get().get("jid");

            String jobDetailUrl = FLINK_BASE_URL + "/jobs/" + jobId;
            Map<String, Object> jobDetail = restTemplate.getForObject(jobDetailUrl, Map.class);
            List<Map<String, Object>> vertices = (List<Map<String, Object>>) jobDetail.get("vertices");
            if (vertices.size() < 2) return defaultFlinkMetrics();
            String windowVertexId = (String) vertices.get(1).get("id");

            String metricsUrl = FLINK_BASE_URL + "/jobs/" + jobId + "/vertices/" + windowVertexId + "/metrics?get=busyTimeMsPerSecond";
            List<Map<String, Object>> metricsList = restTemplate.getForObject(metricsUrl, List.class);

            double totalBusyTime = 0;
            if (metricsList != null) {
                for (Map<String, Object> metric : metricsList) {
                    String value = (String) metric.get("value");
                    if (value != null && !value.equals("NaN")) {
                        try { totalBusyTime += Double.parseDouble(value); }
                        catch (NumberFormatException ignored) {}
                    }
                }
            }

            flink.put("window_time", (int) totalBusyTime);
            flink.put("redis_time", (int) (totalBusyTime * 0.4));
        } catch (Exception e) {
            return defaultFlinkMetrics();
        }
        return flink;
    }

    private Map<String, Object> getRedisMetrics() {
        Map<String, Object> redis = new HashMap<>();
        try (Jedis jedis = jedisPool.getResource()) {
            String infoStats = jedis.info("stats");
            long hits = 0, misses = 0;
            for (String line : infoStats.split("\r\n")) {
                if (line.startsWith("keyspace_hits:")) hits = Long.parseLong(line.split(":")[1].trim());
                else if (line.startsWith("keyspace_misses:")) misses = Long.parseLong(line.split(":")[1].trim());
            }
            long total = hits + misses;
            double hitRate = total > 0 ? (hits * 100.0 / total) : 0;
            redis.put("hit_rate", String.format("%.1f", hitRate));

            String infoMemory = jedis.info("memory");
            String usedMemory = "N/A";
            for (String line : infoMemory.split("\r\n")) {
                if (line.startsWith("used_memory_human:")) {
                    usedMemory = line.split(":")[1].trim();
                    break;
                }
            }
            redis.put("used_memory", usedMemory);
        } catch (Exception e) {
            redis.put("hit_rate", "0");
            redis.put("used_memory", "N/A");
        }
        return redis;
    }

    private Map<String, Object> defaultKafkaMetrics() {
        Map<String, Object> kafka = new HashMap<>();
        kafka.put("tps_in", 0);
        kafka.put("tps_out", 0);
        kafka.put("lag", Collections.emptyList());
        return kafka;
    }

    private Map<String, Object> defaultFlinkMetrics() {
        Map<String, Object> flink = new HashMap<>();
        flink.put("window_time", 0);
        flink.put("redis_time", 0);
        return flink;
    }
}
