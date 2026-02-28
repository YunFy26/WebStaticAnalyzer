import os
import glob

def read_lines_to_set(file_path):
    """读取文件内容并返回去重后的集合，去除首尾空白"""
    if not os.path.exists(file_path):
        print(f"Warning: File not found: {file_path}")
        return set()

    with open(file_path, 'r', encoding='utf-8') as f:
        # strip() 用于去除行首尾的换行符和空格，确保对比准确
        return {line.strip() for line in f if line.strip()}

def append_intersection_to_file(base_set, target_file_path, output_path):
    """计算 target_file 与 base_set 的交集，并将结果追加到 output_path"""
    if not os.path.exists(target_file_path):
        return 0

    target_lines = read_lines_to_set(target_file_path)
    # 计算交集
    intersection = base_set.intersection(target_lines)

    if intersection:
        with open(output_path, 'a', encoding='utf-8') as f:
            for line in intersection:
                f.write(line + '\n')

    return len(intersection)

def deduplicate_file(input_path, output_path):
    """读取 input_path，去重后写入 output_path，并返回行数"""
    if not os.path.exists(input_path):
        # 如果文件不存在（可能是没有交集），创建一个空文件
        with open(output_path, 'w', encoding='utf-8') as f:
            pass
        return 0, 0

    unique_lines = set()
    total_lines_read = 0

    with open(input_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                total_lines_read += 1
                unique_lines.add(line)

    with open(output_path, 'w', encoding='utf-8') as f:
        for line in unique_lines:
            f.write(line + '\n')

    return total_lines_read, len(unique_lines)

def main():
    # 项目列表
    projects = [
        "basemall", "mall4cloud", "mogu", "netdisk", "novel",
        "onemall", "roncoo", "sduoj", "xmall", "youlai"
    ]

    base_b2_dir = "benchmark_b2"
    base_dyer_dir = "dyer"
    output_base_dir = "comparison_results"

    print(f"{'Project':<15} | {'Edges (Raw)':<12} | {'Edges (Set)':<12} | {'Methods (Raw)':<12} | {'Methods (Set)':<12}")
    print("-" * 75)

    for project in projects:
        # 1. 设置路径
        # Benchmark B2 路径
        b2_edges_path = os.path.join(base_b2_dir, project, "result", "call-edges.txt")
        b2_methods_path = os.path.join(base_b2_dir, project, "result", "reachable-methods.txt")

        # 输出路径
        project_out_dir = os.path.join(output_base_dir, project)
        os.makedirs(project_out_dir, exist_ok=True)

        final_edges_path = os.path.join(project_out_dir, "final-edges.txt")
        final_edges_set_path = os.path.join(project_out_dir, "final-edges-set.txt")
        final_methods_path = os.path.join(project_out_dir, "final-methods.txt")
        final_methods_set_path = os.path.join(project_out_dir, "final-methods-set.txt")

        # 清理旧的输出文件（如果存在），防止追加模式导致数据重复
        for p in [final_edges_path, final_methods_path]:
            if os.path.exists(p):
                os.remove(p)

        # 2. 读取 Benchmark B2 的基准数据
        b2_edges_set = read_lines_to_set(b2_edges_path)
        b2_methods_set = read_lines_to_set(b2_methods_path)

        # 3. 处理 Edges (call-edges)
        # 找到 dyer 下该项目所有的 calledges-*.txt
        dyer_edge_files = glob.glob(os.path.join(base_dyer_dir, project, "calledges-*.txt"))
        # 排序以保证处理顺序一致
        dyer_edge_files.sort()

        for d_file in dyer_edge_files:
            append_intersection_to_file(b2_edges_set, d_file, final_edges_path)

        # 4. 处理 Methods (reachable-methods)
        # 找到 dyer 下该项目所有的 methods-*.txt
        dyer_method_files = glob.glob(os.path.join(base_dyer_dir, project, "methods-*.txt"))
        dyer_method_files.sort()

        for d_file in dyer_method_files:
            append_intersection_to_file(b2_methods_set, d_file, final_methods_path)

        # 5. 去重并统计 (Generate -set files)
        # Edges
        edges_raw_count, edges_set_count = deduplicate_file(final_edges_path, final_edges_set_path)
        # Methods
        methods_raw_count, methods_set_count = deduplicate_file(final_methods_path, final_methods_set_path)

        # 6. 打印统计结果
        print(f"{project:<15} | {edges_raw_count:<12} | {edges_set_count:<12} | {methods_raw_count:<12} | {methods_set_count:<12}")

    print("-" * 75)
    print(f"Comparison complete. Detailed results are in the '{output_base_dir}' directory.")

if __name__ == "__main__":
    main()