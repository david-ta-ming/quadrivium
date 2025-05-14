package net.noisynarwhal.quadrivium;

import java.io.*;

/**
 * Utility class for managing the persistence of MagicSquare objects.
 * Provides functionality to save magic squares to files and load them back.
 */
public class MagicSquareState {

    /**
     * Loads a magic square from a file.
     * The file should contain a matrix of numbers representing the magic square.
     *
     * @param f The file to load the magic square from
     * @return A new MagicSquare instance constructed from the file contents
     * @throws IOException If there are any issues reading the file or parsing its contents
     */
    public static MagicSquare load(File f) throws IOException {

        final MagicSquare magic;

        try(final Reader reader = new BufferedReader(new FileReader(f))) {
            final int[][] values = MatrixUtils.read(reader);
            magic = MagicSquare.build(values);
        }

        return magic;
    }

    /**
     * Saves a magic square to a specified file.
     * The magic square's values are written as a matrix to the file.
     *
     * @param magic The magic square to save
     * @param f The file to save the magic square to
     * @return The file that was written to
     * @throws IOException If there are any issues writing to the file
     */
    public static File save(MagicSquare magic, File f) throws IOException {

        try(final Writer writer = new BufferedWriter(new FileWriter(f))) {
            MatrixUtils.print(magic.getValues(), writer);
        }

        return f;
    }

    /**
     * Saves a magic square to a temporary file.
     * The filename includes the order of the magic square for identification.
     *
     * @param magic The magic square to save
     * @return The temporary file that was created and written to
     * @throws IOException If there are any issues creating or writing to the temporary file
     */
    public static File save(MagicSquare magic) throws IOException {
        return MagicSquareState.save(magic, File.createTempFile("quadrivium_" + Integer.toString(magic.getOrder()) + '.', ".txt"));
    }
}
