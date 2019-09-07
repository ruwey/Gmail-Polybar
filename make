#!/bin/sh
./gradlew installDist
ln -sr build/install/EchoArgs/bin/EchoArgs 
