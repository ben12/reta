#!/bin/sh
BASEDIR=$(dirname $(readlink -f $0))
java -cp "${BASEDIR}/libs/*" com.ben12.reta.Launcher $1
