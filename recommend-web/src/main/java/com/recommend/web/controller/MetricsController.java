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
            if (jobsResp == null || !jobsResp.containsKey("jobs")) return defaultKafka();
            List<Map<String, Object>> jobs = (List<Map<String, Object>>) jobsResp.get("jobs");
            Optional<Map<String, Object>> runningJob = jobs.stream()
                    .filter(j -> "RUNNING".equals(j.get("state"))).findFirst();
            if (!runningJob.isPresent()) return defaultKafka();
            String jobId = (String) runningJob.get().get("jid");
            String url = FLINK_BASE_URL + "/jobs/" + jobId;
            Map<String, Object> detail = restTemplate.getForObject(url, Map.class);
            if (detail == null || !detail.containsKey("vertices")) return defaultKafka();
            List<Map<String, Object>> vertices = (List<Map<String, Object>>) detail.get("vertices");
            if (vertices.isEmpty()) return defaultKafka();

            // Get ALL source vertex metrics, then filter by name
            String vid = (String) vertices.get(0).get("id");
            String baseUrl = FLINK_BASE_URL + "/jobs/" + jobId + "/vertices/" + vid + "/metrics";

            double tps = 0;

            // Step 1: get ALL metric IDs (no values)
            List<Map<String, Object>> idList = restTemplate.getForObject(baseUrl, List.class);

            // Step 2: filter for KafkaConsumer metric IDs
            List<String> targetIds = new ArrayList<>();
            if (idList != null) {
                for (Map<String, Object> m : idList) {
                    String id = (String) m.get("id");
                    if (id != null && id.contains("KafkaConsumer.records-consumed-rate")) {
                        targetIds.add(id);
                    }
                }
            }

            // Step 3: query with exact IDs to get values
            if (!targetIds.isEmpty()) {
                StringBuilder sb = new StringBuilder(baseUrl).append("?get=");
                for (int i = 0; i < targetIds.size(); i++) {
                    if (i > 0) sb.append("&get=");
                    sb.append(targetIds.get(i));
                }
                List<Map<String, Object>> valList = restTemplate.getForObject(sb.toString(), List.class);
                if (valList != null) {
                    for (Map<String, Object> m : valList) {
                        String val = (String) m.get("value");
                        if (val != null && !val.equals("NaN")) {
                            try { tps += Double.parseDouble(val); } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }

            kafka.put("tps_in", (int) tps);
            kafka.put("tps_out", (int) tps);
            kafka.put("lag", Collections.emptyList());
        } catch (Exception e) {
            return defaultKafka();
        }
        return kafka;
    }

    private Map<String, Object> getFlinkMetrics() {
        Map<String, Object> flink = new HashMap<>();
        try {
            String url = FLINK_BASE_URL + "/jobs/overview";
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null || !resp.containsKey("jobs")) return defaultFlink();
            List<Map<String, Object>> jobs = (List<Map<String, Object>>) resp.get("jobs");
            Optional<Map<String, Object>> running = jobs.stream()
                    .filter(j -> "RUNNING".equals(j.get("state"))).findFirst();
            if (!running.isPresent()) return defaultFlink();
            String jobId = (String) running.get().get("jid");
            flink.put("m1", (int) getMetricByVertex(jobId, 0, "KafkaConsumer.fetch-rate", false));
            flink.put("m2", (int) getMetricByVertex(jobId, 0, "Flat_Map.numRecordsInPerSecond", false));
            flink.put("m3", (int) getMetricByVertex(jobId, 0, "records-per-request-avg", false));
            flink.put("m4", (int) getMetricByVertex(jobId, 0, "fetch-size-avg", false));
            flink.put("m5", (int) getMetricByVertex(jobId, 1, "window-category-hot.numRecordsInPerSecond", false));
            Object dur = running.get().get("duration");
            int secs = (int) (dur instanceof Number ? ((Number) dur).longValue() / 1000 : 0);
            flink.put("window_time", Math.max(secs, 1));
        } catch (Exception e) {
            return defaultFlink();
        }
        return flink;
    }

    private double getMetricByVertex(String jobId, int vertexIdx, String pattern) {
        return getMetricByVertex(jobId, vertexIdx, pattern, false);
    }

    private double getMetricByVertex(String jobId, int vertexIdx, String pattern, boolean aggregateOnly) {
        String detailUrl = FLINK_BASE_URL + "/jobs/" + jobId;
        Map<String, Object> detail = restTemplate.getForObject(detailUrl, Map.class);
        if (detail == null || !detail.containsKey("vertices")) return 0;
        List<Map<String, Object>> vertices = (List<Map<String, Object>>) detail.get("vertices");
        if (vertices.size() <= vertexIdx) return 0;
        String vid = (String) vertices.get(vertexIdx).get("id");
        String baseUrl = FLINK_BASE_URL + "/jobs/" + jobId + "/vertices/" + vid + "/metrics";
        List<Map<String, Object>> idList = restTemplate.getForObject(baseUrl, List.class);
        List<String> targets = new ArrayList<>();
        if (idList != null) {
            for (Map<String, Object> m : idList) {
                String id = (String) m.get("id");
                if (id != null && id.contains(pattern)) {
                    if (aggregateOnly) {
                        String after = id.substring(id.indexOf('.') + 1);
                        if (after.contains(".")) continue;
                    }
                    targets.add(id);
                }
            }
        }
        double value = 0;
        if (!targets.isEmpty()) {
            StringBuilder sb = new StringBuilder(baseUrl).append("?get=");
            for (int i = 0; i < targets.size(); i++) {
                if (i > 0) sb.append("&get=");
                sb.append(targets.get(i));
            }
            List<Map<String, Object>> valList = restTemplate.getForObject(sb.toString(), List.class);
            if (valList != null) {
                for (Map<String, Object> m : valList) {
                    String val = (String) m.get("value");
                    if (val != null && !val.equals("NaN")) {
                        try { value += Double.parseDouble(val); } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return value;
    }

    private Map<String, Object> getRedisMetrics() {
        Map<String, Object> redis = new HashMap<>();
        try (Jedis jedis = jedisPool.getResource()) {
            String s = jedis.info("stats");
            long hits = 0, misses = 0;
            for (String line : s.split("\r\n")) {
                if (line.startsWith("keyspace_hits:")) hits = Long.parseLong(line.split(":")[1].trim());
                else if (line.startsWith("keyspace_misses:")) misses = Long.parseLong(line.split(":")[1].trim());
            }
            long total = hits + misses;
            redis.put("hit_rate", String.format("%.1f", total > 0 ? hits * 100.0 / total : 0));
            String mem = jedis.info("memory");
            String val = "N/A";
            for (String line : mem.split("\r\n")) {
                if (line.startsWith("used_memory_human:")) { val = line.split(":")[1].trim(); break; }
            }
            redis.put("used_memory", val);
        } catch (Exception e) {
            redis.put("hit_rate", "0");
            redis.put("used_memory", "N/A");
        }
        return redis;
    }

    private Map<String, Object> defaultKafka() {
        Map<String, Object> k = new HashMap<>();
        k.put("tps_in", 0); k.put("tps_out", 0); k.put("lag", Collections.emptyList());
        return k;
    }

    private Map<String, Object> defaultFlink() {
        Map<String, Object> f = new HashMap<>();
        f.put("window_time", 0); f.put("redis_time", 0);
        return f;
    }
}
