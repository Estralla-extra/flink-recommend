package com.recommend.flink;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.RichWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RealtimeRecommendJob {

    private static final String REDIS_HOST = "192.168.249.102";
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_PASSWORD = "myredis123";
    private static final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(),
            REDIS_HOST, REDIS_PORT, 2000, REDIS_PASSWORD);

    private static final List<String> DEFAULT_HOT_ITEMS = Arrays.asList(
            "1535294", "3122135", "2331370", "3031354", "4091349"
    );

        public static void main(String[] args) throws Exception {
            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "hadoop102:9092,hadoop103:9092");
        props.setProperty("group.id", "flink-recommend-group");
        props.setProperty("auto.offset.reset", "latest");

        FlinkKafkaConsumer<String> kafkaSource = new FlinkKafkaConsumer<>(
                "user_behavior", new SimpleStringSchema(), props);

        DataStream<UserAction> actionStream = env.addSource(kafkaSource).flatMap(new BehaviorParser());

        DataStream<String> recommendStream = actionStream
                .keyBy(action -> action.userId)
                .window(TumblingProcessingTimeWindows.of(Time.minutes(3)))
                .aggregate(new BehaviorAggregator(), new RecommendWindowFunction())
                .name("window-recommend");
        recommendStream.print();

        actionStream.filter(action -> action.categoryId != null && !action.categoryId.isEmpty())
                .keyBy(action -> action.categoryId)
                .window(TumblingProcessingTimeWindows.of(Time.minutes(15)))
                .apply(new CategoryHotWindowFunction())
                .name("window-category-hot");

        actionStream.map(action -> new HourlyBucket(action))
                .keyBy(b -> b.userId + "|" + b.hourKey)
                .window(TumblingProcessingTimeWindows.of(Time.minutes(15)))
                .apply(new HourlyStatsWindowFunction())
                .name("window-hourly-stats");

        env.execute("Real-time Recommendation Job with Multi-window");
    }

    public static String toHourKey(long tsMs) {
        LocalDateTime dt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(tsMs), ZoneId.systemDefault());
        return dt.format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
    }

    public static String toDateKey(long tsMs) {
        LocalDateTime dt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(tsMs), ZoneId.systemDefault());
        return dt.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    public static String toHourOfDay(long tsMs) {
        LocalDateTime dt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(tsMs), ZoneId.systemDefault());
        return dt.format(DateTimeFormatter.ofPattern("HH"));
    }

    public static class UserAction {
        public String userId; public String itemId; public String categoryId; public String behavior; public long timestamp;
        public UserAction() {}
        public UserAction(String userId, String itemId, String categoryId, String behavior, long timestamp) {
            this.userId = userId; this.itemId = itemId; this.categoryId = categoryId; this.behavior = behavior; this.timestamp = timestamp;
        }
    }

    public static class HourlyBucket {
        public String userId; public String hourKey; public String dateKey; public String hourOfDay; public String behavior; public long timestamp;
        public HourlyBucket() {}
        public HourlyBucket(UserAction a) {
            this.userId = a.userId; this.behavior = a.behavior; this.timestamp = a.timestamp;
            long now = System.currentTimeMillis();
            this.hourKey = toHourKey(now);
            this.dateKey = toDateKey(now);
            this.hourOfDay = toHourOfDay(now);
        }
    }

    public static class BehaviorParser implements FlatMapFunction<String, UserAction> {
        @Override
        public void flatMap(String line, Collector<UserAction> out) {
            if (!line.startsWith("heartbeat")) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.lpush("recent:events", line);
                    jedis.ltrim("recent:events", 0, 49);
                    String[] parts = line.split(",");
                    if (parts.length >= 5) {
                        String dt = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
                        jedis.incr("counter:events:" + dt);
                    jedis.expire("counter:events:" + dt, 604800);
                        jedis.hincrBy("counter:behavior:" + dt, parts[3], 1);
                    jedis.expire("counter:behavior:" + dt, 604800);
                        jedis.pfadd("counter:users:" + dt, parts[0]);
                    jedis.expire("counter:users:" + dt, 604800);
                    }
                } catch (Exception e) {
                    System.err.println("Redis write error (ignored): " + e.getMessage());
                }
            }
            try {
                String[] fields = line.split(",");
                if (fields.length >= 5) {
                    String userId = fields[0]; String itemId = fields[1]; String categoryId = fields[2];
                    String behavior = fields[3]; long tsSeconds = Long.parseLong(fields[4]);
                    if ("pv".equals(behavior) || "buy".equals(behavior) || "cart".equals(behavior) || "fav".equals(behavior)) {
                        out.collect(new UserAction(userId, itemId, categoryId, behavior, tsSeconds * 1000L));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    public static class BehaviorAggregator implements AggregateFunction<UserAction, List<UserAction>, List<UserAction>> {
        @Override public List<UserAction> createAccumulator() { return new ArrayList<>(); }
        @Override public List<UserAction> add(UserAction a, List<UserAction> acc) { acc.add(a); return acc; }
        @Override public List<UserAction> getResult(List<UserAction> acc) { return acc; }
        @Override public List<UserAction> merge(List<UserAction> a, List<UserAction> b) { a.addAll(b); return a; }
    }

    public static class RecommendWindowFunction extends RichWindowFunction<List<UserAction>, String, String, TimeWindow> {
        private transient JedisPool pool;
        @Override public void open(Configuration parameters) { pool = jedisPool; }

        @Override
        public void apply(String userId, TimeWindow window, Iterable<List<UserAction>> iterable, Collector<String> out) {
            List<UserAction> actions = new ArrayList<>(); iterable.forEach(actions::addAll);
            Map<String, Double> candidateScores = new HashMap<>();
            try (Jedis jedis = pool.getResource()) {
                for (UserAction action : actions) {
                    String simValue = jedis.get("sim:" + action.itemId);
                    if (simValue == null || simValue.isEmpty()) continue;
                    String[] simItems = simValue.split(",");
                    for (String sim : simItems) {
                        String[] p = sim.split(":");
                        if (p.length == 2) {
                            double w = "buy".equals(action.behavior) ? 1.0 : "cart".equals(action.behavior) ? 0.9 : 0.5;
                            candidateScores.merge(p[0], Double.parseDouble(p[1]) * w, Double::sum);
                        }
                    }
                }
            }
            List<String> topItems = candidateScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(5).map(Map.Entry::getKey).collect(Collectors.toList());
            if (topItems.size() < 5) {
                for (String hotItem : DEFAULT_HOT_ITEMS) {
                    if (!topItems.contains(hotItem)) { topItems.add(hotItem); if (topItems.size() == 5) break; }
                }
            }
            try (Jedis jedis = pool.getResource()) {
                String cacheKey = "recommend:user:" + userId;
                jedis.setex(cacheKey, 900, String.join(",", topItems));
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                System.out.println("Cached: " + cacheKey + " -> " + String.join(",", topItems) + " at " + sdf.format(new java.util.Date()));
            } catch (Exception e) {}
        }
    }

    public static class CategoryHotWindowFunction extends RichWindowFunction<UserAction, String, String, TimeWindow> {
        private transient JedisPool pool;
        @Override public void open(Configuration parameters) { pool = jedisPool; }

        @Override
        public void apply(String categoryId, TimeWindow window, Iterable<UserAction> input, Collector<String> out) {
            int count = 0; for (UserAction a : input) { count++; }
            String dateKey = toDateKey(window.getEnd());
            try (Jedis jedis = pool.getResource()) {
                jedis.zincrby("category:hot:" + dateKey, count, categoryId);
                jedis.expire("category:hot:" + dateKey, 604800);
                jedis.zremrangeByRank("category:hot:" + dateKey, 0, -101);
            } catch (Exception e) {
                System.err.println("CategoryHot Redis error: " + e.getMessage());
            }
        }
    }

    public static class HourlyStatsWindowFunction extends RichWindowFunction<HourlyBucket, String, String, TimeWindow> {
        private transient JedisPool pool;
        @Override public void open(Configuration parameters) { pool = jedisPool; }

        @Override
        public void apply(String key, TimeWindow window, Iterable<HourlyBucket> input, Collector<String> out) {
            List<HourlyBucket> buckets = new ArrayList<>(); input.forEach(buckets::add); if (buckets.isEmpty()) return;
            HourlyBucket first = buckets.get(0);
            Map<String, Integer> behaviorCount = new HashMap<>();
            for (HourlyBucket b : buckets) { if (!b.userId.startsWith("heartbeat")) behaviorCount.merge(b.behavior, 1, Integer::sum); }
            try (Jedis jedis = pool.getResource()) {
                for (Map.Entry<String, Integer> e : behaviorCount.entrySet()) {
                    jedis.hincrBy("funnel:" + first.hourKey, e.getKey(), e.getValue());
                    jedis.expire("funnel:" + first.hourKey, 259200);
                }
                for (Map.Entry<String, Integer> e : behaviorCount.entrySet()) {
                    jedis.hincrBy("heatmap:" + first.dateKey, first.hourOfDay + "_" + e.getKey(), e.getValue());
                jedis.expire("heatmap:" + first.dateKey, 604800);
                }
                String actKey = "user:activity:" + first.hourKey;
                String oldVal = jedis.hget(actKey, first.userId);
                int prev = (oldVal != null && !oldVal.isEmpty()) ? Integer.parseInt(oldVal) : 0;
                int totalEvents = behaviorCount.values().stream().mapToInt(Integer::intValue).sum();
                jedis.hset(actKey, first.userId, String.valueOf(prev + totalEvents));
                jedis.expire(actKey, 259200);
            }
        }
    }
}
