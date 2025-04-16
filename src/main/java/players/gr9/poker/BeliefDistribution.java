package players.gr9.poker;

import java.util.List;
import java.util.Map;

public interface BeliefDistribution {
    double getProbDistr(PokerHandBucket bucket, BoardAndPlayStyle style);

    void updateDistribution(BoardAndPlayStyle style, Map<PokerHandBucket, List<Double>> adjustments);
    enum PokerHandBucket {
        TPSK, PPAB, PPBB, TRifPair, PF, PS, Other
    }
    enum BoardAndPlayStyle {
        DRY_AGGRESSIVE, DRY_PASSIVE, WET_AGGRESSIVE, WET_PASSIVE
    }
}