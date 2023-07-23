# NaBL2 Benchmark

__This is a work in progress. The tool is not actually measuring what it is supposed to measure yet.__

Run analysis on example program
```shell
mvn clean verify exec:java@run -Dargs="PATH_TO_LANGUAGE_PROJECT PATH_TO_EXAMPLE_PROGRAM"
```

Benchmark analysis on example program
```shell
mvn clean verify exec:exec@benchmark -Dbenchmark.args="-p languagePath=PATH_TO_LANGUAGE_PROJECT -p programPath=PATH_TO_EXAMPLE_PROGRAM"
```

mvn clean verify exec:exec@benchmark -Dbenchmark.args="-p languagePath=../thesis/develop -p programPath=../thesis-benchmark-dataset/accounting/accounting.dev"