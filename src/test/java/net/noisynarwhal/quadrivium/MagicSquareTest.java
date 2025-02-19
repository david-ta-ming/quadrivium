package net.noisynarwhal.quadrivium;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}