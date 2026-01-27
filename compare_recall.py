#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import glob
import statistics
import re

def natural_sort_key(s):
    """自然排序键生成器"""
    return [int(text) if text.isdigit() else text.lower()
            for text in re.split('([0-9]+)', s)]

def read_file_to_set(file_path):
    """读取文件内容，存入集合"""
    lines = set()
    if os.path.exists(file_path):
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                for line in f:
                    stripped = line.strip()
                    if stripped:
                        lines.add(stripped)
        except Exception as e:
            print(f"⚠️ 读取出错 {file_path}: {e}")
    return lines

def process_comparison(base_file, target_pattern, label):
    """
    处理对比逻辑：
    1. 打印详细对比过程
    2. 返回 (Sum, Ave) 元组供后续汇总使用
    """
    # 默认返回 0, 0
    if not os.path.exists(base_file):
        print(f"  ❌ [{label}] 基准文件缺失: {os.path.basename(base_file)}")
        return 0, 0.0

    base_set = read_file_to_set(base_file)
    # print(f"  📂 [{label}] 基准数据量: {len(base_set)}")

    target_files = glob.glob(target_pattern)
    target_files.sort(key=lambda f: natural_sort_key(os.path.basename(f)))

    if not target_files:
        print(f"  ⚠️ [{label}] 未找到匹配文件")
        return 0, 0.0

    print(f"  📊 [{label}] 详细对比 (基准 vs Dyer):")

    intersection_counts = []

    for target_path in target_files:
        target_name = os.path.basename(target_path)
        target_set = read_file_to_set(target_path)

        common_count = len(base_set.intersection(target_set))
        intersection_counts.append(common_count)

        # 打印单行详情
        print(f"     - vs {target_name:<20} : {common_count} 条相同")

    # 计算统计值
    total_sum = sum(intersection_counts)
    average = statistics.mean(intersection_counts) if intersection_counts else 0.0

    print(f"     {'-'*40}")
    print(f"     >> 总和 (Sum) : {total_sum}")
    print(f"     >> 平均 (Ave) : {average:.1f}")
    print("")

    return total_sum, average

def main():
    base_dir = os.getcwd()
    benchmark_b2_dir = os.path.join(base_dir, "benchmark_b2")
    dyer_dir = os.path.join(base_dir, "dyer")

    if not os.path.exists(benchmark_b2_dir) or not os.path.exists(dyer_dir):
        print("❌ 错误: 未找到目录，请确保在 WebAnalyzer 根目录下运行脚本。")
        return

    projects = [d for d in os.listdir(benchmark_b2_dir)
                if os.path.isdir(os.path.join(benchmark_b2_dir, d)) and not d.startswith('.')]
    projects.sort()

    # 用于存储汇总数据: [ (name, edge_sum, edge_ave, method_sum, method_ave), ... ]
    summary_data = []

    print(f"=== 开始详细对比分析 ({len(projects)} 个项目) ===")

    for project in projects:
        print("=" * 80)
        print(f"🚀 项目: {project}")
        print("=" * 80)

        b2_result_dir = os.path.join(benchmark_b2_dir, project, "result")
        dyer_proj_dir = os.path.join(dyer_dir, project)

        # --- 1. 对比 Call Edges ---
        base_edges = os.path.join(b2_result_dir, "call-edges.txt")
        target_edges_pattern = os.path.join(dyer_proj_dir, "calledges-*.txt")

        e_sum, e_ave = process_comparison(base_edges, target_edges_pattern, "Call Edges")

        # --- 2. 对比 Reachable Methods ---
        base_methods = os.path.join(b2_result_dir, "reachable-methods.txt")
        target_methods_pattern = os.path.join(dyer_proj_dir, "methods-*.txt")

        m_sum, m_ave = process_comparison(base_methods, target_methods_pattern, "Methods")

        # 收集数据
        summary_data.append({
            "name": project,
            "e_sum": e_sum,
            "e_ave": e_ave,
            "m_sum": m_sum,
            "m_ave": m_ave
        })

    # --- 最终汇总表格输出 ---
    print("\n" + "="*100)
    print("📋 最终结果汇总表格")
    print("="*100)

    # 表头
    header = f"{'Project':<15} | {'Edge Sum':<12} | {'Edge Ave':<12} | {'Method Sum':<12} | {'Method Ave':<12}"
    print(header)
    print("-" * len(header))

    for item in summary_data:
        print(f"{item['name']:<15} | {item['e_sum']:<12} | {item['e_ave']:<12.1f} | {item['m_sum']:<12} | {item['m_ave']:<12.1f}")

    print("="*100)
    print(f"✅ 统计完成，共处理 {len(summary_data)} 个项目。")

if __name__ == "__main__":
    main()