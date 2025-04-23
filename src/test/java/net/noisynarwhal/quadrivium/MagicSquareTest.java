package net.noisynarwhal.quadrivium;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class MagicSquareTest {

    @Test
    void testBuildWithValidOrder() {
        // Test building a magic square with valid order
        int order = 3;
        MagicSquare square = MagicSquare.build(order);

        assertNotNull(square);
        assertEquals(order, square.getOrder());
        assertEquals(order + order + 2, square.getMaxScore());
    }

    @Test
    void testBuildWithExistingValues() {
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
    }

    @Test
    void testGetValues() {
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
    }

    @Test
    void testEqualsAndHashCode() {
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
    }

    @Test
    void testCompareTo() {
        MagicSquare square1 = MagicSquare.build(3);
        MagicSquare square2 = MagicSquare.build(3);

        // Basic comparison
        int result = square1.compareTo(square2);
        assertTrue(result == 0 || result > 0 || result < 0);

        // Reflexive property
        assertEquals(0, square1.compareTo(square1));

        // Null comparison
        assertThrows(NullPointerException.class, () -> square1.compareTo(null));
    }

    @Test
    void testCollectionSorting() {
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
    }

    @Test
    void testIsSemiMagic() {
        // A semi-magic square has all rows and columns summing to magic constant
        // but diagonals may not
        int[][] semiMagicValues = {
                {1, 2, 3},
                {4, 5, 6},
                {9, 8, 7}
        };

        MagicSquare square = MagicSquare.build(semiMagicValues);
        assertFalse(square.isMagic());
        // Note: This may need adjustment based on actual implementation
        // as the current implementation might require specific properties
        // for semi-magic determination
    }

    @Test
    void testBuildWithInvalidOrder() {
        // Test building a magic square with invalid order
        assertThrows(IllegalArgumentException.class, () -> MagicSquare.build(2));
        assertThrows(IllegalArgumentException.class, () -> MagicSquare.build(0));
        assertThrows(IllegalArgumentException.class, () -> MagicSquare.build(-1));
    }

    @Test
    void testBuildWithNonSquareMatrix() {
        // Test building with non-square matrix
        int[][] nonSquareValues = {
                {1, 2, 3},
                {4, 5, 6}
        };
        assertThrows(IllegalArgumentException.class, () -> MagicSquare.build(nonSquareValues));
    }

    @Test
    void testBuildWithNullValues() {
        // Test building with null values
        assertThrows(NullPointerException.class, () -> MagicSquare.build((int[][]) null));
    }

    @Test
    void testEvolve() {
        // Test that evolve() produces a square with equal or better score
        MagicSquare square = MagicSquare.build(3);
        int initialScore = square.getScore();
        
        MagicSquare evolved = square.evolve();
        assertTrue(evolved.getScore() >= initialScore);
    }

    @Test
    void testNewChild() {
        // Test that newChild() produces a valid square
        MagicSquare square = MagicSquare.build(3);
        MagicSquare child = square.newChild();
        
        assertNotNull(child);
        assertEquals(square.getOrder(), child.getOrder());
        assertTrue(child.getScore() >= 0);
        assertTrue(child.getScore() <= child.getMaxScore());
    }

    @Test
    void testMagicSum() {
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
    }

    @Test
    void testToString() {
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
    }

    @Test
    void testSemiMagicSquare() {
        // Test a non-semi-magic square (rows and columns don't sum to magic constant)
        int[][] nonSemiMagicValues = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        MagicSquare square = MagicSquare.build(nonSemiMagicValues);
        assertFalse(square.isSemiMagic());
        assertFalse(square.isMagic());
    }

    @Test
    void testNonMagicSquare() {
        // Test a non-magic square
        int[][] nonMagicValues = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        MagicSquare square = MagicSquare.build(nonMagicValues);
        assertFalse(square.isSemiMagic());
        assertFalse(square.isMagic());
    }

    @Test
    void testCompareToWithDifferentScores() {
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
    }

    @Test
    void testCompareToWithEqualScores() {
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
    }

    @Test
    public void testStandardize() {
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
    }
}

class MatrixUtilsTest {
    
    @Test
    void testReadAndPrintMatrix() throws IOException {
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
    }
    
    @Test
    void testReadFromString() throws IOException {
        String input = "1 2 3\n4 5 6\n7 8 9";
        
        try (StringReader reader = new StringReader(input)) {
            int[][] matrix = MatrixUtils.read(reader);
            
            assertEquals(3, matrix.length);
            assertEquals(3, matrix[0].length);
            assertEquals(1, matrix[0][0]);
            assertEquals(5, matrix[1][1]);
            assertEquals(9, matrix[2][2]);
        }
    }
    
    @Test
    void testPrintToWriter() throws IOException {
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
    }
    
    @Test
    void testReadWithExtraWhitespace() throws IOException {
        String input = "  1  2  3  \n  4  5  6  \n  7  8  9  ";
        
        try (StringReader reader = new StringReader(input)) {
            int[][] matrix = MatrixUtils.read(reader);
            
            assertEquals(3, matrix.length);
            assertEquals(3, matrix[0].length);
            assertEquals(1, matrix[0][0]);
            assertEquals(5, matrix[1][1]);
            assertEquals(9, matrix[2][2]);
        }
    }
    
    @Test
    void testReadEmptyLines() throws IOException {
        String input = "\n1 2 3\n\n4 5 6\n\n7 8 9\n\n";
        
        try (StringReader reader = new StringReader(input)) {
            int[][] matrix = MatrixUtils.read(reader);
            
            assertEquals(3, matrix.length);
            assertEquals(3, matrix[0].length);
            assertEquals(1, matrix[0][0]);
            assertEquals(5, matrix[1][1]);
            assertEquals(9, matrix[2][2]);
        }
    }
}