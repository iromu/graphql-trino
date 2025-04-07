#!/usr/bin/env bash

if ! [ -x "$(command -v wget)" ]; then
  curl --fail --silent localhost:8080/actuator/health/liveness | grep UP || exit 1
else
  wget --no-verbose --tries=1 -qO - localhost:8080/actuator/health/liveness | grep UP || exit 1
fi
