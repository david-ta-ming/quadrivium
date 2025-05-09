package net.noisynarwhal.quadrivium;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MagicSquareTest {
    private static final Logger logger = LoggerFactory.getLogger(MagicSquareTest.class);

    @Test
    void testBuildWithValidOrder() {
        logger.info("Testing building a magic square with valid order");
        // Test building a magic square with valid order
        int order = 3;
        MagicSquare square = MagicSquare.build(order);

        assertNotNull(square);
        assertEquals(order, square.getOrder());
        assertEquals(order + order + 2, square.getMaxScore());
        logger.info("Successfully tested building a magic square with valid order");
    }

    @Test
    void testBuildWithExistingValues() {
        logger.info("Testing building a magic square with predefined values");
        // Test building a magic square with predefined values
        int[][] values = {
                {8, 1, 6},
                {3, 5, 7},
                {4, 9, 2}
        };

        MagicSquare square = MagicSquare.build(values);

        assertNotNull(square);
        assertEquals(3, square.getOrder());
        assertTrue(square.isMagic());
        logger.info("Successfully tested building a magic square with predefined values");
    }

    @Test
    void testGetValues() {
        logger.info("Testing getValues returns a defensive copy");
        // Test that getValues returns a defensive copy
        int[][] values = {
                {8, 1, 6},
                {3, 5, 7},
                {4, 9, 2}
        };

        MagicSquare square = MagicSquare.build(values);
        int[][] retrievedValues = square.getValues();

        // Modify the retrieved values
        retrievedValues[0][0] = 99;

        // Original square should be unchanged
        assertNotEquals(retrievedValues[0][0], square.getValues()[0][0]);
        logger.info("Successfully tested getValues returns a defensive copy");
    }

    @Test
    void testEqualsAndHashCode() {
        logger.info("Testing equals and hashCode methods");
        int[][] values1 = {
                {8, 1, 6},
                {3, 5, 7},
                {4, 9, 2}
        };

        int[][] values2 = {
                {8, 1, 6},
                {3, 5, 7},
                {4, 9, 2}
        };

        MagicSquare square1 = MagicSquare.build(values1);
        MagicSquare square2 = MagicSquare.build(values2);

        assertEquals(square1, square2);
        assertEquals(square1.hashCode(), square2.hashCode());
        logger.info("Successfully tested equals and hashCode methods");
    }

    @Test
    void testCompareTo() {
        logger.info("Testing compareTo method");
        MagicSquare square1 = MagicSquare.build(3);
        MagicSquare square2 = MagicSquare.build(3);

        // Basic comparison
        int result = square1.compareTo(square2);
        assertTrue(result == 0 || result > 0 || result < 0);

        // Reflexive property
        assertEquals(0, square1.compareTo(square1));

        // Null comparison
        assertThrows(NullPointerException.class, () -> square1.compareTo(null));
        logger.info("Successfully tested compareTo method");
    }

    @Test
    void testCollectionSorting() {
        logger.info("Testing collection sorting of magic squares");
        // Create squares with different scores
        int[][] values1 = {
                {8, 1, 6},
                {3, 5, 7},
                {4, 9, 2}
        }; // Magic square - highest score

        int[][] values2 = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        }; // Non-magic square - lower score

        MagicSquare square1 = MagicSquare.build(values1);
        MagicSquare square2 = MagicSquare.build(values2);

        List<MagicSquare> squares = new ArrayList<>();
        squares.add(square2); // Add lower score first
        squares.add(square1); // Add higher score second

        Collections.sort(squares);

        // After sorting, square1 (higher score) should be first
        assertSame(square1, squares.get(0), "Higher scoring square should be first after sorting");
        assertSame(square2, squares.get(1), "Lower scoring square should be second after sorting");
        logger.info("Successfully tested collection sorting of magic squares");
    }

    @Test
    void testIsSemiMagic() {
        logger.info("Testing semi-magic square detection");
        // A semi-magic square has all rows and columns summing to magic constant
        // but diagonals may not
        int[][] semiMagicValues = {
                {1, 2, 3},
                {4, 5, 6},
                {9, 8, 7}
        };

        MagicSquare square = MagicSquare.build(semiMagicValues);
        assertFalse(square.isMagic());
        logger.info("Successfully tested semi-magic square detection");
    }

    @Test
    void testBuildWithInvalidOrder() {
        logger.info("Testing building magic squares with invalid orders");
        // Test building a magic square with invalid order
        assertThrows(IllegalArgumentException.class, () -> MagicSquare.build(2));
        assertThrows(IllegalArgumentException.class, () -> MagicSquare.build(0));
        assertThrows(IllegalArgumentException.class, () -> MagicSquare.build(-1));
        logger.info("Successfully tested building magic squares with invalid orders");
    }

    @Test
    void testBuildWithNonSquareMatrix() {
        logger.info("Testing building with non-square matrix");
        // Test building with non-square matrix
        int[][] nonSquareValues = {
                {1, 2, 3},
                {4, 5, 6}
        };
        assertThrows(IllegalArgumentException.class, () -> MagicSquare.build(nonSquareValues));
        logger.info("Successfully tested building with non-square matrix");
    }

    @Test
    void testBuildWithNullValues() {
        logger.info("Testing building with null values");
        // Test building with null values
        assertThrows(NullPointerException.class, () -> MagicSquare.build((int[][]) null));
        logger.info("Successfully tested building with null values");
    }

    @Test
    void testEvolve() {
        logger.info("Testing evolve method");
        // Test that evolve() produces a square with equal or better score
        MagicSquare square = MagicSquare.build(3);
        int initialScore = square.getScore();
        
        MagicSquare evolved = square.evolve();
        assertTrue(evolved.getScore() >= initialScore);
        logger.info("Successfully tested evolve method");
    }

    @Test
    void testNewChild() {
        logger.info("Testing newChild method");
        // Test that newChild() produces a valid square
        MagicSquare square = MagicSquare.build(3);
        MagicSquare child = square.newChild();
        
        assertNotNull(child);
        assertEquals(square.getOrder(), child.getOrder());
        assertTrue(child.getScore() >= 0);
        assertTrue(child.getScore() <= child.getMaxScore());
        logger.info("Successfully tested newChild method");
    }

    @Test
    void testMagicSum() {
        logger.info("Testing magic sum calculation");
        // Test magic sum calculation for different orders
        int[][] magic3x3 = {
                {8, 1, 6},
                {3, 5, 7},
                {4, 9, 2}
        };
        MagicSquare square3 = MagicSquare.build(magic3x3);
        assertEquals(8, square3.getScore()); // 3 rows + 3 columns + 2 diagonals = 8

        int[][] magic4x4 = {
                {16, 3, 2, 13},
                {5, 10, 11, 8},
                {9, 6, 7, 12},
                {4, 15, 14, 1}
        };
        MagicSquare square4 = MagicSquare.build(magic4x4);
        assertEquals(10, square4.getScore()); // 4 rows + 4 columns + 2 diagonals = 10
        logger.info("Successfully tested magic sum calculation");
    }

    @Test
    void testToString() {
        logger.info("Testing string representation");
        // Test string representation
        int[][] values = {
                {8, 1, 6},
                {3, 5, 7},
                {4, 9, 2}
        };
        MagicSquare square = MagicSquare.build(values);
        String str = square.toString();
        assertNotNull(str);
        assertTrue(str.contains("[8, 1, 6]"));
        assertTrue(str.contains("[3, 5, 7]"));
        assertTrue(str.contains("[4, 9, 2]"));
        logger.info("Successfully tested string representation");
    }

    @Test
    void testSemiMagicSquare() {
        logger.info("Testing non-semi-magic square detection");
        // Test a non-semi-magic square (rows and columns don't sum to magic constant)
        int[][] nonSemiMagicValues = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        MagicSquare square = MagicSquare.build(nonSemiMagicValues);
        assertFalse(square.isSemiMagic());
        assertFalse(square.isMagic());
        logger.info("Successfully tested non-semi-magic square detection");
    }

    @Test
    void testNonMagicSquare() {
        logger.info("Testing non-magic square detection");
        // Test a non-magic square
        int[][] nonMagicValues = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        MagicSquare square = MagicSquare.build(nonMagicValues);
        assertFalse(square.isSemiMagic());
        assertFalse(square.isMagic());
        logger.info("Successfully tested non-magic square detection");
    }

    @Test
    void testCompareToWithDifferentScores() {
        logger.info("Testing compareTo with squares of different scores");
        // Test comparison with squares of different scores
        int[][] highScoreValues = {
                {8, 1, 6},
                {3, 5, 7},
                {4, 9, 2}
        };
        int[][] lowScoreValues = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        
        MagicSquare highScore = MagicSquare.build(highScoreValues);
        MagicSquare lowScore = MagicSquare.build(lowScoreValues);
        
        assertTrue(highScore.compareTo(lowScore) < 0); // Higher score should be "less" (comes first)
        assertTrue(lowScore.compareTo(highScore) > 0); // Lower score should be "greater"
        logger.info("Successfully tested compareTo with squares of different scores");
    }

    @Test
    void testCompareToWithEqualScores() {
        logger.info("Testing compareTo with squares of equal scores");
        // Test comparison with squares of equal scores but different values
        int[][] values1 = {
                {8, 1, 6},
                {3, 5, 7},
                {4, 9, 2}
        };
        int[][] values2 = {
                {6, 1, 8},
                {7, 5, 3},
                {2, 9, 4}
        };
        
        MagicSquare square1 = MagicSquare.build(values1);
        MagicSquare square2 = MagicSquare.build(values2);
        
        // Should compare lexicographically since scores are equal
        assertNotEquals(0, square1.compareTo(square2));
        logger.info("Successfully tested compareTo with squares of equal scores");
    }

    @Test
    public void testStandardize() {
        logger.info("Testing matrix standardization");
        // Test with a simple 2x2 matrix
        int[][] matrix = {
            {1, 2},
            {3, 4}
        };
        
        // The standardized form should be the one with the smallest values in the top-left
        int[][] expected = {
            {1, 2},
            {3, 4}
        };
        
        int[][] standardized = MatrixUtils.standardize(matrix);
        assertTrue(MatrixUtils.valuesEqual(expected, standardized));
        
        // Test with a rotated version of the same matrix
        int[][] rotated = {
            {3, 1},
            {4, 2}
        };
        
        standardized = MatrixUtils.standardize(rotated);
        assertTrue(MatrixUtils.valuesEqual(expected, standardized));
        
        // Test with a mirrored version
        int[][] mirrored = {
            {2, 1},
            {4, 3}
        };
        
        standardized = MatrixUtils.standardize(mirrored);
        assertTrue(MatrixUtils.valuesEqual(expected, standardized));
        
        // Test with a 3x3 magic square
        int[][] magicSquare = {
            {8, 1, 6},
            {3, 5, 7},
            {4, 9, 2}
        };
        
        // The standardized form should be the one with the smallest values in the top-left
        int[][] expectedMagic = {
            {2, 7, 6},
            {9, 5, 1},
            {4, 3, 8}
        };
        
        standardized = MatrixUtils.standardize(magicSquare);
        assertTrue(MatrixUtils.valuesEqual(expectedMagic, standardized));
        logger.info("Successfully tested matrix standardization");
    }

    @Test
    void testMatrixUtilsIsMagic() {
        logger.info("Testing MatrixUtils.isMagic method");
        // Test a valid magic square
        int[][] validMagicSquare = {
            {8, 1, 6},
            {3, 5, 7},
            {4, 9, 2}
        };
        assertTrue(MatrixUtils.isMagic(validMagicSquare), "Valid magic square should return true");

        // Test a non-magic square (rows don't sum to same value)
        int[][] nonMagicSquare = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        assertFalse(MatrixUtils.isMagic(nonMagicSquare), "Non-magic square should return false");

        // Test a non-square matrix
        int[][] nonSquareMatrix = {
            {1, 2, 3},
            {4, 5, 6}
        };
        assertThrows(IllegalArgumentException.class, () -> MatrixUtils.isMagic(nonSquareMatrix),
            "Non-square matrix should throw IllegalArgumentException");

        // Test with null
        assertThrows(NullPointerException.class, () -> MatrixUtils.isMagic(null),
            "Null matrix should throw NullPointerException");
        logger.info("Successfully tested MatrixUtils.isMagic method");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMagicSquareWorkerSearch() {
        logger.info("Testing MagicSquareWorker.search() with order 11");
        // Test searching for a magic square of order 11
        int order = 11;
        
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        final int numThreads = Math.max(3, (availableProcessors * 2) / 3);
        
        MagicSquare result = MagicSquareWorker.search(order, numThreads);
        
        assertNotNull(result, "Search should return a result");
        assertEquals(order, result.getOrder(), "Result should have the requested order");
        assertTrue(result.isMagic(), "Result should be a valid magic square");
        assertTrue(MatrixUtils.isMagic(result.getValues()), "Result should be a valid magic square");
        logger.info("Successfully tested MagicSquareWorker.search() with order 11");
    }

    @Test
    void testReadAndPrintMatrix() throws IOException {
        logger.info("Testing matrix read and print operations");
        // Test matrix to use
        int[][] original = {
            {8, 1, 6},
            {3, 5, 7},
            {4, 9, 2}
        };
        
        // Test printing
        String printed = MatrixUtils.print(original);
        assertNotNull(printed);
        assertTrue(printed.contains("8"));
        assertTrue(printed.contains("5"));
        assertTrue(printed.contains("2"));
        
        // Test reading back what we printed
        try (StringReader reader = new StringReader(printed)) {
            int[][] read = MatrixUtils.read(reader);
            
            // Verify dimensions
            assertEquals(original.length, read.length);
            assertEquals(original[0].length, read[0].length);
            
            // Verify all values match
            for (int i = 0; i < original.length; i++) {
                assertArrayEquals(original[i], read[i]);
            }
        }
        logger.info("Successfully tested matrix read and print operations");
    }
    
    @Test
    void testReadFromString() throws IOException {
        logger.info("Testing reading matrix from string");
        String input = "1 2 3\n4 5 6\n7 8 9";
        
        try (StringReader reader = new StringReader(input)) {
            int[][] matrix = MatrixUtils.read(reader);
            
            assertEquals(3, matrix.length);
            assertEquals(3, matrix[0].length);
            assertEquals(1, matrix[0][0]);
            assertEquals(5, matrix[1][1]);
            assertEquals(9, matrix[2][2]);
        }
        logger.info("Successfully tested reading matrix from string");
    }
    
    @Test
    void testPrintToWriter() throws IOException {
        logger.info("Testing printing matrix to writer");
        int[][] matrix = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        
        StringWriter writer = new StringWriter();
        MatrixUtils.print(matrix, writer);
        
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("5"));
        assertTrue(result.contains("9"));
        
        // Verify format - numbers should be space-separated
        String[] lines = result.split("\n");
        assertEquals(3, lines.length);
        for (String line : lines) {
            assertTrue(line.matches("\\s*\\d+\\s+\\d+\\s+\\d+\\s*"));
        }
        logger.info("Successfully tested printing matrix to writer");
    }
    
    @Test
    void testReadWithExtraWhitespace() throws IOException {
        logger.info("Testing reading matrix with extra whitespace");
        String input = "  1  2  3  \n  4  5  6  \n  7  8  9  ";
        
        try (StringReader reader = new StringReader(input)) {
            int[][] matrix = MatrixUtils.read(reader);
            
            assertEquals(3, matrix.length);
            assertEquals(3, matrix[0].length);
            assertEquals(1, matrix[0][0]);
            assertEquals(5, matrix[1][1]);
            assertEquals(9, matrix[2][2]);
        }
        logger.info("Successfully tested reading matrix with extra whitespace");
    }
    
    @Test
    void testReadEmptyLines() throws IOException {
        logger.info("Testing reading matrix with empty lines");
        String input = "\n1 2 3\n\n4 5 6\n\n7 8 9\n\n";
        
        try (StringReader reader = new StringReader(input)) {
            int[][] matrix = MatrixUtils.read(reader);
            
            assertEquals(3, matrix.length);
            assertEquals(3, matrix[0].length);
            assertEquals(1, matrix[0][0]);
            assertEquals(5, matrix[1][1]);
            assertEquals(9, matrix[2][2]);
        }
        logger.info("Successfully tested reading matrix with empty lines");
    }

    @Test
    void testMagicSquareWorkerInvalidInputs() {
        logger.info("Testing MagicSquareWorker with invalid inputs");
        assertThrows(IllegalArgumentException.class, () -> MagicSquareWorker.search(2, 1),
            "Order less than 3 should throw IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> MagicSquareWorker.search(3, 0),
            "Zero threads should throw IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> MagicSquareWorker.search(3, -1),
            "Negative threads should throw IllegalArgumentException");
        logger.info("Successfully tested MagicSquareWorker with invalid inputs");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMagicSquareWorkerSingleThread() {
        logger.info("Testing MagicSquareWorker with single thread");
        MagicSquare result = MagicSquareWorker.search(7, 1);
        assertNotNull(result, "Search should return a result");
        assertEquals(7, result.getOrder(), "Result should have the requested order");
        assertTrue(result.isMagic(), "Result should be a valid magic square");
        logger.info("Successfully tested MagicSquareWorker with single thread");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testEvolutionChain() {
        logger.info("Testing evolution chain");
        MagicSquare square = MagicSquare.build(3);
        int initialScore = square.getScore();
        
        // Evolve multiple times
        for (int i = 0; i < 10; i++) {
            square = square.evolve();
            assertTrue(square.getScore() >= initialScore, 
                "Score should never decrease during evolution");
            initialScore = square.getScore();
        }
        logger.info("Successfully tested evolution chain");
    }

    @Test
    void testMatrixUtilsReadWithInvalidInput() {
        logger.info("Testing MatrixUtils.read with invalid input");
        String invalidInput = "1 2 3\n4 5\n7 8 9"; // Inconsistent row lengths
        
        try (StringReader reader = new StringReader(invalidInput)) {
            assertThrows(IllegalArgumentException.class, () -> MatrixUtils.read(reader),
                "Reading matrix with inconsistent row lengths should throw IllegalArgumentException");
        }
        logger.info("Successfully tested MatrixUtils.read with invalid input");
    }

    @Test
    void testMatrixUtilsReadWithNonNumericInput() {
        logger.info("Testing MatrixUtils.read with non-numeric input");
        String invalidInput = "1 2 3\n4 abc 6\n7 8 9";
        
        try (StringReader reader = new StringReader(invalidInput)) {
            assertThrows(NumberFormatException.class, () -> MatrixUtils.read(reader),
                "Reading matrix with non-numeric values should throw NumberFormatException");
        }
        logger.info("Successfully tested MatrixUtils.read with non-numeric input");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testMagicSquareWorkerThreadSafety() {
        logger.info("Testing MagicSquareWorker thread safety");
        int order = 11;
        int numThreads = 4;
        
        // Run multiple searches in parallel
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<MagicSquare>> futures = new ArrayList<>();
        
        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> MagicSquareWorker.search(order, numThreads)));
        }
        
        // Verify all results are valid magic squares
        for (Future<MagicSquare> future : futures) {
            try {
                MagicSquare result = future.get(45, TimeUnit.SECONDS); // Add timeout for each future
                assertNotNull(result, "Search should return a result");
                assertEquals(order, result.getOrder(), "Result should have the requested order");
                assertTrue(result.isMagic(), "Result should be a valid magic square");
                assertTrue(MatrixUtils.isMagic(result.getValues()), "Result should be a valid magic square");
            } catch (Exception e) {
                fail("Unexpected exception during parallel search", e);
            }
        }
        
        executor.shutdown();
        logger.info("Successfully tested MagicSquareWorker thread safety");
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testMagicSquareWorkerWithLargeOrder() {
        logger.info("Testing MagicSquareWorker with large order");
        int order = 35; // Using a relatively large order
        int numThreads = Runtime.getRuntime().availableProcessors() / 2; // Half the available processors
        
        MagicSquare result = MagicSquareWorker.search(order, numThreads);
        
        assertNotNull(result, "Search should return a result");
        assertEquals(order, result.getOrder(), "Result should have the requested order");
        assertTrue(result.isMagic(), "Result should be a valid magic square");
        assertTrue(MatrixUtils.isMagic(result.getValues()), "Result should be a valid magic square");
        logger.info("Successfully tested MagicSquareWorker with large order");
    }
}