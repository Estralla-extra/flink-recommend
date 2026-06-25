import redis

# 连接 Redis（测试阶段用虚拟机 IP，部署时改为 localhost）
r = redis.Redis(host='192.168.249.102',
                port=6379,
                password='myredis123',
                decode_responses=True)

# 请从你的数据集中选几个常见的商品 ID（在 mini_behavior.csv 中出现次数较多的）
test_items = ['1535294', '3122135', '2331370', '3031354', '4091349']

print("========== Redis 相似度验证 ==========")
found = 0
missing = 0

for item_id in test_items:
    key = f"sim:{item_id}"
    value = r.get(key)
    if value:
        # 只显示前三个相似商品，避免刷屏
        sims = value.split(',')
        preview = sims[:3]
        print(f"✅ {key}: {', '.join(preview)}...")
        found += 1
    else:
        print(f"❌ {key}: 未找到（可能是该商品出现次数 < min_count 被过滤）")
        missing += 1

print(f"\n验证完毕：{found} 个有数据，{missing} 个缺失")
print(f"数据库总键数：{r.dbsize()}")