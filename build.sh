#!/bin/bash

APPLICATION_JSON="src/nyx/resource/application.json"
IMAGE_NAME="nyx"

increment_minor_version() {
  local version=$1
  local major_version=${version%%.*}
  local minor_version=${version##*.}
  local new_minor_version=$((minor_version + 1))
  echo "${major_version}.${new_minor_version}"
}

increment_major_version() {
  local version=$1
  local major_version=${version%%.*}
  local new_major_version=$((major_version + 1))
  echo "${new_major_version}.0"
}

if [[ -f "$APPLICATION_JSON" ]]; then
  VERSION=$(grep '"version"' "$APPLICATION_JSON" | sed -E 's/.*"version":\s*"([^"]*)".*/\1/')
  if [[ -z "$VERSION" ]]; then
    echo "Error: VERSION not found in $APPLICATION_JSON"
    exit 1
  fi
else
  echo "Error: File $APPLICATION_JSON not found."
  exit 1
fi

if [[ "$1" == "--major" ]]; then
  NEW_VERSION=$(increment_major_version "$VERSION")
else
  NEW_VERSION=$(increment_minor_version "$VERSION")
fi

echo "building version $NEW_VERSION"

sed -i -E "s/\"version\":\s*\"[0-9]+\.[0-9]+\"/\"version\": \"$NEW_VERSION\"/" "$APPLICATION_JSON"

sed -i -E 's/"logLevel":\s*"[^"]*"/"logLevel": "WARN"/' "$APPLICATION_JSON"

git add -u
git commit -m "[increase version to $NEW_VERSION]"

read  -n 1 -p "build the jar and press any key to continue..."

echo "Building Docker image..."
docker build -t "$IMAGE_NAME:$NEW_VERSION" -t "$IMAGE_NAME:latest" .

if [[ $? -ne 0 ]]; then
  echo "Error: Failed to build Docker image."
  exit 1
fi

echo "Docker image tagged as $IMAGE_NAME:$NEW_VERSION and $IMAGE_NAME:latest"

echo "Build and tagging completed successfully."

sed -i -E 's/"logLevel":\s*"[^"]*"/"logLevel": "DEBUG"/' "$APPLICATION_JSON"

git add -u
git commit -m "[prepare for next development iteration]"