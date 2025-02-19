/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.noisynarwhal.quadrivium;

import java.io.*;
import java.util.*;

/**
 * @author lioudt
 */
public class MatrixUtils {

    /**
     * Copy the given matrix
     *
     * @param values the matrix to copy
     * @return a copy of the matrix
     */
    public static int[][] copy(int[][] values) {
        final int[][] copy = new int[values.length][];
        for (int r = 0; r < values.length; r++) {
            copy[r] = Arrays.copyOf(values[r], values[r].length);
        }
        return copy;
    }

    /**
     * Modify the passed matrix such that the two specified values are switched
     *
     * @param matrix the matrix to modify
     * @param r1     the row index of the first value
     * @param c1     the column index of the first value
     * @param r2     the row index of the second value
     * @param c2     the column index of the second value
     */
    public static void switchValues(final int[][] matrix, int r1, int c1, int r2, int c2) {
        matrix[r1][c1] ^= matrix[r2][c2];
        matrix[r2][c2] ^= matrix[r1][c1];
        matrix[r1][c1] ^= matrix[r2][c2];
    }

    /**
     * Modify the passed matrix such that the two specified columns are switched
     *
     * @param matrix the matrix to modify
     * @param c1     the index of the first column
     * @param c2     the index of the second column
     */
    public static void switchCols(final int[][] matrix, final int c1, final int c2) {

        for (final int[] row : matrix) {

            final int v2 = row[c2];

            row[c2] = row[c1];
            row[c1] = v2;

        }

    }

    /**
     * Modify the passed matrix such that the two specified rows are switched
     *
     * @param matrix the matrix to modify
     * @param r1     the index of the first row
     * @param r2     the index of the second row
     */
    public static void switchRows(final int[][] matrix, final int r1, final int r2) {

        final int r2len = matrix[r2].length;

        final int[] valsR2 = new int[r2len];

        System.arraycopy(matrix[r2], 0, valsR2, 0, r2len);

        matrix[r2] = matrix[r1];
        matrix[r1] = valsR2;
    }

    /**
     * @param square the square to modify
     * @return the square with the rows reversed
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
     * Mirror the square
     *
     * @param square the square to mirror
     * @return the mirrored square
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
     * Rotate a square matrix
     *
     * @param square the square to rotate
     * @return the rotated square
     */
    public static int[][] rotate(int[][] square) {

        return MatrixUtils.mirror(MatrixUtils.transpose(square));
    }

    /**
     * Print a matrix of integers
     *
     * @param values the matrix to print
     * @param out    the writer to write to
     * @throws IOException if an I/O error occurs
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
     * Print a matrix of integers
     *
     * @param values the matrix to print
     * @return the matrix as a string
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
     * Compare two matrices for equality
     *
     * @param values1 the first matrix
     * @param values2 the second matrix
     * @return true if the two matrices are equal
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
     * Return a matrix in Frénicle standard form
     *
     * @param matrix the matrix to standardize
     * @return the matrix in Frénicle standard form
     * @see <a href="https://en.wikipedia.org/wiki/Fr%C3%A9nicle_standard_form">Frénicle standard form</a>
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
     * Parse a white-space delimited matrix of integers
     *
     * @param in the reader to read from
     * @return the matrix
     * @throws IOException if an I/O error occurs
     */
    public static int[][] read(final Reader in) throws IOException {

        final int[][] matrix;

        final List<int[]> rows = new ArrayList<>();

        try (final BufferedReader reader = (in instanceof BufferedReader) ? (BufferedReader) in : new BufferedReader(in)) {

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

        }

        matrix = new int[rows.size()][];

        int i = 0;
        for (final int[] row : rows) {
            matrix[i++] = row;
        }

        return matrix;
    }

    /**
     * Tests the sum of rows, columns, and diagonals for equality. This does not
     * check that the values are distinct or consecutive.
     *
     * @param matrix the square to test
     * @return true if the square is magic
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
     * Tests whether a square is bi-magic
     *
     * @param matrix the square to test
     * @return true if the square is bi-magic
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
