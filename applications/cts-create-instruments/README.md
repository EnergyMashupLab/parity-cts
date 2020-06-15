# Instrument Generator

This program generates instruments from a specified start date, interval, and number of days.

## Usage

```text
Usage: instruments.jar [-f filepath] -s | -c START_DAY INTERVAL_MINS NUM_DAYS
Generate instruments in parity system/client configuration format
Example: java -jar -f sys.conf -s 20200704 60 1

Options:
    -f      Path to output generated instruments. If the file in the
              path exists then it is appended to, otherwise it is created.
              If this option is not provided the output will be sent to stdout
    -s      Outputs instruments in parity-system configuration format
    -c      Outputs instruments in parity-client configuration format
```
