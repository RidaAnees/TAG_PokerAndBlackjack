package players.ISMCTS.poker;

import core.components.Deck;
import core.components.FrenchCard;
import games.poker.PokerGameState;
import players.ISMCTS.LoggerUtility;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static games.poker.PokerGameState.PokerGamePhase.Preflop;

public class PokerHandBuckets {
    private static final Logger logger = LoggerUtility.getLogger();

    private final Map<String, List<List<FrenchCard>>> buckets;
    private final boolean considerHoleCards;

    public PokerHandBuckets(PokerGameState gameState, List<FrenchCard> holeCards, boolean considerHoleCards) {
        this.considerHoleCards = considerHoleCards;
        this.buckets = new HashMap<>();
        logger.fine("Initializing PokerHandBuckets with considerHoleCards: \" + considerHoleCard");
        populateBuckets(gameState, holeCards);
    }

    //populates once s
    private void populateBuckets(PokerGameState gameState, List<FrenchCard> holeCards) {
        if (gameState.getGamePhase().equals(Preflop)) {
            return; // Skip the bucket population
        }
        Deck<FrenchCard> fullDeck = FrenchCard.generateDeck("Full Deck", core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        List<FrenchCard> allRemainingCards = new ArrayList<>(fullDeck.getComponents());
        allRemainingCards.removeAll(gameState.getCommunityCards().getComponents());

        if (!considerHoleCards) {
            allRemainingCards.removeAll(holeCards);
        }

        List<FrenchCard> communityCards = gameState.getCommunityCards().getComponents();
        logger.warning("Community Cards: " + communityCards);
        logger.fine("Remaining cards: " + allRemainingCards);

        buckets.put("TPSK", generateTPSK(allRemainingCards, communityCards));
        logger.finest("TPSK: "+ buckets);
        buckets.put("TPGK", generateTPGK(allRemainingCards, communityCards));
        logger.finest("TPGK: "+ buckets);
        buckets.put("TPWK", generateTPWK(allRemainingCards, communityCards));
        logger.finest("TPWK: "+ buckets);
        buckets.put("PPAB", generatePPAB(allRemainingCards, communityCards));
        logger.finest("PPAB: "+ buckets);
        buckets.put("PPBB", generatePPBB(allRemainingCards, communityCards));
        logger.finest("PPBB: "+ buckets);
        buckets.put("TR", generateTR(allRemainingCards, communityCards));
        logger.finest("TR: "+ buckets);

    }

    public List<List<FrenchCard>> getHandsInBucket(String bucketName) {
        return buckets.getOrDefault(bucketName, Collections.emptyList());
    }

    public Set<String> getBucketNames() {
        return buckets.keySet();
    }

    public int getTotalBuckets() {
        return buckets.size();
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
            pokerHands.addAll(hand);  // Add each card in the hand to pokerHands
        }

        return pokerHands;
    }


    /*Static utility methods*/
    private static int getCardValue(FrenchCard card) {
        if (card == null || card.getType() == null) {
            System.out.println("Warning: Card or Card type is null. Returning default value of -1.");
            return -1;  // Default value for invalid cards
        }
        return card.type.getNumber();
    }

    private static FrenchCard findTopCard(List<FrenchCard> board) {
        if (!(board == null || board.isEmpty())) {
            return board.stream().max(Comparator.comparingInt(PokerHandBuckets::getCardValue)).orElse(null);
        }
        return new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Clubs );
    }

    private static FrenchCard findLowestCard(List<FrenchCard> board) {
        if (!(board == null || board.isEmpty())) {
            return board.stream().min(Comparator.comparingInt(PokerHandBuckets::getCardValue)).orElse(null);
        }
        return null;
    }
    private static List<List<FrenchCard>> generateTPSK(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> hands = new ArrayList<>();
        FrenchCard topCard = findTopCard(communityCards);

        if (topCard == null) {
            return hands;
        }

        int topCardValue = getCardValue(topCard);

        for (FrenchCard kicker : allRemainingCards) {
            int kickerValue = getCardValue(kicker);

            // Example condition: kicker must be higher than top card and not a Queen
            if (kickerValue > topCardValue && kickerValue != 12) {
                for (FrenchCard.Suite s1 : FrenchCard.Suite.values()) {
                    for (FrenchCard.Suite s2 : FrenchCard.Suite.values()) {
                        if (s1 != s2) {
                            FrenchCard.FrenchCardType topCardType = FrenchCard.getTypeFromNumber(topCard.number);
                            FrenchCard.FrenchCardType kickerType = FrenchCard.getTypeFromNumber(kicker.number);

                            FrenchCard topCardCopy;
                            FrenchCard kickerCard;

                            // Use correct constructor depending on type
                            if (topCardType == FrenchCard.FrenchCardType.Number) {
                                topCardCopy = new FrenchCard(topCardType, s1, topCard.number);
                            } else {
                                topCardCopy = new FrenchCard(topCardType, s1);
                            }

                            if (kickerType == FrenchCard.FrenchCardType.Number) {
                                kickerCard = new FrenchCard(kickerType, s2, kicker.number);
                            } else {
                                kickerCard = new FrenchCard(kickerType, s2);
                            }

                            hands.add(Arrays.asList(topCardCopy, kickerCard));
                        }
                    }
                }
            }
        }
        return hands;
    }

    private static List<List<FrenchCard>> generateTPGK(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> hands = new ArrayList<>();
        FrenchCard topCard = findTopCard(communityCards);
        if (topCard == null) {
            // Handle the case where top card can't be determined, maybe skip this part or return an empty list
            return hands;
        }
        int topCardValue = getCardValue(topCard);

        for (FrenchCard kicker : allRemainingCards) {
            int kickerValue = getCardValue(kicker);
            if (kickerValue >= 6 && kickerValue < topCardValue) {
                for (FrenchCard.Suite s1 : FrenchCard.Suite.values()) {
                    for (FrenchCard.Suite s2 : FrenchCard.Suite.values()) {
                        if (s1 != s2) {
                            hands.add(Arrays.asList(
                                    new FrenchCard(topCard.type, s1),
                                    new FrenchCard(kicker.type, s2)
                            ));
                        }
                    }
                }
            }
        }
        return hands;
    }

    private static List<List<FrenchCard>> generateTPWK(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> hands = new ArrayList<>();
        FrenchCard topCard = findTopCard(communityCards);
        if (topCard == null) {
            // Handle the case where top card can't be determined, maybe skip this part or return an empty list
            return hands;
        }
        for (FrenchCard kicker : allRemainingCards) {
            int kickerValue = getCardValue(kicker);
            if (kickerValue < 6) {
                for (FrenchCard.Suite s1 : FrenchCard.Suite.values()) {
                    for (FrenchCard.Suite s2 : FrenchCard.Suite.values()) {
                        if (s1 != s2) {
                            hands.add(Arrays.asList(
                                    new FrenchCard(topCard.type, s1),
                                    new FrenchCard(kicker.type, s2)
                            ));
                        }
                    }
                }
            }
        }
        return hands;
    }

    private static List<List<FrenchCard>> generatePPAB(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> hands = new ArrayList<>();
        FrenchCard topCard = findTopCard(communityCards);
        if (topCard == null) {
            // Handle the case where top card can't be determined, maybe skip this part or return an empty list
            return hands;
        }
        int topCardValue = getCardValue(topCard);

        for (FrenchCard card : allRemainingCards) {
            int value = getCardValue(card);
            if (value > topCardValue) {
                for (FrenchCard.Suite s1 : FrenchCard.Suite.values()) {
                    for (FrenchCard.Suite s2 : FrenchCard.Suite.values()) {
                        if (s1.ordinal() < s2.ordinal()) {
                            hands.add(Arrays.asList(
                                    new FrenchCard(card.type, s1),
                                    new FrenchCard(card.type, s2)
                            ));
                        }
                    }
                }
            }
        }
        return hands;
    }

    private static List<List<FrenchCard>> generatePPBB(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> hands = new ArrayList<>();
        FrenchCard lowCard = findLowestCard(communityCards);
        if (lowCard == null) {
            // Handle the case where top card can't be determined, maybe skip this part or return an empty list
            return hands;
        }
        int lowCardValue = getCardValue(lowCard);

        for (FrenchCard card : allRemainingCards) {
            int value = getCardValue(card);
            if (value < lowCardValue) {
                for (FrenchCard.Suite s1 : FrenchCard.Suite.values()) {
                    for (FrenchCard.Suite s2 : FrenchCard.Suite.values()) {
                        if (s1.ordinal() < s2.ordinal()) {
                            hands.add(Arrays.asList(
                                    new FrenchCard(card.type, s1),
                                    new FrenchCard(card.type, s2)
                            ));
                        }
                    }
                }
            }
        }
        return hands;
    }

    private static List<List<FrenchCard>> generateTR(List<FrenchCard> allRemainingCards, List<FrenchCard> communityCards) {
        List<List<FrenchCard>> tripsHands = new ArrayList<>();
        Map<FrenchCard.FrenchCardType, Integer> cardRanks = countCardRanks(communityCards);

        for (Map.Entry<FrenchCard.FrenchCardType, Integer> entry : cardRanks.entrySet()) {
            if (entry.getValue() == 2 || entry.getValue() == 1) {
                FrenchCard.FrenchCardType rank = entry.getKey();
                List<FrenchCard.Suite> existingSuits = new ArrayList<>();
                for (FrenchCard card : communityCards) {
                    if (card.type == rank) {
                        existingSuits.add(card.suite);
                    }
                }

                for (FrenchCard.Suite suit1 : FrenchCard.Suite.values()) {
                    if (!existingSuits.contains(suit1)) {
                        for (FrenchCard card : allRemainingCards) {
                            if (!existingSuits.contains(card.suite)) {
                                List<FrenchCard> hand = Arrays.asList(
                                        new FrenchCard(rank, suit1),
                                        card
                                );
                                tripsHands.add(hand);
                            }
                        }
                    }
                }
            }
        }
        return tripsHands;
    }

    private static Map<FrenchCard.FrenchCardType, Integer> countCardRanks(List<FrenchCard> board) {
        Map<FrenchCard.FrenchCardType, Integer> rankCounts = new HashMap<>();
        for (FrenchCard card : board) {
            rankCounts.put(card.type, rankCounts.getOrDefault(card.type, 0) + 1);
        }
        return rankCounts;
    }
}
