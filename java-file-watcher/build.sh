#!/usr/bin/env bash
set -e

source "$HOME/.sdkman/bin/sdkman-init.sh"

mvn clean verify
