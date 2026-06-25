"""
FAISS CPU 相似度搜索 — 读取 ALS itemFactors → top-20 相似结果
用法:
  python search_similar_faiss.py --output hdfs:///item2vec/output
输入:
  {output}/item_factors/   ALS 输出的商品向量 (parquet)
  {output}/item_mapping/   商品编码映射 (parquet)
输出:
  {output}/sim_results.parquet  每个商品 top-20 相似结果
"""
import numpy as np
import faiss
import pandas as pd
import time

HDFS_OUTPUT = "hdfs:///item2vec/output"
TOPN = 20
IVF_CENTROIDS = 1000
NPROBE = 100
FAISS_BATCH = 100000
NUM_THREADS = 0


def read_parquet(path, local):
    if local:
        return pd.read_parquet(path)
    from pyspark.sql import SparkSession
    spark = SparkSession.builder.appName("ReadParquet").getOrCreate()
    pdf = spark.read.parquet(path).toPandas()
    spark.stop()
    return pdf


def write_parquet(df, path, local):
    import subprocess, os
    local_path = "/tmp/item2vec/sim_results.parquet"
    os.makedirs("/tmp/item2vec", exist_ok=True)
    df.to_parquet(local_path)
    if local:
        os.rename(local_path, path)
    else:
        subprocess.run(["hadoop", "fs", "-put", "-f", local_path, path], check=False)


def main():
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default=HDFS_OUTPUT)
    parser.add_argument("--topn", type=int, default=TOPN)
    parser.add_argument("--local", action="store_true")
    args = parser.parse_args()

    t0 = time.time()
    factors_path = f"{args.output}/item_factors"
    mapping_path = f"{args.output}/item_mapping"

    print("Stage 1: 读取 item vectors...")
    factors = read_parquet(factors_path, args.local)
    mapping = read_parquet(mapping_path, args.local)
    factors = factors.sort_values("id").reset_index(drop=True)
    mapping = mapping.sort_values("item_idx").reset_index(drop=True)
    vectors = np.array([v for v in factors["features"]], dtype=np.float32)
    print(f"  加载完成: {len(vectors):,} 个, 维度 {vectors.shape[1]}, 耗时 {time.time()-t0:.1f}s")

    print("Stage 2: L2 归一化...")
    vectors = vectors / (np.linalg.norm(vectors, axis=1, keepdims=True) + 1e-10)

    print("Stage 3: 构建 FAISS 索引...")
    if NUM_THREADS > 0:
        faiss.omp_set_num_threads(NUM_THREADS)
    quantizer = faiss.IndexFlatIP(vectors.shape[1])
    index = faiss.IndexIVFFlat(quantizer, vectors.shape[1], IVF_CENTROIDS, faiss.METRIC_INNER_PRODUCT)
    index.train(vectors)
    index.nprobe = NPROBE
    index.add(vectors)
    print(f"  索引构建完成, 耗时 {time.time()-t0:.1f}s")

    print(f"Stage 4: 搜索 top-{args.topn}...")
    results = []
    n = len(vectors)
    for i in range(0, n, FAISS_BATCH):
        j = min(i + FAISS_BATCH, n)
        scores, ids_arr = index.search(vectors[i:j], args.topn + 1)
        for k in range(j - i):
            idx = i + k
            mask = ids_arr[k] != idx
            top_ids = ids_arr[k][mask][:args.topn]
            top_scores = scores[k][mask][:args.topn]
            orig_id = mapping.loc[idx, "item_id"]
            sims = ",".join(f"{mapping.loc[sim_idx, 'item_id']}:{s:.4f}" for sim_idx, s in zip(top_ids, top_scores))
            results.append({"item_id": orig_id, "similars": sims})
        pct = 100 * j / n
        eta = (time.time() - t0) / j * (n - j) if j > 0 else 0
        print(f"  进度: {j:,}/{n:,} ({pct:.1f}%) | 已耗时 {time.time()-t0:.1f}s | 预计剩余 {eta:.1f}s")

    print("Stage 5: 保存结果...")
    result_df = pd.DataFrame(results)
    write_parquet(result_df, f"{args.output}/sim_results.parquet", args.local)
    print(f"  完成! 总耗时 {time.time()-t0:.1f}s")


if __name__ == "__main__":
    main()
