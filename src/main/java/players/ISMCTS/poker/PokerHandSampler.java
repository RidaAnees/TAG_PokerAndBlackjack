package players.ISMCTS.poker;

import core.components.Deck;
import core.components.FrenchCard;
import games.poker.PokerGameState;
import players.ISMCTS.LoggerUtility;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class responsible for sampling poker hands for players based on style-specific belief distributions.
 */
public class PokerHandSampler {
    private static final Logger logger = LoggerUtility.getLogger();

    private final int numSamples = 50;
    private final int maxNumSamplesEach = 10;
    private final PokerGameState state;
    private int numPlayers;
    private PokerHandBuckets handBuckets;
    private BeliefDistribution allProbabilities;


    //given the number of players, handbuckets and probability for each bucket, we can sample from each bucket:
    public PokerHandSampler(PokerGameState gameState, int numPlayers, PokerHandBuckets handBuckets, BeliefDistribution allProbabilities) {
        this.state = gameState;
        this.numPlayers = numPlayers;
        this.handBuckets = handBuckets;
        this.allProbabilities = allProbabilities;
    }


    public List<FrenchCard> sampleOpponentCards(PokerGameState state, int opponentIdx) {
        // Derive the opponent's playing style based on the game state
        String styleStr = deriveStyle(state, opponentIdx);
        logger.log(Level.FINE, "Derived style for opponent {0}: {1}", new Object[]{opponentIdx, styleStr});

        // Convert the style string to a BoardAndPlayStyle
        BeliefDistribution.BoardAndPlayStyle style = BeliefDistribution.BoardAndPlayStyle.valueOf(styleStr);
        logger.log(Level.FINE, "Sampling hands for style: {0}", style);

        // Prepare an array to hold probabilities for each hand bucket
        double[] probs = new double[BeliefDistribution.PokerHandBucket.values().length];

        // Get probabilities for each poker hand bucket
        for (int i = 0; i < probs.length; i++) {
            BeliefDistribution.PokerHandBucket bucket = BeliefDistribution.PokerHandBucket.values()[i];
            double rawProb = allProbabilities.getProbDistr(bucket, style);
            probs[i] = Math.max(0, rawProb);
            //logger.log(Level.FINER, "Raw prob for bucket {0}: {1}", new Object[]{bucket, rawProb});
        }

        // List to store the sampled hands
        List<FrenchCard> result = new ArrayList<>();

        // Sample hands based on the probabilities for each bucket
        for (int i = 0; i < probs.length; i++) {
            int count = (int) Math.round(probs[i] * numSamples);
            logger.log(Level.FINER, "Sampling {0} hands from bucket {1}", new Object[]{count, i});
            result.addAll(sampleBucketHands(i, count));
        }

        logger.log(Level.FINE, "Total hands sampled: {0}", result.size());
        return result;
    }

    // Helper method to sample hands from a specific bucket
    private List<FrenchCard> sampleBucketHands(int bucketIndex, int numSamples) {
        if (handBuckets == null) {
            logger.warning("handBuckets is null!");
            return new ArrayList<>();
        }

        // Get the hands from the specified bucket
        List<FrenchCard> bucketHands = handBuckets.getBucketHandsByIndex(bucketIndex);
        List<FrenchCard> sampledHands = new ArrayList<>();

        if (bucketHands.isEmpty()) {
            logger.log(Level.FINE, "Bucket {0} is empty.", bucketIndex);
            return sampledHands; // return empty list if the bucket has no hands
        }

        // Determine the sample size, limiting it to maxNumSamplesEach if necessary
        int sampleSize = Math.min(Math.min(bucketHands.size(), numSamples), maxNumSamplesEach);
        if (bucketHands.size() <= sampleSize) {
            logger.log(Level.FINE, "Bucket {0} has <= sampleSize ({1}) hands, returning all.", new Object[]{bucketIndex, sampleSize});
            return new ArrayList<>(bucketHands);
        }

        // Shuffle the hands and select the desired sample
        Collections.shuffle(bucketHands, new Random());
        List<FrenchCard> selected = new ArrayList<>(bucketHands.subList(0, sampleSize));
        logger.log(Level.FINE, "Sampled {0} hands from bucket {1}", new Object[]{sampleSize, bucketIndex});

        return selected;
    }

    public String deriveStyle(PokerGameState state, int playerIdx) {
        boolean wetBoard = isBoardWet(state.getCommunityCards());
        boolean isAggressive = isAggressive(state, playerIdx);
        String style = wetBoard ?
                (isAggressive ? "WET_AGGRESSIVE" : "WET_PASSIVE") :
                (isAggressive ? "DRY_AGGRESSIVE" : "DRY_PASSIVE");
        logger.log(Level.FINE, "Derived style: {0} (wetBoard={1}, aggressive={2})", new Object[]{style, wetBoard, isAggressive});
        return style;
    }

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
        logger.log(Level.FINEST, "Board wetness check: flushDraw={0}, straightDraw={1}", new Object[]{hasFlushDraw, hasStraightDraw});
        return hasFlushDraw || hasStraightDraw;
    }

    private boolean isAggressive(PokerGameState state, int playerIdx) {
        boolean result = state.isPlayerAggressive(playerIdx);
        logger.log(Level.FINEST, "Player {0} aggression: {1}", new Object[]{playerIdx, result});
        return result;
    }


}
