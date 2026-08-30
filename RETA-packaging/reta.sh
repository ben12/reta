#!/bin/sh
BASEDIR=$(cd "$(dirname "$0")" && pwd)

CLASSPATH="${BASEDIR}/libs/*"

if [ -n "$COMSPEC" ] || echo "$OSTYPE" | grep -q "msys\|cygwin"; then
    # Convertit le chemin Linux (/c/dir) en chemin Windows (C:\dir)
    WIN_BASEDIR=$(cygpath -w "$BASEDIR")
    # Sous Windows, la JVM exige l'antislash avant l'étoile
    CLASSPATH="${WIN_BASEDIR}\libs\*"
fi

java -cp "$CLASSPATH" com.ben12.reta.Launcher "$1"
