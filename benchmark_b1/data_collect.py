import os
import re
import pandas as pd

# ================= 配置区域 =================
# 文件夹名称 -> 对应的数学符号含义
# entry_options        -> Entry (基准)
# entry_di_options     -> DI
# entry_di_aop_options -> AOP
STAGE_MAPPING = {
    "entry_options": "Entry",
    "entry_di_options": "DI",
    "entry_di_aop_options": "AOP"
}

# 处理顺序
STAGES = ["entry_options", "entry_di_options", "entry_di_aop_options"]

def parse_log_file(file_path):
    """
    解析 console_run.log，提取 insens 和 sens 的方法数与边数
    """
    result = {
        'm_insens': 0, 'm_sens': 0,
        'e_insens': 0, 'e_sens': 0
    }

    if not os.path.exists(file_path):
        return result

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

            # 提取 #reachable methods
            # 格式: #reachable methods: 14 (insens) / 14 (sens)
            m_match = re.search(r"#reachable methods:\s+([\d,]+)\s+\(insens\)\s+/\s+([\d,]+)\s+\(sens\)", content)
            if m_match:
                result['m_insens'] = int(m_match.group(1).replace(',', ''))
                result['m_sens'] = int(m_match.group(2).replace(',', ''))

            # 提取 #call graph edges
            # 格式: #call graph edges: 3 (insens) / 3 (sens)
            e_match = re.search(r"#call graph edges:\s+([\d,]+)\s+\(insens\)\s+/\s+([\d,]+)\s+\(sens\)", content)
            if e_match:
                result['e_insens'] = int(e_match.group(1).replace(',', ''))
                result['e_sens'] = int(e_match.group(2).replace(',', ''))

    except Exception as e:
        print(f"  [Warn] 解析日志失败 {file_path}: {e}")

    return result

def count_aop_edges(file_path):
    """
    计算 aop-call-edges.txt 的有效行数
    """
    count = 0
    if os.path.exists(file_path):
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                # 过滤空行
                lines = [line.strip() for line in f if line.strip()]
                count = len(lines)
        except Exception as e:
            print(f"  [Warn] 读取 AOP 文件失败 {file_path}: {e}")
    return count

def calculate_growth(current, previous, baseline):
    """
    通用增长率计算公式: (Current - Previous) / Baseline * 100
    """
    if baseline == 0:
        return 0.0
    return ((current - previous) / baseline) * 100

def main():
    base_dir = os.getcwd()

    # 获取所有项目文件夹
    projects = [d for d in os.listdir(base_dir)
               if os.path.isdir(os.path.join(base_dir, d)) and not d.startswith('.')]
    projects.sort()

    print(f"检测到 {len(projects)} 个项目，开始执行提取与计算...")

    all_rows = []

    for project in projects:
        project_path = os.path.join(base_dir, project)
        result_base = os.path.join(project_path, "result")

        # 跳过没有 result 文件夹的项目
        if not os.path.exists(result_base):
            continue

        print(f"正在处理: {project}")

        # 临时存储各阶段数据
        # 结构: data['Entry']['m_insens'], data['Entry']['e_sens'] ...
        stage_data = {}

        # 1. 遍历三个阶段提取数据
        for folder in STAGES:
            stage_name = STAGE_MAPPING[folder]
            log_path = os.path.join(result_base, folder, "console_run.log")

            # 解析 Log
            extracted = parse_log_file(log_path)

            # === AOP 特殊逻辑 ===
            # E_AOP = Log中的边 + aop-call-edges.txt行数
            if stage_name == "AOP":
                aop_txt_path = os.path.join(result_base, folder, "aop-call-edges.txt")
                aop_extra_count = count_aop_edges(aop_txt_path)

                # 将额外边数加到 Insens 和 Sens 的边数中
                extracted['e_insens'] += aop_extra_count
                extracted['e_sens'] += aop_extra_count
                # 记录一下以备查验
                extracted['aop_txt_count'] = aop_extra_count

            stage_data[stage_name] = extracted

        # 2. 准备数据行
        row = {'Project': project}

        # 提取变量以便计算，使用 0 作为默认值防止 Key Error
        def get_val(stage, key):
            return stage_data.get(stage, {}).get(key, 0)

        # --- 变量提取 (Insens) ---
        M_Entry_In = get_val('Entry', 'm_insens')
        M_DI_In    = get_val('DI', 'm_insens')
        M_AOP_In   = get_val('AOP', 'm_insens')

        E_Entry_In = get_val('Entry', 'e_insens')
        E_DI_In    = get_val('DI', 'e_insens')
        E_AOP_In   = get_val('AOP', 'e_insens')

        # --- 变量提取 (Sens) ---
        M_Entry_Se = get_val('Entry', 'm_sens')
        M_DI_Se    = get_val('DI', 'm_sens')
        M_AOP_Se   = get_val('AOP', 'm_sens')

        E_Entry_Se = get_val('Entry', 'e_sens')
        E_DI_Se    = get_val('DI', 'e_sens')
        E_AOP_Se   = get_val('AOP', 'e_sens')

        # 3. 填充 Excel 行数据 (原始数据)
        # Insens Raw
        row['M_Entry (Insens)'] = M_Entry_In
        row['M_DI (Insens)']    = M_DI_In
        row['M_AOP (Insens)']   = M_AOP_In
        row['E_Entry (Insens)'] = E_Entry_In
        row['E_DI (Insens)']    = E_DI_In
        row['E_AOP (Insens)']   = E_AOP_In

        # Sens Raw
        row['M_Entry (Sens)'] = M_Entry_Se
        row['M_DI (Sens)']    = M_DI_Se
        row['M_AOP (Sens)']   = M_AOP_Se
        row['E_Entry (Sens)'] = E_Entry_Se
        row['E_DI (Sens)']    = E_DI_Se
        row['E_AOP (Sens)']   = E_AOP_Se

        # 4. 计算指标 (Percentage Metrics)

        # === Insens Metrics ===
        # P^m (可达方法)
        row['P^m_DI (Insens %)']  = calculate_growth(M_DI_In, M_Entry_In, M_Entry_In)
        row['P^m_AOP (Insens %)'] = calculate_growth(M_AOP_In, M_DI_In, M_Entry_In)

        # P^e (调用边)
        row['P^e_DI (Insens %)']  = calculate_growth(E_DI_In, E_Entry_In, E_Entry_In)
        row['P^e_AOP (Insens %)'] = calculate_growth(E_AOP_In, E_DI_In, E_Entry_In)

        # === Sens Metrics ===
        # P^m (可达方法)
        row['P^m_DI (Sens %)']  = calculate_growth(M_DI_Se, M_Entry_Se, M_Entry_Se)
        row['P^m_AOP (Sens %)'] = calculate_growth(M_AOP_Se, M_DI_Se, M_Entry_Se)

        # P^e (调用边)
        row['P^e_DI (Sens %)']  = calculate_growth(E_DI_Se, E_Entry_Se, E_Entry_Se)
        row['P^e_AOP (Sens %)'] = calculate_growth(E_AOP_Se, E_DI_Se, E_Entry_Se)

        all_rows.append(row)

    # 5. 生成 Excel
    if not all_rows:
        print("未提取到数据。")
        return

    df = pd.DataFrame(all_rows)

    # 排列列顺序：项目 -> Insens原始 -> Insens指标 -> Sens原始 -> Sens指标
    cols = ['Project']

    # Insens Block
    cols += ['M_Entry (Insens)', 'M_DI (Insens)', 'M_AOP (Insens)',
             'P^m_DI (Insens %)', 'P^m_AOP (Insens %)']
    cols += ['E_Entry (Insens)', 'E_DI (Insens)', 'E_AOP (Insens)',
             'P^e_DI (Insens %)', 'P^e_AOP (Insens %)']

    # Sens Block
    cols += ['M_Entry (Sens)', 'M_DI (Sens)', 'M_AOP (Sens)',
             'P^m_DI (Sens %)', 'P^m_AOP (Sens %)']
    cols += ['E_Entry (Sens)', 'E_DI (Sens)', 'E_AOP (Sens)',
             'P^e_DI (Sens %)', 'P^e_AOP (Sens %)']

    # 过滤不存在的列并重排
    final_cols = [c for c in cols if c in df.columns]
    df = df[final_cols]

    output_file = "New_Metrics_Analysis.xlsx"
    df.to_excel(output_file, index=False)
    print(f"\n成功！结果已保存至: {output_file}")
    print("注意: E_AOP 的值已包含 aop-call-edges.txt 的行数。")

if __name__ == "__main__":
    main()