#!/bin/bash
set -e
ACTION=$1
INPUT=${2:-"hdfs:///flink-recommend/datas/train_20m.csv"}
OUTPUT=${3:-"hdfs:///item2vec/output"}
CODE_DIR="/opt/item2vec/code"
LOG_DIR="/opt/item2vec/logs"
export HADOOP_CONF_DIR=/opt/module/hadoop-3.3.5/etc/hadoop
mkdir -p "$LOG_DIR"
env_up() {
    echo "启动 ZooKeeper..."; zk.sh start; sleep 3
    echo "启动 Hadoop..."; hdp.sh start; sleep 5
    echo "启动 Redis..."; /opt/module/redis/bin/redis-server /opt/module/redis/redis.conf; sleep 2
    echo "环境启动完成"
}
env_down() {
    echo "停止 Redis..."; ps aux | grep redis-server | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null || true
    echo "停止 Hadoop..."; hdp.sh stop
    echo "停止 ZooKeeper..."; zk.sh stop
    echo "环境已停止"
}
train() {
    echo "ALS 训练..."
    spark-submit \
      --master yarn \
      --deploy-mode cluster \
      --name "Item2Vec_ALS" \
      --num-executors 3 \
      --executor-cores 2 \
      --executor-memory 1536m \
      --driver-memory 1024m \
      --conf spark.sql.shuffle.partitions=200 \
      --conf spark.yarn.jars=hdfs:///spark-jars/*.jar \
      --conf spark.executor.memoryOverhead=384 \
      --conf spark.driver.memoryOverhead=384 \
      --conf "spark.yarn.dist.archives=hdfs:///flink-recommend/.venv.tar.gz#.venv" \
      --conf "spark.pyspark.python=./.venv/bin/python" \
      --conf "spark.pyspark.driver.python=./.venv/bin/python" \
      "$CODE_DIR/train_als.py" \
      --input "$INPUT" \
      --output "$OUTPUT" \
      > "$LOG_DIR/train.log" 2>&1
}
search_and_sync() {
    echo "FAISS 搜索..."
    python3 "$CODE_DIR/search_similar_faiss.py" --output "$OUTPUT" > "$LOG_DIR/faiss.log" 2>&1
    echo "Redis 同步..."
    python3 "$CODE_DIR/sync_redis.py" --results "$OUTPUT/sim_results.parquet" > "$LOG_DIR/redis-sync.log" 2>&1
    echo "完成!"
}
case "$ACTION" in
    env-up) env_up ;;
    env-down) env_down ;;
    full) train; search_and_sync ;;
    refresh) search_and_sync ;;
    *) echo "用法: $0 {env-up|env-down|full|refresh}"; exit 1 ;;
esac
