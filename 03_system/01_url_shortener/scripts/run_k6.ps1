# Run K6 load test using Docker
# Mounts the current scripts directory to /scripts container path
docker run --rm -i -v ${PWD}:/scripts grafana/k6 run /scripts/load_test.js
