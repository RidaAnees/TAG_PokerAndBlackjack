package players.ISMCTS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllProbabilities implements BeliefDistribution {

    private Map<BoardAndPlayStyle, Map<PokerHandBucket, Double>> allProbabilities;

    public AllProbabilities() {
        allProbabilities = new HashMap<>();
        initializeProbabilities();
    }

    private void initializeProbabilities() {
        /*
         * The playing styles are considered mutually exclusive. An opponent can only play one style at a time
         * The buckets are not strictly mutually exclusive
         * The sum of the probabilities given a playstyle should equal 1
         * */
        // DRY_AGGRESSIVE must sum up to 1
        allProbabilities.put(BoardAndPlayStyle.DRY_AGGRESSIVE, new HashMap<>());
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.TPSK, 0.20);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.TPGK, 0.22);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.TPWK, 0.22);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.PPAB, 0.02);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.PPBB, 0.02);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.TR, 0.32);

        // DRY_PASSIVE must sum up to 1
        allProbabilities.put(BoardAndPlayStyle.DRY_PASSIVE, new HashMap<>());
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.TPSK, 0.10);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.TPGK, 0.20);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.TPWK, 0.25);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.PPAB, 0.20);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.PPBB, 0.15);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.TR, 0.10);

        // WET_AGGRESSIVE must sum up to 1
        allProbabilities.put(BoardAndPlayStyle.WET_AGGRESSIVE, new HashMap<>());
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.TPSK, 0.40);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.TPGK, 0.25);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.TPWK, 0.15);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.PPAB, 0.10);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.PPBB, 0.05);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.TR, 0.05);

        // WET_PASSIVE must sum up to 1
        allProbabilities.put(BoardAndPlayStyle.WET_PASSIVE, new HashMap<>());
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.TPSK, 0.08);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.TPGK, 0.12);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.TPWK, 0.20);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.PPAB, 0.25);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.PPBB, 0.20);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.TR, 0.15);

        //normalizeallProbabilities();
        }

    private void normalizeallProbabilities() {
        for (BoardAndPlayStyle style : allProbabilities.keySet()) {
            double sum = 0.0;
            for (double prob : allProbabilities.get(style).values()) {
                sum += prob;
            }
            if (sum > 0) {
                for (PokerHandBucket bucket : allProbabilities.get(style).keySet()) {
                    allProbabilities.get(style).put(bucket, allProbabilities.get(style).get(bucket) / sum);
                }
            }
        }
    }

    public double getBucketProbability(BoardAndPlayStyle style, PokerHandBucket bucket) {
        if (allProbabilities.containsKey(style) && allProbabilities.get(style).containsKey(bucket)) {
            return allProbabilities.get(style).get(bucket);
        } else {
            throw new IllegalArgumentException("Invalid style or bucket.");
        }
    }

    @Override
    public double getProbDistr(PokerHandBucket bucket, BoardAndPlayStyle style) throws Exception {
        if (allProbabilities.containsKey(style) && allProbabilities.get(style).containsKey(bucket)) {
            return allProbabilities.get(style).get(bucket);
        } else {
            throw new Exception("Invalid bucket name or board&play style");
        }
    }

    //updates for the whole playing style inorder to ensure it all sums upto 1
    @Override
    public void updateDistribution(BoardAndPlayStyle style, Map<PokerHandBucket, List<Double>> adjustments) {
        if (allProbabilities.containsKey(style)) {
            Map<PokerHandBucket, Double> styleProbs = allProbabilities.get(style);
            for (PokerHandBucket bucket : adjustments.keySet()) {
                if (styleProbs.containsKey(bucket)) {
                    List<Double> adjustmentList = adjustments.get(bucket);
                    double currentProbability = styleProbs.get(bucket);
                    double newProbability = currentProbability;

                    for (double adjustment : adjustmentList) {
                        newProbability += adjustment;
                    }

                    newProbability = Math.max(0.0, Math.min(1.0, newProbability));
                    styleProbs.put(bucket, newProbability);
                }
            }
            normalizeallProbabilities();
        }
    }
}