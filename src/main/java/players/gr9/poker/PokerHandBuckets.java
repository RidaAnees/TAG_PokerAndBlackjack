package players.gr9.poker;

import core.components.Deck;
import core.components.FrenchCard;
import games.poker.PokerGameState;
import players.gr9.LoggerUtility;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static games.poker.PokerGameState.PokerGamePhase.Preflop;

public class PokerHandBuckets {
    private static final Logger logger = LoggerUtility.getLogger();

    private final Map<String, List<List<FrenchCard>>> buckets;
    private final boolean considerHoleCards;

    public PokerHandBuckets(PokerGameState gameState, List<FrenchCard> holeCards, boolean considerHoleCards) {
        this.considerHoleCards = considerHoleCards;
        this.buckets = new HashMap<>();
        populateBuckets(gameState, holeCards);
    }

    //populates once s
    private void populateBuckets(PokerGameState gameState, List<FrenchCard> holeCards) {
        if (gameState.getGamePhase().equals(Preflop)) {
            return; // Skip the bucket population
        }
        Deck<FrenchCard> fullDeck = FrenchCard.generateDeck("Full Deck", core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        Deck<FrenchCard> allRemainingCards = fullDeck.copy();
        allRemainingCards.removeAll(gameState.getCommunityCards().getComponents());
//        if (!considerHoleCards) {
//            allRemainingCards.removeAll(holeCards);
//        }

        Deck<FrenchCard> communityCards = gameState.getCommunityCards();
        logger.warning("Community Cards: " + communityCards);

        buckets.clear();

        buckets.put("TPSK", generateTPSK(allRemainingCards, communityCards));
        logger.log(Level.WARNING,"TPSK: " + buckets);
        buckets.put("PPAB", generatePPAB(allRemainingCards, communityCards));
        logger.log(Level.WARNING,"PPAB: " + buckets);
        buckets.put("PPBB", generatePPBB(allRemainingCards, communityCards));
        buckets.put("TRifPair", generateTRifPair(allRemainingCards, communityCards));
        buckets.put("PF", generatePotentialFlush(allRemainingCards, communityCards));
        buckets.put("PS", generatePotentialStraights(allRemainingCards, communityCards));

        // Track all cards used in any of the above hands
        Set<String> usedPairs = new HashSet<>();
        for (List<List<FrenchCard>> handList : buckets.values()) {
            for (List<FrenchCard> pair : handList) {
                usedPairs.add(getCardPairKey(pair.get(0), pair.get(1)));
            }
        }

        // Build the "Other" bucket
        List<List<FrenchCard>> otherHands = new ArrayList<>();
        for (int i = 0; i < allRemainingCards.getSize(); i++) {
            for (int j = i + 1; j < allRemainingCards.getSize(); j++) {
                FrenchCard c1 = allRemainingCards.get(i);
                FrenchCard c2 = allRemainingCards.get(j);
                String key = getCardPairKey(c1, c2);
                if (!usedPairs.contains(key)) {
                    otherHands.add(Arrays.asList(c1, c2));
                }
            }
        }
        buckets.put("Other", otherHands);
    }

    private String getCardPairKey(FrenchCard c1, FrenchCard c2) {
        return c1.hashCode() < c2.hashCode()
                ? c1 + "-" + c2
                : c2 + "-" + c1;
    }

    public List<FrenchCard> getBucketHandsByIndex(int bucketIndex) {
        List<String> bucketNames = new ArrayList<>(buckets.keySet());
        if (bucketIndex < 0 || bucketIndex >= bucketNames.size()) {
            return Collections.emptyList();
        }
        String bucketName = bucketNames.get(bucketIndex);
        List<List<FrenchCard>> hands = buckets.get(bucketName);
        List<FrenchCard> pokerHands = new ArrayList<>();
        for (List<FrenchCard> hand : hands) {
            pokerHands.addAll(hand);
        }
        return pokerHands;
    }


    private static FrenchCard findTopCard(Deck<FrenchCard> board) {
        if ((board == null )|| (board.getSize()==0)) {
            logger.log(Level.WARNING, "null top card");
        }
        logger.log(Level.FINEST, "top card " + board.stream().max(Comparator.comparingInt(PokerHandBuckets::getCardValue)));
        return board.stream()
                .max(Comparator.comparingInt(PokerHandBuckets::getCardValue))
                .orElse(null);
    }

    private static FrenchCard findLowestCard(Deck<FrenchCard> board) {
        if ((board == null || board.getSize()==0)) {
            logger.log(Level.WARNING, "null lowest card");
            return null;
        }
        logger.log(Level.FINEST, "lowest card " + board.stream().min(Comparator.comparingInt(PokerHandBuckets::getCardValue)));
        return board.stream().min(Comparator.comparingInt(PokerHandBuckets::getCardValue)).orElse(null);
    }


    private static int getCardValue(FrenchCard card) {
        if (card == null) {
            return -1; // Return invalid value if card is null
        }
        return card.number; // Return card number as value
    }


    private static Map<FrenchCard.FrenchCardType, Integer> countCardRanks(Deck<FrenchCard> board) {
        Map<FrenchCard.FrenchCardType, Integer> rankCounts = new HashMap<>();
        for (FrenchCard card : board) {
            rankCounts.put(card.type, rankCounts.getOrDefault(card.type, 0) + 1);
        }
        return rankCounts;
    }

    /***
     * Buckets logic:
     * */
    private static List<List<FrenchCard>> generateTPSK(Deck<FrenchCard> allRemainingCards, Deck<FrenchCard> communityCards) {
        List<List<FrenchCard>> hands = new ArrayList<>();

        FrenchCard topCard = findTopCard(communityCards);
        int topCardValue = getCardValue(topCard);
        FrenchCard.FrenchCardType topCardType = FrenchCard.getTypeFromNumber(topCard.number);

        Set<FrenchCard.Suite> usedTopSuits = communityCards.stream()
                .filter(c -> getCardValue(c) == topCardValue)
                .map(c -> c.suite)
                .collect(Collectors.toSet());

        Set<String> seen = new HashSet<>();

        for (FrenchCard kicker : allRemainingCards) {
            int kickerValue = getCardValue(kicker);
            if (communityCards.contains(kicker)) continue;

            if (kickerValue <= 10 || kickerValue >= 14) continue;
            if (topCard.number == kicker.number) continue;

            // iterate through suites for top pair card
            for (FrenchCard.Suite s1 : FrenchCard.Suite.values()) {
                if (usedTopSuits.contains(s1)) continue;
                FrenchCard topPairCard = new FrenchCard(topCardType, s1, topCard.number);

                //check for the second kicker
                for (FrenchCard.Suite s2 : FrenchCard.Suite.values()) {
                    if (s1 == s2) continue;
                    FrenchCard kickerCard = new FrenchCard(kicker.type, s2, kicker.number);

                    //check the combination is valid
                    if (communityCards.contains(topPairCard) || communityCards.contains(kickerCard)) continue;
                    if (topPairCard.equals(kickerCard)) continue; // Avoid identical cards
                    if (!allRemainingCards.contains(topPairCard) || !allRemainingCards.contains(kickerCard)) continue;

                    String key = Stream.of(topPairCard, kickerCard)
                            .sorted(Comparator.comparing(FrenchCard::toString))
                            .map(FrenchCard::toString)
                            .collect(Collectors.joining(","));

                    if (seen.add(key)) {
                        hands.add(Arrays.asList(topPairCard, kickerCard));
                    }
                }
            }
        }
        logger.log(Level.FINE, "TPSK samples " + hands);

        return hands;
    }

    private static List<List<FrenchCard>> generatePPAB(Deck<FrenchCard> allRemainingCards, Deck<FrenchCard> communityCards) {
        List<List<FrenchCard>> hands = new ArrayList<>();
        FrenchCard topCard = findTopCard(communityCards);
        if (topCard == null) {
            return hands;
        }
        int topCardValue = getCardValue(topCard);
        // Group available cards by rank
        Map<FrenchCard.FrenchCardType, List<FrenchCard>> cardsByRank = new HashMap<>();
        for (FrenchCard card : allRemainingCards) {
            int cardValue = getCardValue(card);
            if (cardValue > topCardValue) {
                cardsByRank.computeIfAbsent(card.type, k -> new ArrayList<>()).add(card);
            }
        }
        // Generate all unique pairs of same-rank cards
        for (Map.Entry<FrenchCard.FrenchCardType, List<FrenchCard>> entry : cardsByRank.entrySet()) {
            List<FrenchCard> cardsOfSameRank = entry.getValue();
            for (int i = 0; i < cardsOfSameRank.size(); i++) {
                for (int j = i + 1; j < cardsOfSameRank.size(); j++) {
                    hands.add(Arrays.asList(cardsOfSameRank.get(i), cardsOfSameRank.get(j)));
                }
            }
        }
        logger.log(Level.FINE, "PPAB generated " + hands);
        return hands;
    }

    private static List<List<FrenchCard>> generatePPBB(Deck<FrenchCard> allRemainingCards, Deck<FrenchCard> communityCards) {
        List<List<FrenchCard>> hands = new ArrayList<>();
        FrenchCard lowestCard = findLowestCard(communityCards);
        if (lowestCard == null) {
            return hands;
        }

        int lowestCardValue = getCardValue(lowestCard);

        // Group by card rank (value), NOT by card type (suit)
        Map<Integer, List<FrenchCard>> cardsByValue = new HashMap<>();
        for (FrenchCard card : allRemainingCards) {
            int cardValue = getCardValue(card);
            if (cardValue < lowestCardValue) {
                cardsByValue.computeIfAbsent(cardValue, k -> new ArrayList<>()).add(card);
            }
        }

        // Only generate pairs of same-rank cards
        for (Map.Entry<Integer, List<FrenchCard>> entry : cardsByValue.entrySet()) {
            List<FrenchCard> sameRankCards = entry.getValue();
            for (int i = 0; i < sameRankCards.size(); i++) {
                for (int j = i + 1; j < sameRankCards.size(); j++) {
                    hands.add(Arrays.asList(sameRankCards.get(i), sameRankCards.get(j)));
                }
            }
        }

        logger.fine("PPBB generated " + hands);
        return hands;
    }


    //if there is a pair on the board already
    private static List<List<FrenchCard>> generateTRifPair(Deck<FrenchCard> allRemainingCards, Deck<FrenchCard> communityCards) {
        List<List<FrenchCard>> tripsHands = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Map<FrenchCard.FrenchCardType, Integer> cardRanks = countCardRanks(communityCards);

        for (Map.Entry<FrenchCard.FrenchCardType, Integer> entry : cardRanks.entrySet()) {
            FrenchCard.FrenchCardType rank = entry.getKey();
            int countOnBoard = entry.getValue();

            // Gather suits already used on the board for this rank
            Set<FrenchCard.Suite> usedSuits = communityCards.stream()
                    .filter(card -> card.type == rank)
                    .map(card -> card.suite)
                    .collect(Collectors.toSet());

            // If 2 cards of same rank on board → need 1 more to complete trips
            if (countOnBoard == 2) {
                for (FrenchCard.Suite s1 : FrenchCard.Suite.values()) {
                    if (usedSuits.contains(s1)) continue;

                    FrenchCard thirdTripCard = new FrenchCard(rank, s1);

                    for (FrenchCard kicker : allRemainingCards) {
                        if (communityCards.contains(kicker) || kicker.equals(thirdTripCard)) continue;
                        if (!allRemainingCards.contains(thirdTripCard) || !allRemainingCards.contains(kicker)) continue;

                        String key = Stream.of(thirdTripCard, kicker)
                                .sorted(Comparator.comparing(FrenchCard::toString))
                                .map(FrenchCard::toString)
                                .collect(Collectors.joining(","));

                        if (seen.add(key)) {
                            tripsHands.add(Arrays.asList(thirdTripCard, kicker));
                        }
                    }
                }
            }
        }
        logger.fine("Trips (if a pair on the board) generated: " + tripsHands);
        return tripsHands;
    }

    private static List<List<FrenchCard>> generatePotentialFlush(Deck<FrenchCard> allRemainingCards, Deck<FrenchCard> communityCards) {
        List<List<FrenchCard>> flushHands = new ArrayList<>();
        Map<FrenchCard.Suite, List<FrenchCard>> communitySuits = communityCards.stream()
                .collect(Collectors.groupingBy(c -> c.suite));
        for (FrenchCard.Suite suit : FrenchCard.Suite.values()) {
            List<FrenchCard> communitySuitCards = communitySuits.getOrDefault(suit, new ArrayList<>());
            if (communitySuitCards.size() >= 3) {
                List<FrenchCard> remainingCardsOfSuit = allRemainingCards.stream()
                        .filter(c -> c.suite == suit)
                        .collect(Collectors.toList());
                List<FrenchCard> potentialFlush = new ArrayList<>(communitySuitCards);
                potentialFlush.addAll(remainingCardsOfSuit);
                if (potentialFlush.size() >= 5) {
                    for (int i = 0; i < remainingCardsOfSuit.size(); i++) {
                        for (int j = i + 1; j < remainingCardsOfSuit.size(); j++) {
                            List<FrenchCard> flushHand = new ArrayList<>(communitySuitCards);
                            flushHand.add(remainingCardsOfSuit.get(i));
                            flushHand.add(remainingCardsOfSuit.get(j));
                            if (flushHand.size() == 5) {
                                flushHands.add(flushHand);
                            }
                        }
                    }
                }
            }
        }

        logger.fine("Potential flush: " + flushHands);
        return flushHands;
    }

    private static List<List<FrenchCard>> generatePotentialStraights(Deck<FrenchCard> allRemainingCards, Deck<FrenchCard> communityCards) {
        List<List<FrenchCard>> straightHands = new ArrayList<>();

        List<List<Integer>> possibleStraights = Arrays.asList(
                Arrays.asList(2, 3, 4, 5, 6),
                Arrays.asList(3, 4, 5, 6, 7),
                Arrays.asList(4, 5, 6, 7, 8),
                Arrays.asList(5, 6, 7, 8, 9),
                Arrays.asList(6, 7, 8, 9, 10),
                Arrays.asList(7, 8, 9, 10, 11),
                Arrays.asList(8, 9, 10, 11, 12),
                Arrays.asList(9, 10, 11, 12, 13),
                Arrays.asList(10, 11, 12, 13, 14),  // Ace-high
                Arrays.asList(14, 2, 3, 4, 5)      // Ace-low
        );
        for (List<Integer> straightSequence : possibleStraights) {
            List<FrenchCard> potentialStraight = new ArrayList<>();
            Set<Integer> missingRanks = new HashSet<>(straightSequence);

            for (FrenchCard card : communityCards) {
                if (missingRanks.contains(card.number)) {
                    potentialStraight.add(card);
                    missingRanks.remove(card.number);
                }
            }

            if (potentialStraight.size() < 5) {
                List<FrenchCard> remainingCards = allRemainingCards.stream()
                        .filter(card -> missingRanks.contains(card.number))
                        .collect(Collectors.toList());

                if (remainingCards.size() == 2) {
                    straightHands.add(remainingCards);
                }
            }
        }

        logger.fine("Potential straights: " + straightHands);
        return straightHands;
    }

}