#!/bin/sh

javac InstrumentGenerator.java

jar cfe instruments.jar InstrumentGenerator InstrumentGenerator.class