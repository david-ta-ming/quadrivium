package net.noisynarwhal.quadrivium;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker class for MagicSquare
 */
public class MagicSquareWorker implements Callable<MagicSquare> {
    private final int order;
    private final AtomicBoolean solutionFound;

    // Write javadocs for the constructor=
    /**
     * Constructor for MagicSquareWorker
     * @param order
     * @param solutionFound
     */
    public MagicSquareWorker(int order, AtomicBoolean solutionFound) {
        this.order = order;
        this.solutionFound = solutionFound;
    }

    @Override
    public MagicSquare call() {
        MagicSquare magic = MagicSquare.build(order);
        while (!(magic.isMagic() || solutionFound.get())) {
            magic = magic.evolve();
        }
        solutionFound.compareAndSet(false, magic.isMagic());
        return magic;
    }
}
