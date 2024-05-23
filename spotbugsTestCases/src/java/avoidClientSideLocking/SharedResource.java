package avoidClientSideLocking;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class SharedResource {
    private final Map<Integer, Future<?>> validationFutures = new HashMap<>();
    private boolean isFileChannelOpen = false;
    private final long totalSize;
    private final int pieceLength;
    private boolean initialized;
    private Piece[] pieces;
    private final BitSet completedPieces;
    private final BitSet requestedPieces;

    public SharedResource(long totalSize, int pieceLength) {
        this.totalSize = totalSize;
        this.pieceLength = pieceLength;
        this.pieces = new Piece[0];
        this.completedPieces = new BitSet();
        this.requestedPieces = new BitSet();
    }

    public void closeFileChannelIfNecessary() {
        if (isFileChannelOpen && pieces.length == 0) {
            isFileChannelOpen = false;
        }
    }

    public synchronized void init() {
        if (this.initialized) {
            throw new IllegalStateException("Resource was already initialized!");
        }
        this.pieces = new Piece[(int) (totalSize / pieceLength)];
        this.initialized = true;
    }

    public void hashSingleThread() {
        synchronized (this.pieces) {
            for (int idx = 0; idx < this.pieces.length; idx++) {
                this.pieces[idx] = new Piece(idx, pieceLength);
                if (this.pieces[idx].isValid()) {
                    this.completedPieces.set(idx);
                }
            }
        }
    }

    public Piece getPiece(int index) {
        synchronized (this.pieces) {
            if (index >= this.pieces.length) {
                throw new IllegalArgumentException("Invalid piece index!");
            }

            return this.pieces[index];
        }
    }

    public static void main(String[] args) {
        SharedResource resource = new SharedResource(1000L, 100);
        resource.init();
        resource.hashSingleThread();
        resource.getPiece(1);
        resource.closeFileChannelIfNecessary();
    }
}

class Piece {
    private final int index;
    private final int length;
    private boolean valid;

    public Piece(int index, int length) {
        this.index = index;
        this.length = length;
        this.valid = true;
    }

    public boolean isValid() {
        return valid;
    }
}
