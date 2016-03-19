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

# Insert base url
sed -i "s@BASEURL@$1@g" build/map-data/tilesource.json

# Generate application cache manifest
echo 'CACHE MANIFEST' > build/manifest.appcache
printf '# v' >> build/manifest.appcache
cat VERSION >> build/manifest.appcache
printf '# ' >> build/manifest.appcache
find build -type f -print0 | sort -z | xargs -0 sha1sum | sha1sum \
    | sed 's/  -//g' >> build/manifest.appcache
find build -type f | sort | sed 's/build\///g' | sed 's/manifest.appcache//g' \
    | sed '/^$/d' >> build/manifest.appcache
