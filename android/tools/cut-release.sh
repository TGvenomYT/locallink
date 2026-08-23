#!/usr/bin/env bash
# Bumps the app version and pushes a release tag. GitHub Actions
# (.github/workflows/release.yml) picks up the tag, builds the signed
# release APKs, and publishes them to GitHub Releases.
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="${1:?Usage: tools/cut-release.sh <versionName, e.g. 1.0.3>}"

if [ -n "$(git status --porcelain)" ]; then
    echo "Working tree isn't clean. Commit or stash pending changes first." >&2
    exit 1
fi

CURRENT_CODE=$(grep -oE 'versionCode = [0-9]+' app/build.gradle.kts | grep -oE '[0-9]+')
NEW_CODE=$((CURRENT_CODE + 1))

sed -i.bak -E "s/versionCode = [0-9]+/versionCode = $NEW_CODE/" app/build.gradle.kts
sed -i.bak -E "s/versionName = \"[^\"]+\"/versionName = \"$VERSION\"/" app/build.gradle.kts
rm -f app/build.gradle.kts.bak

git add app/build.gradle.kts
git commit -m "Bump version to $VERSION ($NEW_CODE)"
git push origin main

TAG="v$VERSION"
git tag "$TAG"
git push origin "$TAG"

echo
echo "Pushed $TAG — GitHub Actions will build, sign, and publish the release."
echo "Watch it at: https://github.com/TGvenomYT/locallink/actions"
