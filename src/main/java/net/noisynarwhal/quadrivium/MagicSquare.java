package net.noisynarwhal.quadrivium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An NxN square matrix consisting of sequential natural numbers from 1 to N*N.
 * This is the representation of a magic square's evolution.
 * <p>
 * The fitness of an instance is measured by its score. The score is the number
 * of rows, columns, and diagonals whose sum values equal the magic sum
 * {@code (n * (n^2 + 1) / 2)}. An instance is a magic square when this score
 * reaches its maximum possible score {@code n^2 + 2}.
 *
 * @author lioudt
 */
public class MagicSquare implements Comparable<MagicSquare> {
    private static final Logger logger = LoggerFactory.getLogger(MagicSquare.class);
    /** Thread-local random number generator for thread safety */
    private final Random RANDOM = ThreadLocalRandom.current();
    /** The magic constant that all rows, columns, and diagonals should sum to */
    private final int magicSum;
    /** The order (size) of the magic square */
    private final int order;
    /** The actual values in the square, stored as a 2D array */
    private final int[][] values;
    /** Current score: number of rows, columns, and diagonals that sum to magicSum */
    private final int score;
    /** Maximum possible score: all rows, columns, and both diagonals sum to magicSum */
    private final int maxScore;
    /** Whether this square is semi-magic (all rows and columns sum to magicSum) */
    private final boolean isSemiMagic;
    /** Whether this square is fully magic (all rows, columns, and diagonals sum to magicSum) */
    private final boolean isMagic;
    /** Indices of rows that do not sum to the magic constant */
    private final List<Integer> openRows = new ArrayList<>();
    /** Indices of columns that do not sum to the magic constant */
    private final List<Integer> openCols = new ArrayList<>();

    /**
     * Instantiates a new Magic instance. This instance maintains a reference to
     * the passed int[][] value for performance reasons. Modifying this value
     * subsequently will have side effects on this instance. This constructor is
     * marked private mostly for this reason. Use the
     * {@link net.noisynarwhal.quadrivium.MagicSquare#build(int[][])} to
     * instantiate a new instance using a copy of the passed values' matrix.
     *
     * @param values the values of the magic square
     * @param isSemiMagic true if this instance is known to be at least be
     *                    semi-magic (used as an optimization to shortcut calculations),
     *                    false if it is not known to be semi-magic (may be semi-magic or not)
     */
    private MagicSquare(int[][] values, boolean isSemiMagic) {
        this.values = values;
        this.order = values.length;

        // Calculate the magic constant: n(n²+1)/2
        this.magicSum = this.order * (this.order * this.order + 1) / 2;

        // Maximum score is when all rows, columns, and both diagonals sum to magicSum
        this.maxScore = this.order + this.order + 2;

        int scoreSum = 0;

        if (isSemiMagic) {
            // If we know it's semi-magic, all rows and columns sum correctly
            scoreSum = this.maxScore - 2;
            this.isSemiMagic = true;
        } else {
            // Check each row and column
            for (int i = 0; i < this.order; i++) {
                /*
                 * Score of rows
                 */
                int sumRow = 0;
                /*
                 * Score of cols
                 */
                int sumCol = 0;

                for (int j = 0; j < this.order; j++) {
                    sumRow += this.values[i][j];
                    sumCol += this.values[j][i];
                }

                // If row sums to magic constant, increment score
                if (this.magicSum == sumRow) {
                    scoreSum++;
                } else {
                    // Otherwise, add to list of rows needing improvement
                    this.openRows.add(i);
                }
                // If column sums to magic constant, increment score
                if (this.magicSum == sumCol) {
                    scoreSum++;
                } else {
                    // Otherwise, add to list of columns needing improvement
                    this.openCols.add(i);
                }
            }

            // If all rows and columns sum correctly, it's semi-magic
            this.isSemiMagic = (scoreSum == this.maxScore - 2);
        }

        if (this.isSemiMagic) {
            /*
             * Score of left-to-right diagonal
             */
            int sumlr = 0;
            /*
             * Score of right-to-left diagonal
             */
            int sumrl = 0;
            for (int i = 0; i < this.order; i++) {
                sumlr += this.values[i][i];
                sumrl += this.values[i][this.order - 1 - i];
            }

            // If both diagonals sum to magic constant, add 2 to score
            if (magicSum == sumlr && magicSum == sumrl) {
                scoreSum += 2;
            }
        }

        this.score = scoreSum;
        this.isMagic = scoreSum == this.maxScore;
    }

    /**
     * Create a random square with values from 1 to n*n
     * @param order the order of the square
     * @return a new MagicSquare instance
     * @throws IllegalArgumentException if order is less than 3
     */
    public static MagicSquare build(final int order) {
        if (order < 3) {
            throw new IllegalArgumentException("Order must be at least 3 for a magic square");
        }

        final int[][] values = new int[order][order];

        // Create a list of numbers from 1 to n²
        final List<Integer> valuesList = new ArrayList<>();
        for (int i = 1; i <= order * order; i++) {
            valuesList.add(i);
        }

        // Shuffle the numbers randomly
        Collections.shuffle(valuesList, ThreadLocalRandom.current());

        // Fill the square with the shuffled numbers
        final Iterator<Integer> it = valuesList.iterator();
        for (int r = 0; r < order; r++) {
            for (int c = 0; c < order; c++) {
                values[r][c] = it.next();
                it.remove();
            }
        }

        return new MagicSquare(values, false);
    }

    /**
     * Instantiates a new Magic instance. The passed values are defensively
     * copied prior to returning the new instance; subsequent modifications to
     * the passed int[][] are safe from instance side-affects.
     *
     * @param values the values of the magic square
     * @return a new Magic instance
     */
    public static MagicSquare build(int[][] values) {
        // Create a defensive copy of the values
        values = MatrixUtils.copy(values);

        // Validate that it's a square matrix
        for (final int[] row : values) {
            if (row.length != values.length) {
                throw new IllegalArgumentException("Matrix is not an n*n square");
            }
        }

        return new MagicSquare(values, false);
    }

    /**
     * Evolve this instance to a new instance with a higher score.
     * @return a new Magic instance
     */
    public MagicSquare evolve() {
        MagicSquare magic = this;

        // Generate a new child through mutation
        final MagicSquare child = magic.newChild();

        // Keep the child if it has a better or equal score
        if (child.getScore() >= magic.getScore()) {
            magic = child;
        }

        return magic;
    }

    /**
     * Generate a new child based on this instance's values with a mutation.
     * The mutation strategy depends on whether the square is semi-magic:
     * - If semi-magic: permute rows/columns to improve diagonal sums
     * - If not semi-magic: swap values to improve row/column sums
     *
     * @return a new {@code Magic} instance
     */
    public MagicSquare newChild() {
        final MagicSquare child;
        final int[][] childValues = MatrixUtils.copy(this.values);

        if (this.isSemiMagic) {
            // For semi-magic squares, we focus on improving the diagonals
            int sumDiagsStart = 0;
            for (int j = 0; j < this.order; j++) {
                sumDiagsStart += childValues[j][j];
                sumDiagsStart += childValues[j][this.order - 1 - j];
            }

            final int diffStart = Math.abs((2 * this.magicSum) - sumDiagsStart);
            int sumDiagsEnd = sumDiagsStart;
            int diffEnd;

            do {
                if (RANDOM.nextBoolean()) {
                    // Try swapping two random rows
                    int r1, r2;
                    do {
                        r1 = RANDOM.nextInt(this.order);
                        r2 = RANDOM.nextInt(this.order);
                    } while (r1 == r2);

                    // Update diagonal sums after row swap
                    sumDiagsEnd -= (childValues[r1][r1] + childValues[r1][this.order - 1 - r1]);
                    sumDiagsEnd -= (childValues[r2][r2] + childValues[r2][this.order - 1 - r2]);
                    MatrixUtils.switchRows(childValues, r1, r2);
                    sumDiagsEnd += (childValues[r1][r1] + childValues[r1][this.order - 1 - r1]);
                    sumDiagsEnd += (childValues[r2][r2] + childValues[r2][this.order - 1 - r2]);
                } else {
                    // Try swapping two random columns
                    int c1, c2;
                    do {
                        c1 = RANDOM.nextInt(this.order);
                        c2 = RANDOM.nextInt(this.order);
                    } while (c1 == c2);

                    // Update diagonal sums after column swap
                    sumDiagsEnd -= (childValues[c1][c1] + childValues[this.order - 1 - c1][c1]);
                    sumDiagsEnd -= (childValues[c2][c2] + childValues[this.order - 1 - c2][c2]);
                    MatrixUtils.switchCols(childValues, c1, c2);
                    sumDiagsEnd += (childValues[c1][c1] + childValues[this.order - 1 - c1][c1]);
                    sumDiagsEnd += (childValues[c2][c2] + childValues[this.order - 1 - c2][c2]);
                }

                diffEnd = Math.abs((2 * this.magicSum) - sumDiagsEnd);
            } while (diffEnd > diffStart);

            // The child is known to be at least semi-magic
            child = new MagicSquare(childValues, true);
        } else {
            // For non-semi-magic squares, we focus on improving rows and columns
            final boolean openRowSwap;
            if (!(this.openRows.isEmpty() || this.openCols.isEmpty())) {
                // Randomly choose between improving rows or columns
                openRowSwap = RANDOM.nextBoolean();
            } else {
                // If one list is empty, choose the other
                openRowSwap = this.openCols.isEmpty();
            }

            if (openRowSwap) {
                // Swap values between two open rows
                final int size = this.openRows.size();
                final int idx1 = RANDOM.nextInt(size);
                int idx2;
                do {
                    idx2 = RANDOM.nextInt(size);
                } while (idx2 == idx1);

                final int r1 = this.openRows.get(idx1);
                final int c1 = RANDOM.nextInt(this.order);
                final int r2 = this.openRows.get(idx2);
                final int c2 = RANDOM.nextInt(this.order);
                MatrixUtils.switchValues(childValues, r1, c1, r2, c2);
            } else {
                // Swap values between two open columns
                final int size = this.openCols.size();
                final int idx1 = RANDOM.nextInt(size);
                int idx2;
                do {
                    idx2 = RANDOM.nextInt(size);
                } while (idx2 == idx1);

                final int r1 = RANDOM.nextInt(this.order);
                final int c1 = this.openCols.get(idx1);
                final int r2 = RANDOM.nextInt(this.order);
                final int c2 = this.openCols.get(idx2);
                MatrixUtils.switchValues(childValues, r1, c1, r2, c2);
            }

            // The child may or may not be semi-magic
            child = new MagicSquare(childValues, false);
        }

        return child;
    }

    /**
     * Retrieves a copy of this Magic instance matrix values. Modifications to
     * the returned int[][] will not affect the instance.
     *
     * @return an int[][] copy of this instance's values
     */
    public int[][] getValues() {
        return MatrixUtils.copy(this.values);
    }

    /**
     * @return the order of this magic square
     */
    public int getOrder() {
        return this.order;
    }

    /**
     * @return the current score of this magic square
     */
    public int getScore() {
        return this.score;
    }

    /**
     * @return the maximum possible score for this magic square
     */
    public int getMaxScore() {
        return this.maxScore;
    }

    /**
     * @return whether this square is semi-magic (all rows and columns sum correctly)
     */
    public boolean isSemiMagic() {
        return this.isSemiMagic;
    }

    /**
     * @return whether this square is fully magic (all rows, columns, and diagonals sum correctly)
     */
    public boolean isMagic() {
        return this.isMagic;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (final int[] row : values) {
            for (final int v : row) {
                hash = 31 * hash ^ v;
            }
        }
        return hash;
    }

    /**
     * Compares two Magic instances for equality.
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        final boolean equals;

        if (this == obj) {
            equals = true;
        } else if (obj == null) {
            equals = false;
        } else if (getClass() != obj.getClass()) {
            equals = false;
        } else {
            final MagicSquare other = (MagicSquare) obj;
            equals = MatrixUtils.valuesEqual(this.values, other.values);
        }

        return equals;
    }

    /**
     * Returns a string representation of the Magic instance.
     * @return a string representation of the Magic instance
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final int[] row : this.values) {
            sb.append(Arrays.toString(row));
        }
        return sb.toString();
    }

    /**
     * Compares and ranks by score then by values. Higher scores are prioritized.
     * @param other the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(MagicSquare other) {

        if (other == null) {
            throw new NullPointerException("Cannot compare with null MagicSquare");
        }

        // First compare by score (higher scores are prioritized)
        final int scoreComparison = Integer.compare(other.score, this.score);
        if (scoreComparison != 0) {
            return scoreComparison;
        }

        // If still equal, compare by values lexicographically
        for (int i = 0; i < this.order; i++) {
            for (int j = 0; j < this.order; j++) {
                final int valueComparison = Integer.compare(this.values[i][j], other.values[i][j]);
                if (valueComparison != 0) {
                    return valueComparison;
                }
            }
        }

        // If all elements are equal, squares are equal
        return 0;
    }
}
