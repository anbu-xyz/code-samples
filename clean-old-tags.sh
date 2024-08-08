#!/usr/bin/env bash
set -e

# Define the prefix and number of tags to keep
PREFIX='last-successful-build-'
KEEP=10

# Fetch the latest tags from the remote
if ! git fetch --tags; then
  echo "Failed to fetch tags"
  exit 1
fi

# List all remote tags with the specified prefix, sort them, and keep only the most recent ones
tags_to_keep=($(git tag -l "${PREFIX}*" | sort -V | tail -n $KEEP))

# List all remote tags with the specified prefix
all_tags=($(git tag -l "${PREFIX}*"))

# Initialize a flag to check if any tag is deleted
tag_deleted=false

# Delete the tags that are not in the list of tags to keep
for tag in "${all_tags[@]}"; do
  if [[ ! " ${tags_to_keep[*]} " =~ " $tag " ]]; then
    echo "Deleting tag: $tag"
    if ! git push origin --delete "$tag"; then
      echo "Failed to delete tag: $tag"
      exit 1
    fi
    git tag -d "$tag"
    tag_deleted=true
  fi
done

# Check if no tags were deleted
if [ "$tag_deleted" = false ]; then
  echo "No tag to delete this time"
fi
