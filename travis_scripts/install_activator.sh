#!/bin/bash

curl -L https://downloads.typesafe.com/typesafe-activator/$ACTIVATOR_VERSION/typesafe-activator-$ACTIVATOR_VERSION-minimal.zip > activator.zip
unzip -uo activator.zip
chmod +x ./activator-$ACTIVATOR_VERSION-minimal/bin/activator

mkdir ~/.activator
mv ./activator-$ACTIVATOR_VERSION-minimal/* ~/.activator/
