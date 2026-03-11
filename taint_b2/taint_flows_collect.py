import os
import re

def collect_tai_e_data():
    # 获取脚本所在的当前目录 (taint_b2)
    base_dir = os.path.dirname(os.path.abspath(__file__))

    # 遍历当前目录下的所有子文件夹
    projects = [d for d in os.listdir(base_dir) if os.path.isdir(os.path.join(base_dir, d))]

    for project in sorted(projects):
        log_path = os.path.join(base_dir, project, "result", "tai-e.log")

        if not os.path.exists(log_path):
            continue

        print(f"{'='*20} Project: {project} {'='*20}")

        taint_flows = ""
        elapsed_time = ""
        sink_nodes = []
        is_collecting_sinks = False

        try:
            with open(log_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()

                    # 1. 收集 Taint Flow 数量
                    if "Detected" in line and "taint flow(s):" in line:
                        taint_flows = line

                    # 2. 收集 运行结束时间
                    if "Tai-e finishes, elapsed time:" in line:
                        elapsed_time = line

                    # 3. 收集 Sink Nodes 数据 (状态机模式)
                    if "Sink nodes:" in line:
                        is_collecting_sinks = True
                        continue

                    if is_collecting_sinks:
                        # 如果遇到 Dumping 路径，停止收集
                        if "Dumping" in line and "taint-flow-graph.dot" in line:
                            is_collecting_sinks = False
                        else:
                            if line: # 确保不是空行
                                sink_nodes.append(line)

            # 打印提取结果
            if taint_flows:
                print(f"[Flows] {taint_flows}")

            if sink_nodes:
                print("[Sinks]")
                for node in sink_nodes:
                    print(f"  {node}")

            if elapsed_time:
                print(f"[Time]  {elapsed_time}")

            print("\n")

        except Exception as e:
            print(f"Error reading {log_path}: {e}\n")

if __name__ == "__main__":
    collect_tai_e_data()