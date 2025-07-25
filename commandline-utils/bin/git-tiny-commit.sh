#!/bin/bash

set -e  # Exit on any error

if [ $# -eq 0 ]; then
    echo "Usage: git tiny-commit \"commit message\""
    echo "Creates or updates a companion branch with tiny commits"
    exit 1
fi

COMMIT_MESSAGE="$*"

CURRENT_BRANCH=$(git branch --show-current)
if [ -z "$CURRENT_BRANCH" ]; then
    echo "Error: Not on a branch (detached HEAD?)"
    exit 1
fi

git add -A

COMPANION_BRANCH="${CURRENT_BRANCH}-tc"

echo "Current branch: $CURRENT_BRANCH"
echo "Companion branch: $COMPANION_BRANCH"

# Check if there are any changes to commit
if git diff --quiet && git diff --cached --quiet; then
    echo "No changes to commit"
    exit 0
fi

TEMP_COMMIT_NAME="temp-commit-$(date +%s)"
git add -A
git commit -m "$TEMP_COMMIT_NAME"
CURRENT_COMMIT=$(git rev-parse HEAD)

# Function to restore state and exit
restore_and_exit() {
    local exit_code=$1
    git checkout "$CURRENT_BRANCH" 2>/dev/null || true
    git reset --soft HEAD~1 2>/dev/null || true
    exit $exit_code
}

# Trap to ensure we always restore state on script exit
trap 'restore_and_exit 1' ERR INT TERM

# Check if companion branch exists
if git show-ref --verify --quiet "refs/heads/$COMPANION_BRANCH"; then
    echo "Companion branch exists, updating it..."

    COMPANION_BASE=$(git rev-parse "$COMPANION_BRANCH")
    ## remove any non-file-name characters from the branch name
    PATCH_FILE_NAME="${COMPANION_BASE//[^a-zA-Z0-9]/}-$(date +%s).patch"
    git diff "$COMPANION_BASE..$CURRENT_COMMIT" > /tmp/${PATCH_FILE_NAME}
    git checkout "$COMPANION_BRANCH"

    if [ -s /tmp/${PATCH_FILE_NAME} ]; then
        git apply --index /tmp/${PATCH_FILE_NAME} 2>/dev/null || git apply /tmp/${PATCH_FILE_NAME}
    fi
    rm -f /tmp/${PATCH_FILE_NAME}

    git commit -m "$COMMIT_MESSAGE"
    echo "✓ Updated companion branch with new commit"
else
    echo "Creating new companion branch..."
    git checkout -b "$COMPANION_BRANCH"
    git reset --soft HEAD~1
    git add -A
    git commit -m "$COMMIT_MESSAGE"
    git checkout "$CURRENT_BRANCH"
    echo "✓ Created companion branch with initial commit"
fi

git checkout "$CURRENT_BRANCH"
git reset --soft HEAD~1

echo "------------------------"
echo "✓ Tiny commit completed!"
echo "------------------------"
echo " "
git log ${CURRENT_BRANCH}..${COMPANION_BRANCH} --oneline