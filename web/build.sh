#!/bin/sh

# Check arguments
if [ "$#" -ne 1 ]; then
  echo "Usage: $0 BASEURL" >&2
  exit 1
fi

# Make build directory
rm -r build
mkdir -p build

# Copy files
cp -r icons build/
cp -r mapbox-gl-js build/
cp -rL map-data build/
cp *.html build/
cp manifest.json build/
cp style.css build/
cp -r about build/
cp stay-standalone.js build/

# Insert base url
sed -i "s@BASEURL@$1@g" build/map-data/tilesource.json
