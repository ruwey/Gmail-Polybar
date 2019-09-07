#!/bin/sh
./gradlew installDist
ln -srf build/install/EchoArgs/bin/EchoArgs 
