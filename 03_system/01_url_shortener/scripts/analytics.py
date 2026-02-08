import sys
import re
from collections import Counter

def analyze_logs(log_file):
    # 패턴: ACCESS_LOG: {short_url} -> {long_url}
    pattern = re.compile(r"ACCESS_LOG:\s+(\w+)\s+->")
    counter = Counter()
    
    try:
        with open(log_file, 'r', encoding='utf-8') as f:
            for line in f:
                match = pattern.search(line)
                if match:
                    short_url = match.group(1)
                    counter[short_url] += 1
    except FileNotFoundError:
        print(f"Error: File '{log_file}' not found.")
        return

    print(f"Total Access Logs Found: {sum(counter.values())}")
    print("-" * 30)
    print(f"{'Short URL':<10} | {'Count':<5}")
    print("-" * 30)
    for short_url, count in counter.most_common(20):
        print(f"{short_url:<10} | {count:<5}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python analytics.py <log_file_path>")
        sys.exit(1)
    
    analyze_logs(sys.argv[1])
