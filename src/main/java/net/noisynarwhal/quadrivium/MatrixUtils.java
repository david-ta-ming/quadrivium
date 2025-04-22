/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.noisynarwhal.quadrivium;

import java.io.*;
import java.util.*;

/**
 * Utility class for matrix operations used in magic square generation and manipulation.
 * Provides methods for copying, transforming, and analyzing square matrices.
 *
 * @author lioudt
 */
public class MatrixUtils {

    /**
     * Creates a deep copy of the given matrix.
     *
     * @param values the matrix to copy
     * @return a new matrix containing copies of all values
     * @throws NullPointerException if the input matrix is null
     */
    public static int[][] copy(int[][] values) {
        final int[][] copy = new int[values.length][];
        for (int r = 0; r < values.length; r++) {
            copy[r] = Arrays.copyOf(values[r], values[r].length);
        }
        return copy;
    }

    /**
     * Swaps two values in a matrix at the specified positions. If both positions are the same,
     * the method returns immediately without making any changes.
     *
     * @param matrix the matrix to modify
     * @param r1     the row index of the first value (0-based)
     * @param c1     the column index of the first value (0-based)
     * @param r2     the row index of the second value (0-based)
     * @param c2     the column index of the second value (0-based)
     * @throws ArrayIndexOutOfBoundsException if any of the indices are out of bounds
     * @throws NullPointerException if the matrix is null
     */
    public static void switchValues(final int[][] matrix, int r1, int c1, int r2, int c2) {
        if (r1 == r2 && c1 == c2) return;
        final int temp = matrix[r1][c1];
        matrix[r1][c1] = matrix[r2][c2];
        matrix[r2][c2] = temp;
    }

    /**
     * Swaps two columns in a matrix.
     *
     * @param matrix the matrix to modify
     * @param c1     the index of the first column to swap (0-based)
     * @param c2     the index of the second column to swap (0-based)
     * @throws ArrayIndexOutOfBoundsException if either column index is out of bounds
     * @throws NullPointerException if the matrix is null
     */
    public static void switchCols(final int[][] matrix, final int c1, final int c2) {

        for (final int[] row : matrix) {

            final int v2 = row[c2];

            row[c2] = row[c1];
            row[c1] = v2;

        }

    }

    /**
     * Swaps two rows in a matrix.
     *
     * @param matrix the matrix to modify
     * @param r1     the index of the first row to swap (0-based)
     * @param r2     the index of the second row to swap (0-based)
     * @throws ArrayIndexOutOfBoundsException if either row index is out of bounds
     * @throws NullPointerException if the matrix is null
     */
    public static void switchRows(final int[][] matrix, final int r1, final int r2) {

        final int r2len = matrix[r2].length;

        final int[] valsR2 = new int[r2len];

        System.arraycopy(matrix[r2], 0, valsR2, 0, r2len);

        matrix[r2] = matrix[r1];
        matrix[r1] = valsR2;
    }

    /**
     * Creates the transpose of a square matrix.
     *
     * @param square the square matrix to transpose
     * @return a new matrix that is the transpose of the input
     * @throws IllegalArgumentException if the input matrix is not square
     * @throws NullPointerException if the input matrix is null
     */
    public static int[][] transpose(int[][] square) {

        final int[][] sq = MatrixUtils.copy(square);

        for (int i = 0; i < sq.length; i++) {
            for (int j = i; j < sq.length; j++) {

                if (i != j) {
                    final int v = sq[i][j];
                    sq[i][j] = sq[j][i];
                    sq[j][i] = v;
                }
            }
        }

        return sq;
    }

    /**
     * Creates a mirror image of a square matrix along its main diagonal.
     *
     * @param square the square matrix to mirror
     * @return a new matrix that is the mirror image of the input
     * @throws IllegalArgumentException if the input matrix is not square
     * @throws NullPointerException if the input matrix is null
     */
    public static int[][] mirror(int[][] square) {

        final int[][] sq = MatrixUtils.copy(square);

        for (final int[] row : sq) {

            int a = 0;
            int b = row.length - 1;
            while (a < b) {
                final int v = row[a];
                row[a] = row[b];
                row[b] = v;
                a++;
                b--;
            }

        }

        return sq;
    }

    /**
     * Rotates a square matrix 90 degrees clockwise.
     *
     * @param square the square matrix to rotate
     * @return a new matrix that is rotated 90 degrees clockwise
     * @throws IllegalArgumentException if the input matrix is not square
     * @throws NullPointerException if the input matrix is null
     */
    public static int[][] rotate(int[][] square) {

        return MatrixUtils.mirror(MatrixUtils.transpose(square));
    }

    /**
     * Prints a matrix to the specified writer.
     *
     * @param values the matrix to print
     * @param out    the writer to print to
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if either argument is null
     */
    public static void print(int[][] values, Writer out) throws IOException {

        int maxVal = values[0][0];
        for (final int[] row : values) {
            for (final int v : row) {
                maxVal = Math.max(v, maxVal);
            }
        }
        /*
         * For visual formatting, length of max value plus 1 as separator
         */
        final int padLen = (int) (Math.log10(maxVal) + 1) + 1;
        final String padFormat = "%1$" + padLen + "s";

        final BufferedWriter writer = (out instanceof BufferedWriter) ? (BufferedWriter) out : new BufferedWriter(out);

        /*
         * Write first line then write subsequent lines preceded by newline
         */
        {
            final int[] row = values[0];
            for (final int v : row) {
                final String s = String.format(padFormat, Integer.toString(v));
                writer.append(s);
            }
        }

        for (int r = 1; r < values.length; r++) {

            writer.newLine();

            final int[] row = values[r];

            for (final int v : row) {
                final String s = String.format(padFormat, Integer.toString(v));
                writer.append(s);
            }

        }

        writer.flush();
    }

    /**
     * Converts a matrix to a string representation.
     *
     * @param values the matrix to convert
     * @return a string representation of the matrix
     * @throws NullPointerException if the input matrix is null
     */
    public static String print(int[][] values) {

        try(final StringWriter writer = new StringWriter()) {

            MatrixUtils.print(values, writer);

            /*
             * Remove last newline
             */
            return writer.toString();

        } catch (IOException ex) {
            /*
             * Should not happen with StringWriter
             */
            throw new RuntimeException(ex);
        }
    }

    /**
     * Compares two matrices for equality of their values.
     *
     * @param values1 the first matrix to compare
     * @param values2 the second matrix to compare
     * @return true if both matrices have the same dimensions and values
     * @throws NullPointerException if either matrix is null
     */
    public static boolean valuesEqual(int[][] values1, int[][] values2) {

        if (values1.length != values2.length) {
            return false;
        }

        for (int r = 0; r < values1.length; r++) {
            for (int c = 0; c < values1.length; c++) {
                if (values1[r][c] != values2[r][c]) {
                    return false;
                }
            }

        }

        return true;
    }

    /**
     * Standardizes a matrix by sorting its rows and columns.
     *
     * @param matrix the matrix to standardize
     * @return a new standardized matrix
     * @throws NullPointerException if the input matrix is null
     */
    public static int[][] standardize(int[][] matrix) {

        /*
         * A sorted set based on integer comparisons starting with the
         * upper-left then proceeding left-to-right and up-to-down.
         */
        final SortedSet<int[][]> forms = new TreeSet<>(new Comparator<int[][]>() {
            @Override
            public int compare(int[][] mtrx1, int[][] mtrx2) {
                int rank = 0;
                for (int r = 0; r < mtrx1.length && rank == 0; r++) {
                    for (int c = 0; c < mtrx1.length && rank == 0; c++) {
                        final int v1 = mtrx1[r][c];
                        final int v2 = mtrx2[r][c];

                        rank = v1 - v2;
                    }

                }
                return rank;
            }
        });

        /*
         * Add the 8 forms of this matrix: the original + its 3 rotations, then
         * the mirror of the original + its 3 rotations.
         */
        forms.add(matrix);

        for (int i = 0; i < 3; i++) {
            matrix = MatrixUtils.rotate(matrix);
            forms.add(matrix);
        }

        matrix = MatrixUtils.mirror(matrix);
        forms.add(matrix);

        for (int i = 0; i < 3; i++) {
            matrix = MatrixUtils.rotate(matrix);
            forms.add(matrix);
        }

        /*
         * Return the lowest ranked form from the sorted set, as the Frénicle
         * standard form
         */
        return forms.first();
    }

    /**
     * Reads a matrix from a reader.
     *
     * @param in the reader to read from
     * @return the matrix read from the input
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if the input reader is null
     */
    public static int[][] read(final Reader in) throws IOException {

        final int[][] matrix;

        final List<int[]> rows = new ArrayList<>();

        final BufferedReader reader = (in instanceof BufferedReader) ? (BufferedReader) in : new BufferedReader(in);

        String line;
        while ((line = reader.readLine()) != null) {

            line = line.trim();

            if (!line.isEmpty()) {

                final String[] vals = line.split("\\s+");

                final int[] row = new int[vals.length];
                for (int i = 0; i < vals.length; i++) {
                    row[i] = Integer.parseInt(vals[i]);
                }

                rows.add(row);

            }

        }

        matrix = new int[rows.size()][];

        int i = 0;
        for (final int[] row : rows) {
            matrix[i++] = row;
        }

        return matrix;
    }

    /**
     * Checks if a matrix is a magic square.
     *
     * @param matrix the matrix to check
     * @return true if the matrix is a magic square
     * @throws IllegalArgumentException if the input matrix is not square
     * @throws NullPointerException if the input matrix is null
     */
    public static boolean isMagic(int[][] matrix) {

        for (final int[] row : matrix) {
            if (row.length != matrix.length) {
                throw new IllegalArgumentException("Matrix is not an N*N square");
            }
        }

        final int order = matrix.length;

        /*
         * Assign magic sum as sum of first row
         */
        final int magicSum;
        {
            int sumRow = 0;
            for (final int v : matrix[0]) {
                sumRow += v;
            }
            magicSum = sumRow;
        }

        /*
         * Test sum of rows
         */
        for (int r = 1; r < order; r++) {
            final int[] row = matrix[r];

            int sumRow = 0;
            for (final int v : row) {
                sumRow += v;
            }
            if (magicSum != sumRow) {
                return false;
            }
        }

        /*
         * Test sum of columns
         */
        for (int c = 0; c < order; c++) {
            int sumCol = 0;
            for (final int[] ints : matrix) {
                sumCol += ints[c];
            }
            if (magicSum != sumCol) {
                return false;
            }
        }

        /*
         * Test sum of left to right diagonal
         */
        {
            int sumDiag = 0;
            for (int i = 0; i < order; i++) {
                sumDiag += matrix[i][i];
            }
            if (magicSum != sumDiag) {
                return false;
            }
        }

        /*
         * Test sum of right to left diagonal
         */
        {
            int sumDiag = 0;
            for (int i = 0; i < order; i++) {
                sumDiag += matrix[i][order - 1 - i];
            }
            return magicSum == sumDiag;
        }
    }

    /**
     * Checks if a matrix is a bimagic square (magic square where the squares of the numbers
     * also form a magic square).
     *
     * @param matrix the matrix to check
     * @return true if the matrix is a bimagic square
     * @throws IllegalArgumentException if the input matrix is not square
     * @throws NullPointerException if the input matrix is null
     */
    public static boolean isBiMagic(int[][] matrix) {

        matrix = MatrixUtils.copy(matrix);

        for (final int[] row : matrix) {
            for (int c = 0; c < row.length; c++) {
                row[c] = row[c] * row[c];
            }
        }

        return MatrixUtils.isMagic(matrix);
    }

}
