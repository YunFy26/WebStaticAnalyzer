#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
分析 call-edges.txt 和 reachable-methods.txt 文件
统计 APP-APP、APP-LIB/JDK 调用边和 APP/LIB/JDK 方法数量
"""

import os
import re
from pathlib import Path

# 定义 JDK 包前缀
JDK_PREFIXES = [
    'java.', 'javax.', 'sun.', 'com.sun.', 'jdk.', 'org.xml.', 'org.w3c.'
]

def is_jdk_method(class_name: str) -> bool:
    """判断是否是 JDK 方法"""
    for prefix in JDK_PREFIXES:
        if class_name.startswith(prefix):
            return True
    return False

def get_class_name_from_method(method_sig: str) -> str:
    """从方法签名中提取类名
    格式: <class: return_type method(params)>
    """
    match = re.match(r'<([^:]+):', method_sig)
    if match:
        return match.group(1).strip()
    return ""

def categorize_method(method_sig: str, app_prefixes: set) -> str:
    """
    将方法分类为 APP, JDK 或 LIB
    """
    class_name = get_class_name_from_method(method_sig)
    if not class_name:
        return "UNKNOWN"
    
    # 检查是否是 JDK
    if is_jdk_method(class_name):
        return "JDK"
    
    # 检查是否是 APP（与 APP 前缀匹配）
    for prefix in app_prefixes:
        if class_name.startswith(prefix):
            return "APP"
    
    return "LIB"

def detect_app_prefixes(methods_file: str) -> set:
    """
    自动检测 APP 包前缀
    通过分析 reachable-methods.txt 中出现最多的非 JDK/常见 LIB 包前缀
    """
    prefixes = {}
    common_lib_prefixes = [
        'com.baomidou.', 'cn.binarywang.', 'org.apache.', 'org.springframework.',
        'com.alibaba.', 'cn.hutool.', 'org.slf4j.', 'com.fasterxml.', 'org.mybatis.',
        'io.netty.', 'com.google.', 'org.aspectj.', 'org.hibernate.', 'com.mysql.',
        'redis.', 'org.redisson.', 'io.swagger.', 'com.github.', 'org.yaml.',
        'com.zaxxer.', 'net.bytebuddy.', 'org.objectweb.', 'cglib.', 'org.aopalliance.',
        'com.netflix.', 'org.apache.commons.', 'lombok.', 'io.micrometer.'
    ]
    
    with open(methods_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            class_name = get_class_name_from_method(line)
            if not class_name or is_jdk_method(class_name):
                continue
            
            # 跳过常见 LIB 前缀
            is_lib = False
            for lib_prefix in common_lib_prefixes:
                if class_name.startswith(lib_prefix):
                    is_lib = True
                    break
            
            if not is_lib:
                # 提取二级包名作为潜在的 APP 前缀
                parts = class_name.split('.')
                if len(parts) >= 3:
                    prefix = '.'.join(parts[:3]) + '.'
                    prefixes[prefix] = prefixes.get(prefix, 0) + 1
    
    # 返回出现次数最多的前缀作为 APP 前缀
    if prefixes:
        max_count = max(prefixes.values())
        app_prefixes = {p for p, c in prefixes.items() if c >= max_count * 0.1}  # 至少10%的方法
        return app_prefixes
    return set()

def analyze_call_edges(file_path: str, app_prefixes: set) -> dict:
    """分析 call-edges.txt 文件"""
    results = {
        'APP-APP': 0,
        'APP-JDK': 0,
        'APP-LIB': 0,
        'LIB-APP': 0,
        'LIB-JDK': 0,
        'LIB-LIB': 0,
        'JDK-APP': 0,
        'JDK-LIB': 0,
        'JDK-JDK': 0,
        'total': 0
    }
    
    with open(file_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            
            # 解析格式: <caller-method>/<call-site-info>\t<callee-method>
            parts = line.split('\t')
            if len(parts) != 2:
                continue
            
            caller_part = parts[0]
            callee = parts[1]
            
            # 从 caller_part 提取 caller 方法签名
            # 格式: <class: return_type method(params)>/method-name/index
            caller_match = re.match(r'(<[^>]+>)', caller_part)
            if not caller_match:
                continue
            caller = caller_match.group(1)
            
            caller_type = categorize_method(caller, app_prefixes)
            callee_type = categorize_method(callee, app_prefixes)
            
            key = f'{caller_type}-{callee_type}'
            if key in results:
                results[key] += 1
            results['total'] += 1
    
    return results

def analyze_reachable_methods(file_path: str, app_prefixes: set) -> dict:
    """分析 reachable-methods.txt 文件"""
    results = {
        'APP': 0,
        'JDK': 0,
        'LIB': 0,
        'total': 0
    }
    
    with open(file_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            
            method_type = categorize_method(line, app_prefixes)
            if method_type in results:
                results[method_type] += 1
            results['total'] += 1
    
    return results

def analyze_directory(dir_path: str) -> dict:
    """分析单个目录"""
    call_edges_file = os.path.join(dir_path, 'call-edges.txt')
    reachable_methods_file = os.path.join(dir_path, 'reachable-methods.txt')
    
    if not os.path.exists(call_edges_file) or not os.path.exists(reachable_methods_file):
        return None
    
    # 自动检测 APP 前缀
    app_prefixes = detect_app_prefixes(reachable_methods_file)
    
    call_edges_results = analyze_call_edges(call_edges_file, app_prefixes)
    reachable_methods_results = analyze_reachable_methods(reachable_methods_file, app_prefixes)
    
    return {
        'app_prefixes': list(app_prefixes),
        'call_edges': call_edges_results,
        'reachable_methods': reachable_methods_results
    }

def main():
    base_dir = Path(__file__).parent
    
    # 获取所有子目录
    subdirs = [d for d in base_dir.iterdir() if d.is_dir()]
    subdirs.sort()
    
    print("=" * 100)
    print("静态分析结果统计")
    print("=" * 100)
    
    all_results = {}
    
    for subdir in subdirs:
        result = analyze_directory(str(subdir))
        if result:
            all_results[subdir.name] = result
            
            print(f"\n📁 {subdir.name}")
            print("-" * 80)
            print(f"  APP 包前缀: {', '.join(result['app_prefixes'])}")
            
            # 打印调用边统计
            ce = result['call_edges']
            print(f"\n  📊 调用边统计 (call-edges.txt):")
            print(f"     总调用边数: {ce['total']}")
            print(f"     APP → APP:  {ce['APP-APP']:>6} ({ce['APP-APP']/ce['total']*100 if ce['total'] else 0:.1f}%)")
            app_to_lib_jdk = ce['APP-LIB'] + ce['APP-JDK']
            print(f"     APP → LIB:  {ce['APP-LIB']:>6} ({ce['APP-LIB']/ce['total']*100 if ce['total'] else 0:.1f}%)")
            print(f"     APP → JDK:  {ce['APP-JDK']:>6} ({ce['APP-JDK']/ce['total']*100 if ce['total'] else 0:.1f}%)")
            print(f"     APP → LIB/JDK: {app_to_lib_jdk:>6} ({app_to_lib_jdk/ce['total']*100 if ce['total'] else 0:.1f}%)")
            
            # 打印可达方法统计
            rm = result['reachable_methods']
            print(f"\n  📊 可达方法统计 (reachable-methods.txt):")
            print(f"     总方法数: {rm['total']}")
            print(f"     APP 方法: {rm['APP']:>6} ({rm['APP']/rm['total']*100 if rm['total'] else 0:.1f}%)")
            print(f"     LIB 方法: {rm['LIB']:>6} ({rm['LIB']/rm['total']*100 if rm['total'] else 0:.1f}%)")
            print(f"     JDK 方法: {rm['JDK']:>6} ({rm['JDK']/rm['total']*100 if rm['total'] else 0:.1f}%)")
            lib_jdk = rm['LIB'] + rm['JDK']
            print(f"     LIB/JDK:  {lib_jdk:>6} ({lib_jdk/rm['total']*100 if rm['total'] else 0:.1f}%)")
    
    # 打印汇总表格
    print("\n" + "=" * 100)
    print("汇总表格")
    print("=" * 100)
    
    # 调用边汇总
    print("\n📊 调用边汇总:")
    print("-" * 100)
    print(f"{'项目':<15} {'总数':>10} {'APP-APP':>10} {'APP-LIB':>10} {'APP-JDK':>10} {'APP-LIB/JDK':>12}")
    print("-" * 100)
    
    for name, result in all_results.items():
        ce = result['call_edges']
        app_lib_jdk = ce['APP-LIB'] + ce['APP-JDK']
        print(f"{name:<15} {ce['total']:>10} {ce['APP-APP']:>10} {ce['APP-LIB']:>10} {ce['APP-JDK']:>10} {app_lib_jdk:>12}")
    
    # 可达方法汇总
    print("\n📊 可达方法汇总:")
    print("-" * 100)
    print(f"{'项目':<15} {'总数':>10} {'APP':>10} {'LIB':>10} {'JDK':>10} {'LIB/JDK':>12}")
    print("-" * 100)
    
    for name, result in all_results.items():
        rm = result['reachable_methods']
        lib_jdk = rm['LIB'] + rm['JDK']
        print(f"{name:<15} {rm['total']:>10} {rm['APP']:>10} {rm['LIB']:>10} {rm['JDK']:>10} {lib_jdk:>12}")

if __name__ == '__main__':
    main()
