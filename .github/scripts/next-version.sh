#!/usr/bin/env bash
# Prints the version that follows <current> for the given kind of release.
#   next-version.sh major 2.7.5  ->  3.0.0
#   next-version.sh minor 2.7.5  ->  2.8.0
#   next-version.sh hotfix 2.7.5 ->  2.7.6
set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "usage: $0 <major|minor|hotfix> <current-version>" >&2
    exit 2
fi

type=$1
current=$2

if [[ ! $current =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "not a X.Y.Z version: '$current'" >&2
    exit 2
fi
major=${BASH_REMATCH[1]}
minor=${BASH_REMATCH[2]}
patch=${BASH_REMATCH[3]}

case $type in
    major) echo "$((major + 1)).0.0" ;;
    minor) echo "$major.$((minor + 1)).0" ;;
    hotfix) echo "$major.$minor.$((patch + 1))" ;;
    *)
        echo "unknown release type: '$type'" >&2
        exit 2
        ;;
esac
