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
        # 如果目录不存在，创建它，防止后续报错
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

def run_benchmark_b2():
    # --- 1. 基础配置 ---
    # 根据截图，Jar包在 build/libs 下
    jar_path = "build/libs/WebAnalyzer-all.jar"

    base_benchmark_dir = "benchmark_b2"
    source_output_dir = "output"  # 工具默认输出目录

    # 需要保存的文件列表
    files_to_save = [
        "call-edges.txt",
        "tai-e.log",
        "reachable-methods.txt",
        "aop-call-edges.txt",
        "tai-e-plan.yml"
    ]

    # --- 检查环境 ---
    if not os.path.exists(jar_path):
        print(f"❌ 错误: 找不到 Jar 包: {jar_path}")
        return

    if not os.path.exists(base_benchmark_dir):
        print(f"❌ 错误: 找不到测试目录: {base_benchmark_dir}")
        return

    # 获取 benchmark_b2 下的所有项目目录 (排除文件和隐藏文件夹)
    projects = [d for d in os.listdir(base_benchmark_dir)
                if os.path.isdir(os.path.join(base_benchmark_dir, d)) and not d.startswith('.')]

    projects.sort() # 排序，保证执行顺序一致

    print(f"=== 🔍 发现 {len(projects)} 个项目 (Benchmark B2)，开始批量分析 ===")
    print(f"📦 Jar Path: {jar_path}")
    print("-" * 60)

    # --- 2. 遍历项目 ---
    for proj_idx, project_name in enumerate(projects, 1):
        project_dir = os.path.join(base_benchmark_dir, project_name)

        # B2 的特点：配置文件直接位于项目根目录下，且名为 options.yml
        config_path = os.path.join(project_dir, "options.yml")

        # 结果存放目录
        target_result_dir = os.path.join(project_dir, "result")

        # 检查配置文件是否存在
        if not os.path.exists(config_path):
            print(f"⚠️ [{proj_idx}/{len(projects)}] 项目 {project_name} 缺少 options.yml，跳过。")
            continue

        print(f"🚀 [{proj_idx}/{len(projects)}] 处理项目: {project_name}")

        # 核心修改：确保 result 是个文件夹，如果是文件会报错，这里自动处理掉
        ensure_result_dir_is_folder(target_result_dir)

        print(f"   ├─ 正在分析: options.yml ...", end=" ", flush=True)

        # A. 运行前清理 output 目录，确保结果纯净
        clean_directory(source_output_dir)

        # B. 构建并执行命令
        command = [
            "java", "-jar", jar_path,
            f"-o={config_path}"
        ]

        # 控制台日志保存路径
        console_log_path = os.path.join(target_result_dir, "console_run.log")

        try:
            # 执行命令并将输出重定向到 result 目录下的 console_run.log
            with open(console_log_path, "w") as f_log:
                subprocess.run(command, stdout=f_log, stderr=subprocess.STDOUT, check=True)

            # C. 复制结果文件
            copied_count = 0
            for filename in files_to_save:
                src_file = os.path.join(source_output_dir, filename)
                dst_file = os.path.join(target_result_dir, filename)

                if os.path.exists(src_file):
                    # 使用 copy2 可以保留文件元数据
                    shutil.copy2(src_file, dst_file)
                    copied_count += 1

            # 同时也确认 console_run.log 是否成功生成
            if os.path.exists(console_log_path):
                copied_count += 1

            print(f"✅ 完成 (已收集文件到 result/)")

        except subprocess.CalledProcessError:
            print(f"❌ 分析失败 (请查看日志: {console_log_path})")
        except Exception as e:
            print(f"❌ 脚本异常: {e}")

    print("-" * 60)
    print("=== 🎉 Benchmark B2 批量任务执行完毕 ===")

if __name__ == "__main__":
    run_benchmark_b2()