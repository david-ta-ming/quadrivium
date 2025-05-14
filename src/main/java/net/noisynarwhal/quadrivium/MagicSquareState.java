package net.noisynarwhal.quadrivium;

import java.io.*;

public class MagicSquareState {

    public static MagicSquare load(File f) throws IOException {

        final MagicSquare magic;

        try(final Reader reader = new BufferedReader(new FileReader(f))) {
            final int[][] values = MatrixUtils.read(reader);
            magic = MagicSquare.build(values);
        }

        return magic;
    }

    public static File save(MagicSquare magic, File f) throws IOException {

        try(final Writer writer = new BufferedWriter(new FileWriter(f))) {
            MatrixUtils.print(magic.getValues(), writer);
        }

        return f;
    }

    public static File save(MagicSquare magic) throws IOException {
        return MagicSquareState.save(magic, File.createTempFile("quadrivium_" + Integer.toString(magic.getOrder()) + '.', ".txt"));
    }
}
