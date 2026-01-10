import os
import subprocess
import glob
import sys

def run_benchmarks():
    # --- 1. 配置基础信息 ---
    # 指定你生成好的 fat-jar 路径
    jar_path = "build/libs/WebAnalyzer-all.jar"

    # 检查 jar 包是否存在，避免无效运行
    if not os.path.exists(jar_path):
        print(f"❌ 错误: 找不到 Jar 包: {jar_path}")
        print("💡 请先运行: ./gradlew shadowJar")
        return

    # 定义配置文件的搜索路径
    base_search_path = os.path.join("configs", "benchmarks", "*", "options.yml")

    # 获取所有匹配的文件路径列表
    config_files = glob.glob(base_search_path)

    if not config_files:
        print(f"❌ 未在 '{base_search_path}' 下找到任何 options.yml 文件。")
        return

    # 排序
    config_files.sort()

    print(f"=== 🔍 发现 {len(config_files)} 个任务，准备开始执行 ===")
    print(f"📦 使用 Jar 包: {jar_path}")
    print("-" * 50)

    # --- 2. 遍历并执行 ---
    for index, config_path in enumerate(config_files, 1):
        # 自动计算 result 文件的路径：放在与 options.yml 同级的目录下
        # 例如: configs/benchmarks/basemall/options.yml -> configs/benchmarks/basemall/result
        work_dir = os.path.dirname(config_path)
        result_path = os.path.join(work_dir, "result")

        print(f"[{index}/{len(config_files)}] 🚀 正在处理: {work_dir.split('/')[-1]} ...", end=" ", flush=True)

        # 构建命令：严格按照你测试成功的命令结构
        # java -jar build/libs/WebAnalyzer-all.jar -o="configs/benchmarks/xxx/options.yml"
        command = [
            "java",
            "-jar", jar_path,
            f"-o={config_path}"
        ]

        try:
            # 打开 result 文件用于写入（相当于 Shell 中的 > result）
            with open(result_path, "w") as f_out:
                # subprocess.run 执行命令
                # stdout=f_out: 将标准输出写入文件
                # stderr=subprocess.STDOUT: 将错误日志也合并写入同一个文件（推荐，方便排查报错）
                subprocess.run(command, stdout=f_out, stderr=subprocess.STDOUT, check=True)

            print("✅ 成功 (结果已保存)")

        except subprocess.CalledProcessError as e:
            # 如果 Java 程序返回错误码（非0）
            print(f"❌ 失败 (代码 {e.returncode})")
            print(f"   👉 请查看日志: {result_path}")

        except FileNotFoundError:
            print("\n❌ 错误: 未找到 'java' 命令。")
            sys.exit(1)
        except Exception as e:
            print(f"\n❌ 发生未知错误: {e}")

    print("\n" + "="*50)
    print("=== 🎉 所有批量任务执行完毕 ===")

if __name__ == "__main__":
    run_benchmarks()