# Spoofax Test Coverage Analysis

This tool allows to estimate the test coverage of an SPT test suite on a Statix specification. Part of the [supplementary material of my master thesis](https://github.com/jessetilro/thesis).

It consists of a three-stage pipeline, each invoked by its own script in `bin`. The first two stages can be run independently, whereas the third stage depends on the results of both the first two stages.

|Step   | Script |  Description |
|---|---|---|
| 1 | `bin/extract_tests` | From an SPT test suite, extract the abstract syntax trees of all fixtures |
| 2 | `bin/extract_constraints` | From a Statix spec, extract the term patterns in the heads of all constraint rules |
| 3 | `bin/cross_reference` | Compute the final test coverage estimation results based on the results of 1 and 2 |

The scripts are parameterized by the values in `config.yml`. If the values in the configuration file are left empty, and interactive prompt will ask for the values during execution.

The scripts are implemented in pure Ruby, but the `extract_tests` step depends on the Maven application in this repository as defined by `pom.xml`, having the Java source code in `src`. This application is responsible for extracting the full program fragments of test fixtures from an SPT test suite, and parsing them and performing pre-analysis transformations on the resulting abstract syntax trees. At Ruby script level, the ASTs serialized in the ATerm format are then loaded into memory as trees using the internal DSL defined in `lib`.

