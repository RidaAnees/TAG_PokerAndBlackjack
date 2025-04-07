package players.ISMCTS;

import core.components.FrenchCard;
import java.util.*;

public class PokerHandSampler {
    private PokerHandBuckets buckets;
    private BeliefDistribution belief;
    private Random random = new Random();

    public PokerHandSampler(PokerHandBuckets buckets, BeliefDistribution belief) {
        this.buckets = buckets;
        this.belief = belief;
    }

    // Random sampling from a randomly selected bucket
    public List<FrenchCard> sampleHandRandomBucket(BeliefDistribution.BoardAndPlayStyle style) throws Exception {
        List<BeliefDistribution.PokerHandBucket> bucketList = new ArrayList<>(EnumSet.allOf(BeliefDistribution.PokerHandBucket.class));
        BeliefDistribution.PokerHandBucket selectedBucket = bucketList.get(random.nextInt(bucketList.size()));

        return sampleHandFromBucket(selectedBucket);
    }

    // Weighted sampling based on belief distribution
    public List<FrenchCard> sampleHandWeighted(BeliefDistribution.BoardAndPlayStyle style) throws Exception {

        Map<BeliefDistribution.PokerHandBucket, Double> bucketProbabilities = new HashMap<>();

        // Get the probabilities for each bucket given the board and play style.
        for (BeliefDistribution.PokerHandBucket bucket : BeliefDistribution.PokerHandBucket.values()) {
            bucketProbabilities.put(bucket, belief.getProbDistr(bucket, style));
        }

        // Select a bucket based on the weighted probabilities.
        BeliefDistribution.PokerHandBucket selectedBucket = selectWeightedBucket(bucketProbabilities);

        // Sample a hand from the selected bucket.
        return sampleHandFromBucket(selectedBucket);
    }

    private BeliefDistribution.PokerHandBucket selectWeightedBucket(Map<BeliefDistribution.PokerHandBucket, Double> bucketProbabilities) {
        double randomValue = random.nextDouble();
        double cumulativeProbability = 0.0;

        for (Map.Entry<BeliefDistribution.PokerHandBucket, Double> entry : bucketProbabilities.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (randomValue < cumulativeProbability) {
                return entry.getKey();
            }
        }
        //This should never happen due to normalization.
        return null;
    }

    private List<FrenchCard> sampleHandFromBucket(BeliefDistribution.PokerHandBucket selectedBucket) {
        String bucketName = selectedBucket.name();

        List<List<FrenchCard>> handsInBucket = buckets.getHandsInBucket(bucketName);

        if (handsInBucket == null || handsInBucket.isEmpty()) {
            // Handle the case where the bucket is empty or doesn't exist
            return Collections.emptyList();
        }
        int randomIndex = random.nextInt(handsInBucket.size());
        return handsInBucket.get(randomIndex);
    }
}