package players.gr9.poker.tests;

import core.CoreConstants;
import core.components.FrenchCard;
import core.components.Deck;
import games.poker.PokerGameState;
import players.gr9.LoggerUtility;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestRandomSampling {

    private static final Logger logger = LoggerUtility.getLogger();
    private final int numSamples;  // Instance variable

    // Constructor
    public TestRandomSampling(int numSamples) {
        this.numSamples = numSamples;
    }

    // Method to sample hands - now non-static
    public List<List<FrenchCard>> sampleRandomOpponentHands(PokerGameState state, int opponentIdx) {
        Set<FrenchCard> knownCards = new HashSet<>();
        knownCards.addAll(state.getCommunityCards().getComponents());

        // Add the other players' hole cards to known cards
        for (int i = 0; i < state.getNPlayers(); i++) {
            if (i != opponentIdx) {
                knownCards.addAll(state.getPlayerCards(i));
            }
        }

        // Create full deck and remove known cards
        Deck<FrenchCard> deck = FrenchCard.generateDeck("full", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        List<FrenchCard> fullDeck = new ArrayList<>(deck.getComponents());
        fullDeck.removeAll(knownCards);

        List<List<FrenchCard>> randomHands = new ArrayList<>();
        Random rand = new Random();

        // Sample random hands
        for (int i = 0; i < numSamples && fullDeck.size() >= 2; i++) {
            Collections.shuffle(fullDeck, rand);

            // Take and remove the first two cards
            FrenchCard card1 = fullDeck.remove(0);
            FrenchCard card2 = fullDeck.remove(0);

            randomHands.add(Arrays.asList(card1, card2));
        }

        logger.log(Level.INFO, "Sampled {0} random hands for player {1}", new Object[]{randomHands.size(), opponentIdx});
        return randomHands;
    }

}
