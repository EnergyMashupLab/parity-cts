#!/bin/sh

javac ConfigurationGenerator.java

jar cfe config-gen.jar ConfigurationGenerator ConfigurationGenerator.class