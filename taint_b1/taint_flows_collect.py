import re
import os

base_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "taint_b1")

order = ["MCMS", "My-Blog", "RuoYi", "VBlog", "jeesite5", "jshERP", "mall", "mall-admin", "mall-portal", "mall-search", "newbee-mall", "vhr"]

print(f"{'Project':<15} {'Sources':>10} {'Sinks':>10} {'Taint Flows':>12}")
print("-" * 50)

for project in order:
    log_path = os.path.join(base_dir, project, "result", "tai-e.log")
    if not os.path.exists(log_path):
        print(f"{project:<15} {'N/A':>10} {'N/A':>10} {'N/A':>12}")
        continue

    with open(log_path, "r") as f:
        content = f.read()

    m = re.search(r"Sources:\s+(\d+)\s+generated", content)
    sources = int(m.group(1)) if m else 0

    m = re.search(r"Sinks:\s+(\d+)\s+generated\s+\(risky\)", content)
    sinks = int(m.group(1)) if m else 0

    m = re.search(r"Detected\s+(\d+)\s+taint flow\(s\)", content)
    flows = int(m.group(1)) if m else 0

    print(f"{project:<15} {sources:>10} {sinks:>10} {flows:>12}")