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
    private final Random RANDOM = ThreadLocalRandom.current();
    private final int magicSum;
    private final int order;
    private final int[][] values;
    private final int score;
    private final int maxScore;

    private final boolean isSemiMagic;
    private final boolean isMagic;
    /**
     * Rows that do not sum to the magic constant
     */
    private final List<Integer> openRows = new ArrayList<>();
    /**
     * Cols that do not sum to the magic constant
     */
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
     *                    semi-magic (used as an optimization to shortcut calculations)
     */
    private MagicSquare(int[][] values, boolean isSemiMagic) {

        this.values = values;
        this.order = values.length;

        this.magicSum = this.order * (this.order * this.order + 1) / 2;

        this.maxScore = this.order + this.order + 2;

        int scoreSum = 0;

        if (isSemiMagic) {

            scoreSum = this.maxScore - 2;

            this.isSemiMagic = true;

        } else {

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

                if (this.magicSum == sumRow) {
                    scoreSum++;
                } else {
                    this.openRows.add(i);
                }
                if (this.magicSum == sumCol) {
                    scoreSum++;
                } else {
                    this.openCols.add(i);
                }
            }

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
     */
    public static MagicSquare build(final int order) {

        final int[][] values = new int[order][order];

        final List<Integer> valuesList = new ArrayList<>();
        for (int i = 1; i <= order * order; i++) {
            valuesList.add(i);
        }

        Collections.shuffle(valuesList, ThreadLocalRandom.current());

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

        values = MatrixUtils.copy(values);

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

        final MagicSquare child = magic.newChild();

        if (child.getScore() >= magic.getScore()) {
            magic = child;
        }

        return magic;
    }

    /**
     * Generate a new child based on this instance's values with a mutation.
     *
     * @return a new {@code Magic} instance
     */
    public MagicSquare newChild() {

        final MagicSquare child;

        final int[][] childValues = MatrixUtils.copy(this.values);

        /*
         * If semi-magic permute rows and columns, otherwise switch cell values
         */
        if (this.isSemiMagic) {

            /*
             * Sum the diagonals, keep the new values if it is closer to 2 *
             * magic sum
             */
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
                    /*
                     * Row exchange
                     */
                    int r1;
                    int r2;

                    do {
                        r1 = RANDOM.nextInt(this.order);
                        r2 = RANDOM.nextInt(this.order);
                    } while (r1 == r2);

                    sumDiagsEnd -= (childValues[r1][r1] + childValues[r1][this.order - 1 - r1]);
                    sumDiagsEnd -= (childValues[r2][r2] + childValues[r2][this.order - 1 - r2]);

                    MatrixUtils.switchRows(childValues, r1, r2);

                    sumDiagsEnd += (childValues[r1][r1] + childValues[r1][this.order - 1 - r1]);
                    sumDiagsEnd += (childValues[r2][r2] + childValues[r2][this.order - 1 - r2]);

                } else {
                    /*
                     * Col exchange
                     */
                    int c1;
                    int c2;

                    do {
                        c1 = RANDOM.nextInt(this.order);
                        c2 = RANDOM.nextInt(this.order);
                    } while (c1 == c2);

                    sumDiagsEnd -= (childValues[c1][c1] + childValues[this.order - 1 - c1][c1]);
                    sumDiagsEnd -= (childValues[c2][c2] + childValues[this.order - 1 - c2][c2]);

                    MatrixUtils.switchCols(childValues, c1, c2);

                    sumDiagsEnd += (childValues[c1][c1] + childValues[this.order - 1 - c1][c1]);
                    sumDiagsEnd += (childValues[c2][c2] + childValues[this.order - 1 - c2][c2]);
                }

                diffEnd = Math.abs((2 * this.magicSum) - sumDiagsEnd);

            } while (diffEnd > diffStart);

            /*
             * This child is known to be at least semi-magic
             */
            child = new MagicSquare(childValues, true);

        } else {

            /*
             * Pick a swap by open rows or open cols
             */
            final boolean openRowSwap;

            if (!(this.openRows.isEmpty() || this.openCols.isEmpty())) {
                openRowSwap = RANDOM.nextBoolean();
            } else {
                openRowSwap = this.openCols.isEmpty();
            }

            if (openRowSwap) {

                final int size = this.openRows.size();
                final int idx1 = RANDOM.nextInt(size);
                int idx2;
                do {
                    idx2 = RANDOM.nextInt(size);
                } while (idx2 == idx1);

                /*
                 * Value exchange
                 */
                final int r1 = this.openRows.get(idx1);
                final int c1 = RANDOM.nextInt(this.order);

                final int r2 = this.openRows.get(idx2);
                final int c2 = RANDOM.nextInt(this.order);

                MatrixUtils.switchValues(childValues, r1, c1, r2, c2);

            } else {

                final int size = this.openCols.size();
                final int idx1 = RANDOM.nextInt(size);
                int idx2;
                do {
                    idx2 = RANDOM.nextInt(size);
                } while (idx2 == idx1);

                /*
                 * Value exchange
                 */
                final int r1 = RANDOM.nextInt(this.order);
                final int c1 = this.openCols.get(idx1);

                final int r2 = RANDOM.nextInt(this.order);
                final int c2 = this.openCols.get(idx2);

                MatrixUtils.switchValues(childValues, r1, c1, r2, c2);

            }

            /*
             * This child may or may not be semi-magic
             */
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
     * @return the magic sum of this magic square
     */
    public int getScore() {
        return this.score;
    }

    /**
     * @return the magic sum of this magic square
     */
    public int getMaxScore() {
        return this.maxScore;
    }

    /**
     * @return the magic sum of this magic square
     */
    public boolean isSemiMagic() {
        return this.isSemiMagic;
    }

    /**
     * @return the magic sum of this magic square
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
