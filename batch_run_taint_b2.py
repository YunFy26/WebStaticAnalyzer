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

def ensure_result_dir_is_folder(path):
    """确保 result 路径是一个文件夹，如果是文件则删除重建"""
    if os.path.exists(path):
        if os.path.isfile(path):
            print(f"⚠️ 检测到 {path} 是一个文件（应为目录），正在删除并重建...")
            try:
                os.remove(path)
                os.makedirs(path)
            except Exception as e:
                print(f"❌ 无法删除文件 {path}: {e}")
                sys.exit(1)
    else:
        os.makedirs(path)

def run_benchmark_taint_b2():
    # --- 1. 基础配置 ---
    jar_path = "build/libs/WebAnalyzer-all.jar"

    base_benchmark_dir = "taint_b2"
    source_output_dir = "output"

    # --- 检查环境 ---
    if not os.path.exists(jar_path):
        print(f"❌ 错误: 找不到 Jar 包: {jar_path}")
        return

    if not os.path.exists(base_benchmark_dir):
        print(f"❌ 错误: 找不到测试目录: {base_benchmark_dir}")
        return

    projects = [d for d in os.listdir(base_benchmark_dir)
                if os.path.isdir(os.path.join(base_benchmark_dir, d)) and not d.startswith('.')]

    projects.sort()

    print(f"=== 🔍 发现 {len(projects)} 个项目 (Taint B2)，开始批量分析 ===")
    print(f"📦 Jar Path: {jar_path}")
    print("-" * 60)

    # --- 2. 遍历项目 ---
    for proj_idx, project_name in enumerate(projects, 1):
        project_dir = os.path.join(base_benchmark_dir, project_name)
        config_path = os.path.join(project_dir, "options.yml")
        target_result_dir = os.path.join(project_dir, "result")

        if not os.path.exists(config_path):
            print(f"⚠️ [{proj_idx}/{len(projects)}] 项目 {project_name} 缺少 options.yml，跳过。")
            continue

        print(f"🚀 [{proj_idx}/{len(projects)}] 处理项目: {project_name}")
        ensure_result_dir_is_folder(target_result_dir)
        print(f"   ├─ 正在分析 ...", end=" ", flush=True)

        clean_directory(source_output_dir)

        command = [
            "java", "-jar", jar_path,
            f"-o={config_path}"
        ]

        try:
            subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)

            src_log = os.path.join(source_output_dir, "tai-e.log")
            if os.path.exists(src_log):
                shutil.copy2(src_log, os.path.join(target_result_dir, "tai-e.log"))
                print(f"✅ 完成")
            else:
                print(f"⚠️ 完成但未找到 tai-e.log")

        except subprocess.CalledProcessError:
            print(f"❌ 分析失败")
        except Exception as e:
            print(f"❌ 脚本异常: {e}")

    print("-" * 60)
    print("=== 🎉 Taint B2 批量任务执行完毕 ===")

if __name__ == "__main__":
    run_benchmark_taint_b2()