#!/bin/bash
# 推荐系统服务管理脚本
# 注意: ZK + Hadoop + Redis 由 train.sh 管理，本脚本不做启停
# 用法: ./services.sh start
#       ./services.sh stop

ACTION=$1
LOG_DIR="/opt/recommend/logs"
mkdir -p "$LOG_DIR"

start_all() {
    echo "启动 Kafka..."
    kf.sh start
    sleep 5

    echo "启动 Flink 集群..."
    /opt/module/flink-1.17.0/bin/start-cluster.sh
   sleep 8

    echo "启动 DataProducer..."
    nohup java -cp /opt/recommend/flink-job-1.0-SNAPSHOT.jar \
        com.recommend.kafka.DataProducer /opt/item2vec/train_20m.csv \
        > /opt/recommend/logs/data-producer.log 2>&1 &

    echo "启动 HeartbeatProducer..."
    nohup java -cp /opt/recommend/flink-job-1.0-SNAPSHOT.jar \
        com.recommend.kafka.HeartbeatProducer \
        > /opt/recommend/logs/heartbeat.log 2>&1 &

    echo "启动 Flink 作业..."
    nohup /opt/module/flink-1.17.0/bin/flink run /opt/recommend/flink-job-1.0-SNAPSHOT.jar \
        > /opt/recommend/logs/flink-job.log 2>&1 &

    echo "启动 Spring Boot..."
    nohup java -jar /opt/recommend/recommend-web-1.0-SNAPSHOT.jar \
        > /opt/recommend/logs/web.log 2>&1 &

    echo ""
    echo "全部服务启动完成!"
    echo "  监控大屏: http://hadoop102:18080"
    echo "  Flink UI: http://hadoop102:8081"
}

stop_all() {
    echo "注意: ZK + Hadoop + Redis 由 train.sh 管理，services.sh 不做启停"

    echo "停止 DataProducer..."
    ps aux | grep DataProducer | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null || true

    echo "停止 HeartbeatProducer..."
    ps aux | grep HeartbeatProducer | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null || true

    echo "停止 Spring Boot..."
    sleep 3
    echo "  (等待端口释放...)"
    ps aux | grep recommend-web | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null || true

    echo "停止 Flink 作业..."
    /opt/module/flink-1.17.0/bin/flink list 2>/dev/null | grep "RealtimeRecommendJob" | awk '{print $4}' | sed 's/://' | while read JOB_ID; do
        /opt/module/flink-1.17.0/bin/flink cancel "$JOB_ID" 2>/dev/null || true
    done

    echo "停止 Flink 集群..."
    /opt/module/flink-1.17.0/bin/stop-cluster.sh

    echo "停止 Kafka..."
    kf.sh stop

    echo ""
    echo "服务已全部停止"
}

case "$ACTION" in
    start)
        start_all
        ;;
    stop)
        stop_all
        ;;
    *)
        echo "用法: $0 {start|stop}"
        exit 1
        ;;
esac
