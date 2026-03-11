#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import subprocess
import shutil
import sys


def clean_directory(directory):
    """清理指定目录下的所有文件，防止旧数据干扰"""
    if os.path.exists(directory):
        for filename in os.listdir(directory):
            file_path = os.path.join(directory, filename)
            try:
                if os.path.isfile(file_path) or os.path.islink(file_path):
                    os.unlink(file_path)
                elif os.path.isdir(file_path):
                    shutil.rmtree(file_path)
            except Exception as e:
                print(f"⚠️ 清理 {file_path} 失败: {e}")
    else:
        os.makedirs(directory)


def run_benchmark_jeecg_boot():
    # --- 1. 基础配置 ---
    jar_path = "build/libs/WebAnalyzer-all.jar"
    base_benchmark_dir = "jeecg-boot"
    source_output_dir = "output"

    # --- 检查环境 ---
    if not os.path.exists(jar_path):
        print(f"❌ 错误: 找不到 Jar 包: {jar_path}")
        return

    if not os.path.exists(base_benchmark_dir):
        print(f"❌ 错误: 找不到测试目录: {base_benchmark_dir}")
        return

    # 获取版本目录列表
    versions = [d for d in os.listdir(base_benchmark_dir)
                if os.path.isdir(os.path.join(base_benchmark_dir, d)) and not d.startswith('.')]
    versions.sort()

    print(f"=== 🔍 发现 {len(versions)} 个版本 (jeecg-boot)，开始批量分析 ===")
    print(f"📦 Jar Path: {jar_path}")
    print("-" * 60)

    # --- 2. 遍历版本 ---
    for idx, version in enumerate(versions, 1):
        version_dir = os.path.join(base_benchmark_dir, version)
        config_path = os.path.join(version_dir, "options.yml")

        if not os.path.exists(config_path):
            print(f"⚠️ [{idx}/{len(versions)}] 版本 {version} 缺少 options.yml，跳过。")
            continue

        print(f"🚀 [{idx}/{len(versions)}] 处理版本: {version}")
        print(f"   ├─ 正在分析 ...", end=" ", flush=True)

        clean_directory(source_output_dir)

        command = [
            "java", "-jar", jar_path,
            f"-o={config_path}"
        ]

        try:
            subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)

            # 复制 tai-e.log
            src_log = os.path.join(source_output_dir, "tai-e.log")
            if os.path.exists(src_log):
                shutil.copy2(src_log, os.path.join(version_dir, "tai-e.log"))
            else:
                print(f"⚠️ 未找到 tai-e.log", end=" ")

            # 复制 taint-config-generated.yml
            src_taint = os.path.join(source_output_dir, "taint-config-generated.yml")
            if os.path.exists(src_taint):
                shutil.copy2(src_taint, os.path.join(version_dir, "taint-config-generated.yml"))
            else:
                print(f"⚠️ 未找到 taint-config-generated.yml", end=" ")

            print(f"✅ 完成")

        except subprocess.CalledProcessError:
            print(f"❌ 分析失败")
        except Exception as e:
            print(f"❌ 脚本异常: {e}")

    print("-" * 60)
    print("=== 🎉 jeecg-boot 批量任务执行完毕 ===")


if __name__ == "__main__":
    run_benchmark_jeecg_boot()