#!/bin/bash

echo "Warming up...";
hyperfine --warmup 500 --runs 1000 'curl http://localhost:3000/hello' -i --shell=none -n http;


echo "\n\nRunning benchmark...";
k6 run --vus 192 --duration=2m ./tools/scripts/loadtest.js;
