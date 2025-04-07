package players.ISMCTS;

import core.components.Deck;
import core.components.FrenchCard;
import java.util.*;
import games.poker.PokerGameState;

import static java.util.stream.Collectors.groupingBy;

public class PokerHandBuckets {


    private Map<String, List<List<FrenchCard>>> buckets;
    private boolean considerHoleCards = false; // Default value

    private PokerHandBuckets(PokerGameState gameState, List<FrenchCard> holeCards, boolean considerHoleCards) {
        this.considerHoleCards = considerHoleCards;
        buckets = new HashMap<>();
        populateBuckets(gameState, holeCards); // Pass gameState to populateBuckets
    }

    private  PokerHandBuckets(PokerGameState gameState, List<FrenchCard> holeCards) {
        this(gameState, holeCards, false);
    }
    private void populateBuckets(PokerGameState gameState, List<FrenchCard> holeCards) {
        Deck<FrenchCard> communityCardsDeck = gameState.getCommunityCards().copy();
        List<FrenchCard> allRemainingCards = communityCardsDeck.getComponents();
        allRemainingCards.remove(communityCardsDeck);

        if (!considerHoleCards) {
            allRemainingCards.removeAll(holeCards);
        }

        buckets.put("TopPairStrongKicker", generateTPSK(allRemainingCards, communityCardsDeck.getComponents()));
        buckets.put("TopPairGoodKicker", generateTPGK(allRemainingCards, communityCardsDeck.getComponents()));
        buckets.put("TopPairWeakKicker", generateTPWK(allRemainingCards, communityCardsDeck.getComponents()));
        buckets.put("PocketPairAboveBoard", generatePPAB(allRemainingCards, communityCardsDeck.getComponents()));
        buckets.put("PocketPairBelowBoard", generatePPBB(allRemainingCards, communityCardsDeck.getComponents()));
        buckets.put("Trips", generateTR(allRemainingCards, communityCardsDeck.getComponents()));

        //TODO: Add more buckets
    }
    public List<List<FrenchCard>> getHandsInBucket(String bucketName) {
        return buckets.get(bucketName);
    }

    /**
     * This method is essential for comparing cards and determining which card has a higher rank.
     * It ensures that both numerical (2,3,4...9) and face cards (A, J, Q, K) can be treated consistently in comparisons
     * @param card
     * @return a number
     */
    private static int getCardValue(FrenchCard card) {
        if (card != null) {
            if (card.type == FrenchCard.FrenchCardType.Number) { //if card is A, J, Q, K
                return card.number;
            } else { // if card is a number card
                return card.type.getNumber();
            }
        } else {
            throw new IllegalArgumentException("Card cannot be null");
        }
    }


    /**
     * Method to find the top card on the board (just the community cards)
     * eg: turn board has 4 cards, return the top card amongst the 4
     * @param board
     * @return
     */
    private static FrenchCard findTopCard(List<FrenchCard> board) {
        if (board == null) {
            throw new IllegalArgumentException("Board cannot be null");
        }
        if (board.isEmpty()) {
            return null; // Return null if the board is empty.
        }

        FrenchCard topCard = board.get(0); // Initialize with the first card.

        // Start the loop from the second card (index 1) to avoid comparing the first card with itself.
        for (int i = 1; i < board.size(); i++) {
            FrenchCard card = board.get(i);
            if (getCardValue(card) > getCardValue(topCard)) {
                topCard = card;
            }
        }
        return topCard;
    }


    /**
     * In the postflop, turn and river rounds; returns the top 2 cards on the board
     * @param board
     * @return
     */
    private static FrenchCard[] findTopTwoCards(List<FrenchCard> board){
        if (board ==null || board.size() < 3 ) {
            throw new IllegalArgumentException("Board must have at least 3 cards");
        }
        FrenchCard topCard = null;
        FrenchCard secondTopCard = null;

        for (FrenchCard card : board) {
            if (topCard == null || getCardValue(card) > getCardValue(topCard)) {
                secondTopCard = topCard;
                topCard = card;
            } else if (secondTopCard == null || getCardValue(card) > getCardValue(secondTopCard)) {
                secondTopCard = card;
            }
        }
        return new FrenchCard[]{topCard, secondTopCard};
    }

    private static FrenchCard findLowestCard(List<FrenchCard> board){
        if (board == null) {
            throw new IllegalArgumentException("Board cannot be null");
        }
        if (board.isEmpty()) {
            return null; // Return null if the board is empty.
        }

        FrenchCard lowestCard = board.get(0); // Initialize with the first card.

        // Start the loop from the second card (index 1) to avoid comparing the first card with itself.
        for (int i = 1; i < board.size(); i++) {
            FrenchCard card = board.get(i);
            if (getCardValue(card) < getCardValue(lowestCard)) {
                lowestCard = card;
            }
        }
        return lowestCard;
    }


    /**
     *
     * Top pair strong kicker; pair with the highest ranking card, accompanied by the best possible side card
     * eg: If the board (at turn) is K, J, 6, 5 then having K,A means top pair (KK) and best kicker (A)
     * Note the ordinal check condition; for each rank, without the ordinal check, you'd have (2^4) 16 combinations
     * however, with the oridinal check, total number of suit combinations is 4 choose 2, which is 6 combinations, thus preventing repeat card pairs
     * eg: if [6H, 7D] is valid then [7D,6H] is not; order does not matter.
     * @param allRemainingCards
     * @param communityCards
     * @return
     */
    private static List<List<FrenchCard>> generateTPSK(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> tpskHands = new ArrayList<>();

        FrenchCard topCard = findTopCard(communityCards);
        int topCardValue = getCardValue(topCard);

        // Generate hands with higher-ranking kickers
        for (FrenchCard kickerType : allRemainingCards) {
            int kickerValue = kickerType.number;
            if (kickerValue > topCardValue) {
                for (FrenchCard.Suite suit1 : FrenchCard.Suite.values()) {
                    for (FrenchCard.Suite suit2 : FrenchCard.Suite.values()) {
                        if ((suit1.ordinal() < suit2.ordinal()) && (suit1 != topCard.suite)) { // Exclude the top card when generating pair
                            List<FrenchCard> hand = new ArrayList<>();
                            hand.add(new FrenchCard(topCard.type, suit1));
                            hand.add(new FrenchCard(kickerType.type, suit2));
                            tpskHands.add(hand);
                        }
                    }
                }
            }
        }
        return tpskHands;
    }


    private static List<List<FrenchCard>> generateTPGK(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> tpgkHands = new ArrayList<>();

        FrenchCard topCard = findTopCard(communityCards);
        int topCardValue = getCardValue(topCard);

        // Generate hands with good-ranking kickers
        for (FrenchCard kickerType : allRemainingCards) {
            int kickerValue = kickerType.number;
            if (kickerValue < topCardValue & kickerValue >= 9){
                for (FrenchCard.Suite suit1 : FrenchCard.Suite.values()) {
                    for (FrenchCard.Suite suit2 : FrenchCard.Suite.values()) {
                        if ((suit1.ordinal() < suit2.ordinal()) && (suit1 != topCard.suite )){ // Exclude the top card's suit
                            List<FrenchCard> hand = new ArrayList<>();
                            hand.add(new FrenchCard(topCard.type, suit1));
                            hand.add(new FrenchCard(kickerType.type, suit2));
                            tpgkHands.add(hand);
                        }
                    }
                }
            }
        }
        return tpgkHands;
    }


    private static List<List<FrenchCard>> generateTPWK(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> tpwkHands = new ArrayList<>();

        FrenchCard topCard = findTopCard(communityCards);
        int topCardValue = getCardValue(topCard);

        // Generate hands with good-ranking kickers
        for (FrenchCard kickerType : allRemainingCards) {
            int kickerValue = kickerType.number;
            if (kickerValue < 9){
                for (FrenchCard.Suite suit1 : FrenchCard.Suite.values()) {
                    for (FrenchCard.Suite suit2 : FrenchCard.Suite.values()) {
                        if ((suit1.ordinal() < suit2.ordinal()) && (suit1 != topCard.suite)) {
                            List<FrenchCard> hand = new ArrayList<>();
                            hand.add(new FrenchCard(topCard.type, suit1));
                            hand.add(new FrenchCard(kickerType.type, suit2));
                            tpwkHands.add(hand);
                        }
                    }
                }
            }
        }
        return tpwkHands;
    }

    private static List<List<FrenchCard>> generatePPAB(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> ppabHands = new ArrayList<>();

        FrenchCard topCard = findTopCard(communityCards);
        int topCardValue = getCardValue(topCard); //find top card on board

        for (FrenchCard nextType : allRemainingCards) {
            int nextValue = nextType.number;
            if (nextValue > topCardValue){ //pocket pair greater than top card on the board
                for (FrenchCard.Suite suit1 : FrenchCard.Suite.values()) {
                    for (FrenchCard.Suite suit2 : FrenchCard.Suite.values()) {
                        if ((suit1.ordinal() < suit2.ordinal()) && (suit1 != suit2)) {
                            List<FrenchCard> hand = new ArrayList<>();
                            hand.add(new FrenchCard(nextType.type, suit1));
                            hand.add(new FrenchCard(nextType.type, suit2));
                            ppabHands.add(hand);
                        }
                    }
                }
            }
        }
        return ppabHands;
    }


    private static List<List<FrenchCard>> generatePPBB(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> ppbbHands = new ArrayList<>();

        FrenchCard lowestCard = findLowestCard(communityCards);
        int topCardValue = getCardValue(lowestCard); //find top card on board

        for (FrenchCard nextType : allRemainingCards) {
            int nextValue = nextType.number;
            if (nextValue < topCardValue){ //pocket pair less than top card on the board
                for (FrenchCard.Suite suit1 : FrenchCard.Suite.values()) {
                    for (FrenchCard.Suite suit2 : FrenchCard.Suite.values()) {
                        //a card of same rank (eg:9) cant have same suit twice (eg: 9h is unique)
                        //also if [6H,6D] is valid then [6D,6H] isnt and so prevents duplicate hands
                        if ((suit1.ordinal() < suit2.ordinal()) && (suit1 != suit2)) {
                            List<FrenchCard> hand = new ArrayList<>();
                            hand.add(new FrenchCard(nextType.type, suit1));
                            hand.add(new FrenchCard(nextType.type, suit2));
                            ppbbHands.add(hand);
                        }
                    }
                }
            }
        }
        return ppbbHands;
    }

    private static List<List<FrenchCard>> generateTR(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> tripsHands = new ArrayList<>();
        Map<FrenchCard.FrenchCardType, Integer> cardRanks = countCardRanks(communityCards);

        List<FrenchCard.FrenchCardType> pairRanks = new ArrayList<>();
        List<FrenchCard.FrenchCardType> singleRanks = new ArrayList<>();

        // Separate ranks into pairs and single cards
        for (Map.Entry<FrenchCard.FrenchCardType, Integer> entry : cardRanks.entrySet()) {
            if (entry.getValue() == 2) {
                pairRanks.add(entry.getKey());
            } else if (entry.getValue() == 1) {
                singleRanks.add(entry.getKey());
            }
        }

        // Generate trips hands for each pair rank found
        for (FrenchCard.FrenchCardType pairRank : pairRanks) {
            List<FrenchCard.Suite> boardSuits = new ArrayList<>();
            for (FrenchCard card : communityCards) {
                if (card.type == pairRank) {
                    boardSuits.add(card.suite);
                }
            }

            // Generate trips hands by adding the third card of the pair rank
            for (FrenchCard.Suite suit : FrenchCard.Suite.values()) {
                if (!boardSuits.contains(suit)) {
                    for (FrenchCard card : allRemainingCards) {
                        List<FrenchCard> hand = new ArrayList<>();
                        hand.add(new FrenchCard(pairRank, suit)); // Add the trip card (pairRank + suit)
                        hand.add(card); // Add any card from the remaining cards
                        tripsHands.add(hand);
                    }
                }
            }
        }

        // Generate pocket pairs from single ranks (e.g., 2 of same rank but not yet a trip)
        for (FrenchCard.FrenchCardType singleRank : singleRanks) {
            // Check for possible pairs using all remaining cards, excluding the community cards
            for (FrenchCard.Suite suit1 : FrenchCard.Suite.values()) {
                for (FrenchCard.Suite suit2 : FrenchCard.Suite.values()) {
                    if (suit1 != suit2) { // Ensure that the two suits are different
                        List<FrenchCard> hand = new ArrayList<>();
                        hand.add(new FrenchCard(singleRank, suit1)); // Add the first card of the pocket pair
                        hand.add(new FrenchCard(singleRank, suit2)); // Add the second card of the pocket pair
                        tripsHands.add(hand); // Add the pair to the list
                    }
                }
            }
        }

        return tripsHands;
    }


    private static Map<FrenchCard.FrenchCardType, Integer> countCardRanks(List<FrenchCard> board) {
        Map<FrenchCard.FrenchCardType, Integer> rankCounts = new HashMap<>();
        if (board != null) {
            for (FrenchCard card : board) {
                rankCounts.put(card.type, rankCounts.getOrDefault(card.type, 0) + 1);
            }
        }
        return rankCounts;
    }


    private static boolean allUniqueRanks (List<FrenchCard> board) {
        boolean allUnique = true;
        Map<FrenchCard.FrenchCardType, Integer> rankCounts = countCardRanks(board);
        for (int count : rankCounts.values()) {
            if (count != 1) {
                allUnique =false;
            }
        }
        return allUnique;
    }

    private static Map<FrenchCard.Suite, Integer> countCardSuits(List<FrenchCard> board) {
        Map<FrenchCard.Suite, Integer> suitCounts = new HashMap<>();
        if (board != null) {
            for (FrenchCard card : board) {
                suitCounts.put(card.suite, suitCounts.getOrDefault(card.suite, 0) + 1);
            }
        }
        return suitCounts;
    }

    private static boolean allUniqueSuites (List<FrenchCard> board) {
        boolean allUnique = true;
        Map<FrenchCard.Suite, Integer> suiteCounts = countCardSuits(board);
        for (int count : suiteCounts.values()) {
            if (count != 1) {
                allUnique =false;
            }
        }
        return allUnique;
    }

    private Set<String> getBucketNames() {
        return buckets.keySet();
    }

//    private void updateBuckets(List<FrenchCard> board) {
//        buckets.put("TopPairStrongKicker", generateTPSK(board));
//        buckets.put("TopPairWeakKicker", generateTPWK(board));
//    }
}