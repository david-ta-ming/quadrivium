# Quadrivium

A Java library for finding magic squares using parallel evolutionary algorithms.

## Overview

Quadrivium is a high-performance tool for generating magic squares of arbitrary size. It uses a parallel evolutionary algorithm approach to efficiently search for valid magic square solutions.

A magic square is an n×n square matrix where:
- Contains all numbers from 1 to n²
- All rows sum to the same value
- All columns sum to the same value
- Both main diagonals sum to the same value
- The common sum is called the magic constant: n(n²+1)/2

## Features

- Generate magic squares of any order (n×n where n ≥ 3)
- Multi-threaded search using parallel workers
- Automatic thread count detection based on CPU cores
- Command-line interface with configurable parameters
- Progress logging with customizable verbosity

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Installation

Clone the repository and build using Maven:

```bash
git clone https://github.com/david-ta-ming/Quadrivium.git
cd Quadrivium
mvn clean package
```

This will create an executable JAR file in the `target` directory.

## Usage

Run the JAR file using Java:

```bash
java -jar target/quadrivium.jar [options]
```

### Command Line Options

- `-o, --order <n>`: Order of the magic square (default: 30)
- `-t, --threads <n>`: Number of threads to use (default: auto-detect)
- `-h, --help`: Show help message

### Examples

Generate a 11×11 magic square using 8 threads:
```bash
java -jar quadrivium.jar --order 11 --threads 8
```

Generate a 23×23 magic square using auto-detected thread count:
```bash
java -jar quadrivium.jar --order 23
```

## Algorithm

The implementation uses an evolutionary approach:

1. Start with random permutations of numbers 1 to n²
2. Score each square based on how many rows, columns, and diagonals sum to the magic constant
3. Evolve solutions through successive mutations:
    - For non-semi-magic squares: swap values to improve row/column sums
    - For semi-magic squares: permute rows/columns to improve diagonal sums
4. Use multiple threads to explore different evolutionary paths in parallel
5. Stop when a valid magic square is found

## Performance

Performance varies based on the order of the magic square:
- Small squares (n ≤ 5): typically found in seconds
- Medium squares (5 < n ≤ 15): typically found in minutes
- Large squares (n > 15): may take hours or more

The benchmark class `SearchBenchmark` provides detailed performance metrics.

## API Usage

The library can also be used programmatically:

```java
// Create a magic square of order 4
MagicSquare square = MagicSquare.build(4);

// Check if it's a valid magic square
boolean isMagic = square.isMagic();

// Get the underlying values
int[][] values = square.getValues();

// Search for a magic square using 4 threads
MagicSquare solution = MagicSquareWorker.search(4, 4);
```

## Testing

Run the test suite using Maven:

```bash
mvn test
```

Performance benchmarks can be run using:

```bash
mvn clean package
java -jar target/benchmarks.jar
```

## Logging

Logging is configured using Logback. The default configuration logs:
- INFO and above to console
- DEBUG and above for quadrivium package
- WARN and above for all other packages

Modify `src/main/resources/logback.xml` to adjust logging settings.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## References

- [Magic Squares on Wikipedia](https://en.wikipedia.org/wiki/Magic_square)
- [Frénicle standard form](https://en.wikipedia.org/wiki/Fr%C3%A9nicle_standard_form)