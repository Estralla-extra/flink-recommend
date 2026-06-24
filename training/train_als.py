"""
PySpark ALS 训练 — 替代 Word2Vec 方案

用法 (集群):
  spark-submit --master yarn train_als.py \
    --input hdfs:///flink—recommend/datas/UserBehavior.csv \
    --output hdfs:///item2vec/output

输出:
  {output}/user_mapping/   用户编码映射
  {output}/item_mapping/   商品编码映射
  {output}/item_factors/   商品向量 (ALS itemFactors)
"""
import time
from pyspark import SparkConf
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, row_number, monotonically_increasing_id
from pyspark.sql.types import IntegerType
from pyspark.sql.window import Window
from pyspark.ml.recommendation import ALS

HDFS_INPUT = "hdfs:///flink—recommend/datas/UserBehavior.csv"
HDFS_OUTPUT = "hdfs:///item2vec/output"
MIN_COUNT = 2
ALS_RANK = 128
ALS_EPOCHS = 15
ALS_REG = 0.01
ALS_ALPHA = 40.0
SHUFFLE_PARTITIONS = 200


def main():
    import argparse
    parser = argparse.ArgumentParser(description="ALS Item2Vec 训练")
    parser.add_argument("--input", default=HDFS_INPUT)
    parser.add_argument("--output", default=HDFS_OUTPUT)
    parser.add_argument("--min-count", type=int, default=MIN_COUNT)
    parser.add_argument("--local", action="store_true", help="本地模式")
    args = parser.parse_args()

    t0 = time.time()
    conf = SparkConf()
    conf.setAppName("Item2Vec_ALS")
    if args.local:
        conf.set("spark.sql.shuffle.partitions", "20")
    else:
        conf.set("spark.yarn.dist.archives", "hdfs:///flink—recommend/.venv.tar.gz#.venv")
        conf.set("spark.pyspark.python", "./.venv/bin/python")
        conf.set("spark.pyspark.driver.python", "./.venv/bin/python")
        conf.set("spark.sql.shuffle.partitions", str(SHUFFLE_PARTITIONS))
    spark = SparkSession.builder.config(conf=conf).getOrCreate()

    print("=" * 50)
    print("Stage 1: 读取原始数据...")
    print("=" * 50)
    df = spark.read.csv(
        args.input,
        schema="user_id STRING,item_id STRING,cat_id STRING,behavior STRING,timestamp LONG",
        header=False,
    )
    raw_count = df.count()
    print(f"  原始数据: {raw_count:,} 条")

    print("\nStage 2: 过滤强交互行为...")
    df = df.filter(col("behavior").isin(["buy", "cart", "fav"]))
    filtered_count = df.count()
    print(f"  过滤后: {filtered_count:,} 条")

    print("\nStage 3: 构造隐式反馈...")
    implicit = df.groupBy("user_id", "item_id").count().withColumnRenamed("count", "cnt")

    print(f"\nStage 4: 过滤低频 item (min_count={args.min_count})...")
    item_count = implicit.groupBy("item_id").agg({"cnt": "sum"})
    valid_items = item_count.filter(col("sum(cnt)") >= args.min_count).select("item_id")
    implicit = implicit.join(valid_items, "item_id")
    print(f"  有效 item: {valid_items.count():,}")

    print("\nStage 5: 整数编码 (使用 row_number)...")
    users = implicit.select("user_id").distinct() \
        .withColumn("user_idx", row_number().over(Window.orderBy("user_id")) - 1)
    items = implicit.select("item_id").distinct() \
        .withColumn("item_idx", row_number().over(Window.orderBy("item_id")) - 1)

    train = implicit.join(users, "user_id").join(items, "item_id") \
        .select(col("user_idx").cast(IntegerType()),
                col("item_idx").cast(IntegerType()),
                col("cnt").cast(IntegerType()))

    print(f"  用户: {users.count():,}, 商品: {items.count():,}, 训练样本: {train.count():,}")

    print("\nStage 5.5: 保存编码映射...")
    users.write.mode("overwrite").parquet(f"{args.output}/user_mapping")
    items.write.mode("overwrite").parquet(f"{args.output}/item_mapping")
    print(f"  user_mapping/ -> {args.output}/user_mapping")
    print(f"  item_mapping/ -> {args.output}/item_mapping")

    print(f"\nStage 6: ALS 训练 (rank={ALS_RANK})...")
    als = ALS(userCol="user_idx", itemCol="item_idx", ratingCol="cnt",
              rank=ALS_RANK, maxIter=ALS_EPOCHS, regParam=ALS_REG,
              implicitPrefs=True, alpha=ALS_ALPHA, coldStartStrategy="drop")
    model = als.fit(train)
    item_factors = model.itemFactors
    n_items = item_factors.count()
    vec = item_factors.select("features").first()["features"]
    print(f"  itemFactors: {n_items:,} 个向量, 维度 {len(vec)}")

    print("\nStage 7: 保存结果...")
    item_factors.write.mode("overwrite").parquet(f"{args.output}/item_factors")
    elapsed = time.time() - t0
    print(f"\n训练完成! 总耗时 {elapsed:.1f}s")
    print(f"输出: {args.output}/")
    spark.stop()


if __name__ == "__main__":
    main()
