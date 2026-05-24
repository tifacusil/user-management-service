#!/bin/sh
# Maven Wrapper Script
# Automatically downloads Maven 3.9.6 if not cached

set -e

MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"
WRAPPER_HOME="$HOME/.m2/wrapper"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    DISTRIBUTION_URL=$(grep "^distributionUrl=" "$MAVEN_WRAPPER_PROPERTIES" | sed 's/distributionUrl=//')
else
    echo "ERROR: .mvn/wrapper/maven-wrapper.properties not found" >&2
    exit 1
fi

DISTRIBUTION_FILENAME=$(basename "$DISTRIBUTION_URL")
DISTRIBUTION_NAME=$(echo "$DISTRIBUTION_FILENAME" | sed 's/-bin\.zip//')
DISTRIBUTION_DIR="$WRAPPER_HOME/$DISTRIBUTION_NAME"

if [ ! -d "$DISTRIBUTION_DIR" ]; then
    mkdir -p "$WRAPPER_HOME"
    echo "Downloading Maven: $DISTRIBUTION_URL"
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL -o "$WRAPPER_HOME/$DISTRIBUTION_FILENAME" "$DISTRIBUTION_URL"
    elif command -v wget > /dev/null 2>&1; then
        wget -q -O "$WRAPPER_HOME/$DISTRIBUTION_FILENAME" "$DISTRIBUTION_URL"
    else
        echo "ERROR: Neither curl nor wget is available. Please install Maven manually." >&2
        exit 1
    fi
    unzip -q -d "$WRAPPER_HOME" "$WRAPPER_HOME/$DISTRIBUTION_FILENAME"
    rm -f "$WRAPPER_HOME/$DISTRIBUTION_FILENAME"
fi

MVNW_EXEC="$DISTRIBUTION_DIR/bin/mvn"

if [ ! -f "$MVNW_EXEC" ]; then
    echo "ERROR: Maven executable not found at: $MVNW_EXEC" >&2
    exit 1
fi

exec "$MVNW_EXEC" "$@"
