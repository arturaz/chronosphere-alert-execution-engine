#!/usr/bin/env bash

docker run --name alerts_server -p 9001:9001 quay.io/chronosphereiotest/interview-alerts-engine:latest -d
