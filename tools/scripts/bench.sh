#!/bin/bash

echo "Prepping bench for $1...";

echo "Warming up...";
hyperfine --warmup 1000 --runs 5000 'curl http://localhost:3000/hello' -i --shell=none -n http;

echo "Running benchmark...";
#k6 run --vus 192 --duration=2m ./tools/scripts/loadtest.js;
wrk -c 1024 -d $1 -t 16 --rate 10000000 --latency --timeout 30s http://localhost:3000/hello
