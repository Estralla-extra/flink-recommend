"""
Redis 同步脚本 — 从 parquet 结果写入 Redis

用法 (集群):
  python sync_redis.py \
    --results hdfs:///item2vec/output/sim_results.parquet

用法 (本地测试):
  python sync_redis.py \
    --results /tmp/item2vec/output/sim_results.parquet --local

输出:
  Redis key sim:{item_id} → "top1_id:score,top2_id:score,..."
  带 TTL (默认 7 天)
"""
import redis
import pandas as pd
import time

# ==================== 配置 ====================
HDFS_RESULTS = "hdfs:///item2vec/output/sim_results.parquet"
REDIS_HOST = "192.168.249.102"
REDIS_PORT = 6379
REDIS_PASSWORD = "myredis123"
REDIS_TTL = 7 * 24 * 3600     # 7 天
MSET_BATCH = 5000              # 每批写入条数


def main():
    import argparse
    parser = argparse.ArgumentParser(description="Redis 同步")
    parser.add_argument("--results", default=HDFS_RESULTS)
    parser.add_argument("--redis-host", default=REDIS_HOST)
    parser.add_argument("--redis-port", type=int, default=REDIS_PORT)
    parser.add_argument("--redis-pass", default=REDIS_PASSWORD)
    parser.add_argument("--ttl", type=int, default=REDIS_TTL,
                        help="Redis key 过期时间 (秒)")
    parser.add_argument("--local", action="store_true",
                        help="从本地文件读取")
    args = parser.parse_args()

    t0 = time.time()

    # ========== 1. 读取结果 ==========
    print("=" * 50)
    print("Stage 1: 读取相似度结果...")
    print("=" * 50)

    if args.local:
        df = pd.read_parquet(args.results)
    else:
        from pyspark.sql import SparkSession
        spark = SparkSession.builder.appName("Redis_Sync").getOrCreate()
        df = spark.read.parquet(args.results).toPandas()
        spark.stop()

    n = len(df)
    print(f"  读取完成: {n:,} 条记录")

    # ========== 2. 连接 Redis ==========
    print(f"\nStage 2: 连接 Redis ({args.redis_host}:{args.redis_port})...")
    r = redis.Redis(
        host=args.redis_host,
        port=args.redis_port,
        password=args.redis_pass,
        decode_responses=True,
        socket_connect_timeout=5,
    )
    r.ping()
    print(f"  Redis 连接成功, 当前 DB 大小: {r.dbsize():,}")

    # ========== 3. 批量写入 ==========
    print(f"\nStage 3: 批量写入 Redis (每批 {MSET_BATCH:,} 条)...")
    pipe = r.pipeline(transaction=False)
    count = 0

    for _, row in df.iterrows():
        key = f"sim:{row['item_id']}"
        pipe.setex(key, args.ttl, row['similars'])
        count += 1

        if count % MSET_BATCH == 0:
            pipe.execute()
            elapsed = time.time() - t0
            pct = 100 * count / n
            eta = elapsed / count * (n - count)
            print(f"  进度: {count:,}/{n:,} ({pct:.1f}%) | "
                  f"已耗时 {elapsed:.1f}s | 预计剩余 {eta:.1f}s")

    if count % MSET_BATCH != 0:
        pipe.execute()

    elapsed = time.time() - t0
    print(f"\n{'=' * 50}")
    print(f"同步完成! 总耗时 {elapsed:.1f}s")
    print(f"写入 {count:,} 条, DB 大小: {r.dbsize():,}")
    print(f"{'=' * 50}")

    r.close()


if __name__ == "__main__":
    main()
