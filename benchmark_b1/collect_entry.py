import os
import re
import csv
from datetime import datetime

def extract_router_methods_count(log_file_path):
    """
    从 console_run.log 文件中提取 Router Methods 数量和 Duration 时间

    Args:
        log_file_path: 日志文件路径

    Returns:
        tuple: (Router Methods 数量, Duration 时间), 如果未找到则返回 (None, None)
    """
    if not os.path.exists(log_file_path):
        return None, None

    try:
        with open(log_file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # 使用正则表达式匹配 "Router Methods (Endpoints)     : 327" 这样的行
        router_pattern = r'Router Methods \(Endpoints\)\s*:\s*(\d+)'
        router_match = re.search(router_pattern, content)

        # 使用正则表达式匹配 "Router analysis completed at: ... (Duration: 7ms)" 这样的行
        duration_pattern = r'Router analysis completed at:.*?\(Duration:\s*(\d+(?:\.\d+)?)(ms|s)\)'
        duration_match = re.search(duration_pattern, content)

        router_count = int(router_match.group(1)) if router_match else None
        duration_str = None

        if duration_match:
            duration_value = duration_match.group(1)
            duration_unit = duration_match.group(2)
            duration_str = f"{duration_value}{duration_unit}"

        return router_count, duration_str

    except Exception as e:
        print(f"⚠️ 读取文件 {log_file_path} 时出错: {e}")
        return None, None

def collect_router_methods_stats():
    """
    收集所有项目的 Router Methods 统计数据
    """
    # 当前目录应该是 benchmark_b1
    base_dir = "."
    result_subdir = "entrys"
    log_filename = "console_run.log"

    # 获取所有项目目录
    projects = [d for d in os.listdir(base_dir)
                if os.path.isdir(os.path.join(base_dir, d))
                and not d.startswith('.')]  # 排除隐藏目录

    # 排除非项目文件（如果有）
    exclude_items = ['__pycache__', 'venv', '.git']
    projects = [p for p in projects if p not in exclude_items]
    projects.sort()

    print("=" * 70)
    print("           Router Methods 统计收集工具")
    print("=" * 70)
    print(f"📁 扫描目录: {os.path.abspath(base_dir)}")
    print(f"🔍 发现 {len(projects)} 个项目")
    print("-" * 70)

    # 收集数据
    results = []
    success_count = 0
    fail_count = 0

    for idx, project_name in enumerate(projects, 1):
        log_path = os.path.join(base_dir, project_name, "result", result_subdir, log_filename)

        print(f"[{idx}/{len(projects)}] {project_name:<20}", end=" ")

        router_count, duration = extract_router_methods_count(log_path)

        if router_count is not None:
            results.append({
                'project': project_name,
                'router_methods': router_count,
                'duration': duration,
                'log_path': log_path
            })
            duration_info = f" | Duration: {duration}" if duration else ""
            print(f"✅ Router Methods: {router_count}{duration_info}")
            success_count += 1
        else:
            results.append({
                'project': project_name,
                'router_methods': 'N/A',
                'duration': 'N/A',
                'log_path': log_path
            })
            print(f"❌ 未找到数据")
            fail_count += 1

    print("-" * 70)

    # 保存结果到 CSV 文件
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    csv_filename = f"router_methods_stats_{timestamp}.csv"

    try:
        with open(csv_filename, 'w', newline='', encoding='utf-8-sig') as csvfile:
            fieldnames = ['Project', 'Router Methods', 'Log Path']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)

            writer.writeheader()
            for result in results:
                writer.writerow({
                    'Project': result['project'],
                    'Router Methods': result['router_methods'],
                    'Log Path': result['log_path']
                })

        print(f"✅ 结果已保存到: {csv_filename}")
    except Exception as e:
        print(f"❌ 保存 CSV 文件失败: {e}")

    # 生成文本报告
    txt_filename = f"router_methods_stats_{timestamp}.txt"

    try:
        with open(txt_filename, 'w', encoding='utf-8') as f:
            f.write("=" * 70 + "\n")
            f.write("           Router Methods 统计报告\n")
            f.write("=" * 70 + "\n")
            f.write(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"项目总数: {len(projects)}\n")
            f.write(f"成功收集: {success_count}\n")
            f.write(f"收集失败: {fail_count}\n")
            f.write("=" * 70 + "\n\n")

            # 按 Router Methods 数量降序排序
            sorted_results = sorted(
                [r for r in results if isinstance(r['router_methods'], int)],
                key=lambda x: x['router_methods'],
                reverse=True
            )

            f.write("排名\t项目名称\t\t\tRouter Methods\n")
            f.write("-" * 70 + "\n")

            for rank, result in enumerate(sorted_results, 1):
                f.write(f"{rank}\t{result['project']:<24}\t{result['router_methods']}\n")

            # 添加未成功的项目
            failed_results = [r for r in results if not isinstance(r['router_methods'], int)]
            if failed_results:
                f.write("\n" + "=" * 70 + "\n")
                f.write("未成功收集的项目:\n")
                f.write("-" * 70 + "\n")
                for result in failed_results:
                    f.write(f"- {result['project']}\n")

            # 统计信息
            if sorted_results:
                total = sum(r['router_methods'] for r in sorted_results)
                avg = total / len(sorted_results)
                max_project = max(sorted_results, key=lambda x: x['router_methods'])
                min_project = min(sorted_results, key=lambda x: x['router_methods'])

                f.write("\n" + "=" * 70 + "\n")
                f.write("统计摘要:\n")
                f.write("-" * 70 + "\n")
                f.write(f"总计 Router Methods: {total}\n")
                f.write(f"平均值: {avg:.2f}\n")
                f.write(f"最大值: {max_project['router_methods']} ({max_project['project']})\n")
                f.write(f"最小值: {min_project['router_methods']} ({min_project['project']})\n")

        print(f"✅ 报告已保存到: {txt_filename}")
    except Exception as e:
        print(f"❌ 保存文本报告失败: {e}")

    # 打印汇总统计
    print("\n" + "=" * 70)
    print("📊 统计摘要")
    print("=" * 70)
    print(f"✅ 成功收集: {success_count} 个项目")
    print(f"❌ 收集失败: {fail_count} 个项目")

    if sorted_results := [r for r in results if isinstance(r['router_methods'], int)]:
        total = sum(r['router_methods'] for r in sorted_results)
        avg = total / len(sorted_results)
        print(f"📈 总计 Router Methods: {total}")
        print(f"📊 平均 Router Methods: {avg:.2f}")
        print(f"🔝 最多: {max(sorted_results, key=lambda x: x['router_methods'])['project']} "
              f"({max(sorted_results, key=lambda x: x['router_methods'])['router_methods']})")
        print(f"🔻 最少: {min(sorted_results, key=lambda x: x['router_methods'])['project']} "
              f"({min(sorted_results, key=lambda x: x['router_methods'])['router_methods']})")

    print("=" * 70)
    print("🎉 收集完成!")

if __name__ == "__main__":
    collect_router_methods_stats()