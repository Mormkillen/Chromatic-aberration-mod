#!/usr/bin/env bash
# Switch the build matrix entry: ./switch-version.sh <mc_tag>
# Writes gradle.properties for the requested Minecraft version.
set -e
cd "$(dirname "$0")"

MC="$1"
COMMON_HEADER='# Done to increase the memory available to gradle.
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true

loader_version=0.19.3
modmenu_version=7.2.2

# Mod Properties
mod_version=1.0.0
debug_version=4.0.0
maven_group=com.mormkillen
archives_base_name=speed-chromatic-aberration
mappings_kind=yarn'

case "$MC" in
  1.20.1)
    YARN='1.20.1+build.10'; API='0.92.2+1.20.1'; LOOM='1.6-SNAPSHOT'; JAVA='17'; JDK='17'; DEP='~1.20.1'; JDEP='>=17'; TAG='';;
  1.20.4)
    YARN='1.20.4+build.3'; API='0.97.3+1.20.4'; LOOM='1.6-SNAPSHOT'; JAVA='17'; JDK='17'; DEP='>=1.20.2 <1.20.5'; JDEP='>=17';;
  1.20.6)
    YARN='1.20.6+build.3'; API='0.100.8+1.20.6'; LOOM='1.6-SNAPSHOT'; JAVA='21'; JDK='21'; DEP='>=1.20.5 <1.20.7'; JDEP='>=21';;
  1.21.1)
    YARN='1.21.1+build.3'; API='0.116.15+1.21.1'; LOOM='1.6-SNAPSHOT'; JAVA='21'; JDK='21'; DEP='>=1.21 <1.21.2'; JDEP='>=21';;
  1.21.3)
    YARN='1.21.3+build.2'; API='0.114.1+1.21.3'; LOOM='1.15.5'; JAVA='21'; JDK='21'; DEP='>=1.21.2 <1.21.4'; JDEP='>=21';;
  1.21.4)
    YARN='1.21.4+build.8'; API='0.119.4+1.21.4'; LOOM='1.15.5'; JAVA='21'; JDK='21'; DEP='~1.21.4'; JDEP='>=21';;
  1.21.5)
    YARN='1.21.5+build.1'; API='0.128.2+1.21.5'; LOOM='1.15.5'; JAVA='21'; JDK='21'; DEP='~1.21.5'; JDEP='>=21';;
  1.21.8)
    YARN='1.21.8+build.1'; API='0.136.1+1.21.8'; LOOM='1.15.5'; JAVA='21'; JDK='21'; DEP='>=1.21.6 <1.21.9'; JDEP='>=21';;
  1.21.11)
    YARN='1.21.11+build.6'; API='0.141.6+1.21.11'; LOOM='1.15.5'; JAVA='21'; JDK='21'; DEP='>=1.21.9 <1.21.12'; JDEP='>=21';;
  26.1.2)
    YARN=''; API='0.155.2+26.1.2'; LOOM='1.15.5'; JAVA='25'; JDK='25'; DEP='>=26.1 <26.2'; JDEP='>=25'; MODMENU='21.0.0-alpha.1'; EXTRA='fabric.loom.disableObfuscation=true';;
  *)
    echo "unknown MC: $MC"; exit 1;;
esac

cat > gradle.properties <<EOF
$COMMON_HEADER

minecraft_version=$MC
yarn_mappings=$YARN
fabric_version=$API
loom_version=$LOOM
java_release=$JAVA
java_toolchain=$JDK
mc_tag=${TAG-$MC}
minecraft_dep=$DEP
java_dep=$JDEP
EOF

if [ -z "$YARN" ]; then
  echo 'mappings_kind=mojmap' >> gradle.properties
fi

if [ -n "$MODMENU" ]; then
  echo "modmenu_version=$MODMENU" >> gradle.properties
fi

if [ -n "$EXTRA" ]; then
  echo "$EXTRA" >> gradle.properties
fi

echo "switched to MC $MC (loom $LOOM, java $JAVA, dep $DEP)"
