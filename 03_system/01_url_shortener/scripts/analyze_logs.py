
import json
import argparse
import sys
from collections import defaultdict
import statistics

def parse_logs(log_file):
    request_data = defaultdict(lambda: {"events": [], "total_duration": 0, "status": None})
    
    try:
        with open(log_file, 'r', encoding='utf-8') as f:
            for line in f:
                try:
                    log_entry = json.loads(line)
                    req_id = log_entry.get("request_id")
                    
                    if not req_id:
                        continue
                        
                    event_type = log_entry.get("event")
                    
                    if event_type == "request_end":
                        request_data[req_id]["total_duration"] = log_entry.get("duration_ms", 0)
                        request_data[req_id]["status"] = log_entry.get("metadata", {}).get("status_code")
                    elif event_type == "request_failed":
                         request_data[req_id]["status"] = "ERROR"
                    elif event_type and event_type != "request_start":
                        # This is a timer event
                        request_data[req_id]["events"].append({
                            "name": event_type,
                            "duration": log_entry.get("duration_ms", 0)
                        })
                        
                except json.JSONDecodeError:
                    continue
    except FileNotFoundError:
        print(f"Error: Log file '{log_file}' not found.")
        sys.exit(1)

    return request_data

def analyze_performance(request_data):
    event_stats = defaultdict(list)
    total_requests = len(request_data)
    
    if total_requests == 0:
        print("No requests found in logs.")
        return

    # Collect durations for each event type
    for req_id, data in request_data.items():
        if data["total_duration"] > 0:
            event_stats["Total Request"].append(data["total_duration"])
            
        for event in data["events"]:
            event_stats[event["name"]].append(event["duration"])

    # Calculate statistics
    print(f"## Performance Analysis Report")
    print(f"**Total Requests Analyzed:** {total_requests}\n")
    
    print("| Operation | Count | Avg (ms) | P95 (ms) | Max (ms) | % of Total Time |")
    print("|---|---|---|---|---|---|")
    
    # Calculate total time across all requests to determine percentage
    grand_total_time = sum(event_stats["Total Request"]) if "Total Request" in event_stats else 1
    
    # Sort by average duration descending
    sorted_events = sorted(event_stats.items(), key=lambda x: statistics.mean(x[1]), reverse=True)
    
    for event_name, durations in sorted_events:
        count = len(durations)
        avg_dur = statistics.mean(durations)
        max_dur = max(durations)
        p95_dur = statistics.quantiles(durations, n=100)[94] if count > 1 else max_dur
        
        # Calculate percentage contribution to total request time logic
        # Ideally we compare sum(durations) / sum(total_request_durations)
        total_event_time = sum(durations)
        percentage = (total_event_time / grand_total_time) * 100
        
        print(f"| {event_name} | {count} | {avg_dur:.2f} | {p95_dur:.2f} | {max_dur:.2f} | {percentage:.1f}% |")

    # Bottleneck Analysis
    print("\n## Bottleneck Observations")
    db_events = [k for k in event_stats.keys() if "db" in k]
    redis_events = [k for k in event_stats.keys() if "redis" in k or "cache" in k]
    bloom_events = [k for k in event_stats.keys() if "bloom" in k]
    
    def sum_avg(events):
        return sum(statistics.mean(event_stats[e]) for e in events if e in event_stats)

    avg_db = sum_avg(db_events)
    avg_redis = sum_avg(redis_events)
    avg_bloom = sum_avg(bloom_events)
    
    print(f"- **Database Time (Avg):** {avg_db:.2f} ms")
    print(f"- **Redis/Cache Time (Avg):** {avg_redis:.2f} ms")
    print(f"- **Bloom Filter Time (Avg):** {avg_bloom:.2f} ms")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Analyze performance logs")
    parser.add_argument("logfile", help="Path to the log file (e.g., app.log)")
    args = parser.parse_args()
    
    data = parse_logs(args.logfile)
    analyze_performance(data)
