import os
import subprocess
import shutil

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

def run_entry_points_count_analysis():
    # --- 1. 基础配置 ---
    jar_path = "build/libs/WebAnalyzer-all.jar"
    base_benchmark_dir = "benchmark_b1"
    source_output_dir = "output"  # 工具默认输出目录
    config_filename = "entry_points_count.yml"  # 要执行的配置文件名
    result_subdirectory = "entrys"  # 结果保存的子目录名

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

    # 获取 benchmark_b1 下的所有项目目录 (排除文件)
    projects = [d for d in os.listdir(base_benchmark_dir)
                if os.path.isdir(os.path.join(base_benchmark_dir, d))]

    projects.sort()  # 排序，保证执行顺序一致

    print(f"=== 🔍 发现 {len(projects)} 个项目，开始执行 {config_filename} 分析 ===")
    print(f"📦 Jar Path: {jar_path}")
    print(f"📋 配置文件: {config_filename}")
    print(f"📁 结果目录: result/{result_subdirectory}/")
    print("-" * 60)

    # --- 2. 遍历项目 ---
    success_count = 0
    fail_count = 0
    skip_count = 0

    for proj_idx, project_name in enumerate(projects, 1):
        project_dir = os.path.join(base_benchmark_dir, project_name)
        configs_dir = os.path.join(project_dir, "configs")
        config_path = os.path.join(configs_dir, config_filename)

        # 检查配置文件是否存在
        if not os.path.exists(config_path):
            print(f"⚠️ [{proj_idx}/{len(projects)}] 项目 {project_name}: 配置文件不存在，跳过")
            skip_count += 1
            continue

        # 创建结果存放目录: benchmark_b1/{project_name}/result/entrys/
        target_result_dir = os.path.join(project_dir, "result", result_subdirectory)
        if not os.path.exists(target_result_dir):
            os.makedirs(target_result_dir)

        print(f"🚀 [{proj_idx}/{len(projects)}] 正在分析项目: {project_name} ...", end=" ", flush=True)

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
            with open(console_log_path, "w") as f_log:
                subprocess.run(command, stdout=f_log, stderr=subprocess.STDOUT, check=True)

            # C. 复制结果文件
            copied_count = 0
            for filename in files_to_save:
                src_file = os.path.join(source_output_dir, filename)
                dst_file = os.path.join(target_result_dir, filename)

                if os.path.exists(src_file):
                    shutil.copy(src_file, dst_file)
                    copied_count += 1

            print(f"✅ 完成 (保存 {copied_count} 个文件)")
            success_count += 1

        except subprocess.CalledProcessError:
            print(f"❌ 分析失败 (查看日志: {console_log_path})")
            fail_count += 1
        except Exception as e:
            print(f"❌ 异常: {e}")
            fail_count += 1

    # --- 3. 汇总统计 ---
    print("-" * 60)
    print("=== 📊 执行统计 ===")
    print(f"✅ 成功: {success_count} 个项目")
    print(f"❌ 失败: {fail_count} 个项目")
    print(f"⚠️ 跳过: {skip_count} 个项目")
    print(f"📁 总计: {len(projects)} 个项目")
    print("=== 🎉 批量任务执行完毕 ===")

if __name__ == "__main__":
    run_entry_points_count_analysis()