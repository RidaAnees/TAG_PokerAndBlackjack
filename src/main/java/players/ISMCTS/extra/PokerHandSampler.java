//package players.ISMCTS;
//
//import core.components.FrenchCard;
//import java.util.*;
//
//public class PokerHandSampler {
//    private PokerHandBuckets buckets;
//    private BeliefDistribution belief;
//    private Random random = new Random();
//
//    public PokerHandSampler(PokerHandBuckets buckets, BeliefDistribution belief) {
//        this.buckets = buckets;
//        this.belief = belief;
//    }
//
//    // Random sampling from a randomly selected bucket
//    public List<FrenchCard> sampleHandRandomBucket(BeliefDistribution.BoardAndPlayStyle style) throws Exception {
//        List<BeliefDistribution.PokerHandBucket> bucketList = new ArrayList<>(EnumSet.allOf(BeliefDistribution.PokerHandBucket.class));
//        BeliefDistribution.PokerHandBucket selectedBucket = bucketList.get(random.nextInt(bucketList.size()));
//
//        return sampleHandFromBucket(selectedBucket);
//    }
//
//    // Weighted sampling based on belief distribution
//    public List<FrenchCard> sampleHandWeighted(BeliefDistribution.BoardAndPlayStyle style) throws Exception {
//
//        Map<BeliefDistribution.PokerHandBucket, Double> bucketProbabilities = new HashMap<>();
//
//        // Get the probabilities for each bucket given the board and play style.
//        for (BeliefDistribution.PokerHandBucket bucket : BeliefDistribution.PokerHandBucket.values()) {
//            bucketProbabilities.put(bucket, belief.getProbDistr(bucket, style));
//        }
//
//        // Select a bucket based on the weighted probabilities.
//        BeliefDistribution.PokerHandBucket selectedBucket = selectWeightedBucket(bucketProbabilities);
//
//        // Sample a hand from the selected bucket.
//        return sampleHandFromBucket(selectedBucket); // Again, picking a sweet from a chosen type!
//    }
//
//    private BeliefDistribution.PokerHandBucket selectWeightedBucket(Map<BeliefDistribution.PokerHandBucket, Double> bucketProbabilities) {
//        double randomValue = random.nextDouble();// generate random number between 0 and 1
//        double cumulativeProbability = 0.0;
//
//        for (Map.Entry<BeliefDistribution.PokerHandBucket, Double> entry : bucketProbabilities.entrySet()) {
//            cumulativeProbability += entry.getValue();
//            if (randomValue < cumulativeProbability) { // each bucket gets selected based on its assigned probability (weight) — not equally
//                return entry.getKey();
//            }
//        }
//        //This should never happen due to normalization.
//        return null;
//    }
//
//    private BeliefDistribution.PokerHandBucket selectWeightedBucketwNoise (Map<BeliefDistribution.PokerHandBucket, Double>bucketProbabilities){
//        // Add small noise to each probability
//        Map<BeliefDistribution.PokerHandBucket, Double> noisyProbabilities = new HashMap<>();
//        double total = 0.0;
//        double noiseScale = 0.05; // Adjust noise level here (5% of randomness)
//
//        for (Map.Entry<BeliefDistribution.PokerHandBucket, Double> entry : bucketProbabilities.entrySet()) {
//            double baseProb = entry.getValue();
//            double noise = (random.nextDouble() - 0.5) * 2 * noiseScale * baseProb; // Noise in [-5%, +5%]
//            double noisyProb = Math.max(0.0, baseProb + noise); // Avoid negative values
//            noisyProbabilities.put(entry.getKey(), noisyProb);
//            total += noisyProb;
//        }
//
//        // Normalize the noisy probabilities
//        for (Map.Entry<BeliefDistribution.PokerHandBucket, Double> entry : noisyProbabilities.entrySet()) {
//            noisyProbabilities.put(entry.getKey(), entry.getValue() / total);
//        }
//
//        // Now sample from the noisy distribution
//        double randomValue = random.nextDouble();
//        double cumulativeProbability = 0.0;
//
//        for (Map.Entry<BeliefDistribution.PokerHandBucket, Double> entry : noisyProbabilities.entrySet()) {
//            cumulativeProbability += entry.getValue();
//            if (randomValue < cumulativeProbability) {
//                return entry.getKey();
//            }
//        }
//        return null; // Should never happen
//    }
//    private List<FrenchCard> sampleHandFromBucket(BeliefDistribution.PokerHandBucket selectedBucket) {
//        String bucketName = selectedBucket.name();
//        List<List<FrenchCard>> handsInBucket = buckets.getHandsInBucket(bucketName);
//        if (handsInBucket == null || handsInBucket.isEmpty()) {
//            return Collections.emptyList();
//        }
//        int randomIndex = random.nextInt(handsInBucket.size()); //random sampling
//        return handsInBucket.get(randomIndex);
//    }
//}