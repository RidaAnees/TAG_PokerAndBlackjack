package players.gr9.poker;

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
        // Initialize probabilities for each board/play style. These should sum to 1 for each style.

        //Board texture_PlayerStyle heuristic; probability hardcoded; however can be dynamically updated
        // DRY_AGGRESSIVE: Aggressive players on dry boards often bet top/mid pairs and over pairs. Low bluffing with draws
        allProbabilities.put(BoardAndPlayStyle.DRY_AGGRESSIVE, new HashMap<>());
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.TPSK, 0.30);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.PPAB, 0.30);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.PPBB, 0.05);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.TRifPair, 0.25);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.PF, 0.02);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.PS, 0.02);
        allProbabilities.get(BoardAndPlayStyle.DRY_AGGRESSIVE).put(PokerHandBucket.Other, 0.08);

        // DRY_PASSIVE
        allProbabilities.put(BoardAndPlayStyle.DRY_PASSIVE, new HashMap<>());
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.TPSK, 0.20);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.PPAB, 0.20);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.PPBB, 0.20);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.TRifPair, 0.15);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.PF, 0.03);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.PS, 0.02);
        allProbabilities.get(BoardAndPlayStyle.DRY_PASSIVE).put(PokerHandBucket.Other, 0.20);

        // WET_AGGRESSIVE
        allProbabilities.put(BoardAndPlayStyle.WET_AGGRESSIVE, new HashMap<>());
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.TPSK, 0.10);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.PPAB, 0.10);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.PPBB, 0.05);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.TRifPair, 0.15);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.PF, 0.25);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.PS, 0.25);
        allProbabilities.get(BoardAndPlayStyle.WET_AGGRESSIVE).put(PokerHandBucket.Other, 0.10);

        // WET_PASSIVE
        allProbabilities.put(BoardAndPlayStyle.WET_PASSIVE, new HashMap<>());
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.TPSK, 0.25);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.PPAB, 0.15);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.PPBB, 0.08);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.TRifPair, 0.22);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.PF, 0.10);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.PS, 0.10);
        allProbabilities.get(BoardAndPlayStyle.WET_PASSIVE).put(PokerHandBucket.Other, 0.10);

    }

    // Normalize probabilities for each playstyle so they sum to 1
    private void normalizeAllProbabilities() {
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

    @Override
    public double getProbDistr(PokerHandBucket bucket, BoardAndPlayStyle style) {
        if (allProbabilities.containsKey(style) && allProbabilities.get(style).containsKey(bucket)) {
            return allProbabilities.get(style).get(bucket);
        } else {
            return -1;
        }
    }

    // Update the probabilities for a given style while ensuring they sum to 1
    @Override
    public void updateDistribution(BoardAndPlayStyle style, Map<PokerHandBucket, List<Double>> adjustments) {
        if (allProbabilities.containsKey(style)) {
            Map<PokerHandBucket, Double> styleProbs = allProbabilities.get(style);

            // Apply adjustments for each bucket
            for (PokerHandBucket bucket : adjustments.keySet()) {
                if (styleProbs.containsKey(bucket)) {
                    List<Double> adjustmentList = adjustments.get(bucket);
                    double currentProbability = styleProbs.get(bucket);
                    double newProbability = currentProbability;

                    // Add all adjustments for the bucket
                    for (double adjustment : adjustmentList) {
                        newProbability += adjustment;
                    }

                    // Ensure probability stays within [0, 1] range
                    newProbability = Math.max(0.0, Math.min(1.0, newProbability));

                    // Update the probability for the bucket
                    styleProbs.put(bucket, newProbability);
                }
            }

            // Normalize all probabilities for the style after updates
            normalizeAllProbabilities();
        }
    }

    public Map<PokerHandBucket, Double> getProbabilities(BoardAndPlayStyle boardAndStyle) {
        return allProbabilities.get(boardAndStyle);
    }
}
