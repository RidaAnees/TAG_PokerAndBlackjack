package players.gr9.poker;

import core.AbstractPlayer;
import core.components.Deck;
import core.components.FrenchCard;
import games.poker.PokerGameState;
import players.gr9.LoggerUtility;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for sampling poker hands for players based on style-specific belief distributions.
 */
public class PokerHandSampler {
    private static final Logger logger = LoggerUtility.getLogger();

    private final int numSamples = 50;  // Total number of hands to sample
    private final int maxNumSamplesEach = 10; // Maximum number of samples from each bucket
    private final PokerGameState state;
    private int numPlayers;
    private PokerHandBuckets handBuckets;
    private BeliefDistribution allProbabilities;


    // Constructor to initialize the PokerHandSampler with necessary parameters
    public PokerHandSampler(PokerGameState gameState, int numPlayers, PokerHandBuckets handBuckets, BeliefDistribution allProbabilities) {
        this.state = gameState;
        this.numPlayers = numPlayers;
        this.handBuckets = handBuckets;
        this.allProbabilities = allProbabilities;
    }

    // Method to sample opponent cards based on the derived style and hand probabilities
    public List<List<FrenchCard>> sampleOpponentCards(PokerGameState state, int opponentIdx) {
        String styleStr = deriveStyle(state, opponentIdx);
        logger.log(Level.WARNING, "Derived style for opponent {0}: {1}", new Object[]{opponentIdx, styleStr});
        BeliefDistribution.BoardAndPlayStyle style = BeliefDistribution.BoardAndPlayStyle.valueOf(styleStr);
        logger.log(Level.WARNING, "Sampling hands for style: {0}", style);

        double[] probs = new double[BeliefDistribution.PokerHandBucket.values().length];

        // Get probabilities for each poker hand bucket
        for (int i = 0; i < probs.length; i++) {
            BeliefDistribution.PokerHandBucket bucket = BeliefDistribution.PokerHandBucket.values()[i];
            double rawProb = allProbabilities.getProbDistr(bucket, style);
            probs[i] = Math.max(0, rawProb); // Ensuring no negative probabilities
        }

        // Initialize the final list to hold all sampled hands
        List<List<FrenchCard>> allSampledHands = new ArrayList<>();

        // Sample pairs (or sets) of hands from each bucket
        for (int i = 0; i < probs.length; i++) {
            int count = (int) Math.round(probs[i] * numSamples);

            // Add at least one hand from the bucket if its probability is greater than zero
            if (count == 0 && probs[i] > 0) {
                count = 1;
            }

            logger.log(Level.WARNING, "Sampling {0} hands from bucket {1}", new Object[]{count, i});

            // Sample pairs (or sets) from the current bucket
            List<List<FrenchCard>> sampledPairs = sampleBucketPairs(i, count);

            // Accumulate the sampled hands
            allSampledHands.addAll(sampledPairs);

            // Log the sampled pairs from this bucket
            logger.log(Level.WARNING, "Sampled hands from bucket {0}: {1}", new Object[]{i, sampledPairs});
        }

        // Log the total number of hands sampled
        logger.log(Level.WARNING, "Total hands sampled: {0} for player {1}", new Object[]{allSampledHands.size(), opponentIdx});

        return allSampledHands; // Return the complete list of sampled hands
    }


    // Helper method to sample pairs of hands from a specific bucket
    private List<List<FrenchCard>> sampleBucketPairs(int bucketIndex, int numSamples) {
        if (handBuckets == null) {
            logger.log(Level.WARNING, "handBuckets is null!");
            return new ArrayList<>();
        }

        // Get the hands from the specified bucket
        List<FrenchCard> bucketCards = handBuckets.getBucketHandsByIndex(bucketIndex);
        List<List<FrenchCard>> sampledPairs = new ArrayList<>();

        if (bucketCards.size() < 2) {
            logger.log(Level.WARNING, "Bucket {0} does not have enough cards to form pairs.", bucketIndex);
            return sampledPairs; // No pairs can be formed if there aren't at least two cards
        }

        Random rand = new Random();

        // Sample the requested number of pairs from the bucket
        for (int i = 0; i < numSamples; i++) {
            // Randomly pick two distinct cards from the bucket
            int card1Idx = rand.nextInt(bucketCards.size());
            int card2Idx;
            do {
                card2Idx = rand.nextInt(bucketCards.size());
            } while (card2Idx == card1Idx); // Ensure that card1Idx and card2Idx are distinct

            // Create a pair and add to the result list
            List<FrenchCard> pair = Arrays.asList(bucketCards.get(card1Idx), bucketCards.get(card2Idx));
            sampledPairs.add(pair);
        }

        logger.log(Level.WARNING, "Sampled {0} pairs from bucket {1}", new Object[]{numSamples, bucketIndex});
        return sampledPairs;
    }

    // Method to derive the opponent's playing style based on the board and aggression
    public String deriveStyle(PokerGameState state, int playerIdx) {
        boolean wetBoard = isBoardWet(state.getCommunityCards());
        boolean isAggressive = isAggressive(state, playerIdx);
        String style = wetBoard ?
                (isAggressive ? "WET_AGGRESSIVE" : "WET_PASSIVE") :
                (isAggressive ? "DRY_AGGRESSIVE" : "DRY_PASSIVE");

        logger.log(Level.WARNING, "Derived style: {0} (wetBoard={1}, aggressive={2})", new Object[]{style, wetBoard, isAggressive});
        return style;
    }

    // Helper method to check if the community cards form a "wet" board (flush or straight draw)
    private boolean isBoardWet(Deck<FrenchCard> communityCards) {
        if (communityCards.getSize() < 3) return false;
        List<FrenchCard> sorted = new ArrayList<>(communityCards.getComponents());
        sorted.sort(Comparator.comparingInt(card -> card.number));
        int maxGap = 0;
        for (int i = 0; i < sorted.size() - 1; i++) {
            maxGap = Math.max(maxGap, sorted.get(i + 1).number - sorted.get(i).number);
        }
        Map<FrenchCard.Suite, Integer> suitCounts = new HashMap<>();
        for (FrenchCard card : sorted) {
            suitCounts.merge(card.suite, 1, Integer::sum);
        }
        boolean hasFlushDraw = suitCounts.values().stream().anyMatch(v -> v >= 3);
        boolean hasStraightDraw = maxGap <= 4;

        logger.log(Level.FINE, "Board wetness check: flushDraw={0}, straightDraw={1}", new Object[]{hasFlushDraw, hasStraightDraw});
        return hasFlushDraw || hasStraightDraw;
    }

    // Helper method to check if the player is aggressive
    private boolean isAggressive(PokerGameState state, int playerIdx) {
        boolean result = state.isPlayerAggressive(playerIdx);
        logger.log(Level.FINEST, "Player {0} aggression: {1}", new Object[]{playerIdx, result});
        return result;
    }
}
