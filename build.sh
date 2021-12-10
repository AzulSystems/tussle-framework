#!/bin/bash

COMMON_BASE_DIR=$(cd $(dirname $0); pwd)
cd "${COMMON_BASE_DIR}" || exit 1
echo "Building $(pwd)..."

rm -v *.jar

mvn clean
mvn package -DskipTests || exit 1

from=$(find target -name *-jar-with-dependencies.jar)
to=${from/-jar-with-dependencies/}
to=${to##*/}

cp -fv ${from} ${to}
chmod 777 ${to}

echo "Installing built file '${to}' ..."
mvn install:install-file -Dfile=${to} -DpomFile=pom.xml
