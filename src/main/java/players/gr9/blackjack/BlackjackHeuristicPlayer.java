package players.gr9.blackjack;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.blackjack.BlackjackGameState;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import players.PlayerParameters;

import java.util.List;

public class BlackjackHeuristicPlayer extends AbstractPlayer {
//
    private int runningCount = 0;

    public BlackjackHeuristicPlayer(PlayerParameters params, String name) {
        super(params, name);
    }

    public BlackjackHeuristicPlayer() {
        super(null, "EVBasedBlackjackAgent");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> availableActions) {
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        int playerId = gameState.getCurrentPlayer();

        // Check if both Hit and Stand are valid actions
        boolean canHit = availableActions.contains(new Hit(playerId));
        boolean canStand = availableActions.contains(new Stand());

        // Estimate EV if Hit and Stand are available
        double evHit = canHit ? estimateHitEV(bjgs, playerId) : Double.NEGATIVE_INFINITY;
        double evStand = canStand ? estimateStandEV(bjgs, playerId) : Double.NEGATIVE_INFINITY;

        // Choose action with highest EV
        AbstractAction action;
        if (evHit > evStand && canHit) {
            action = new Hit(playerId);
        } else if (canStand) {
            action = new Stand();
        } else {
            throw new AssertionError("No valid actions available for player " + playerId);
        }

        // Ensure action is present in available actions
        if (!availableActions.contains(action)) {
            throw new AssertionError("Action played that was not in the list of available actions: " + action);
        }

        return action;
    }

    private double estimateHitEV(BlackjackGameState bjgs, int playerID) {
        Deck<FrenchCard> drawDeck = bjgs.getDrawDeck();
        List<FrenchCard> remainingCards = drawDeck.getComponents();
        int currentScore = (int) bjgs.getGameScore(playerID);
        double totalEV = 0;
        int count = 0;

        //gett heuristic score for the player based on the current game state
        double playerHeuristicScore = bjgs._getHeuristicScore(playerID);

        //adjust EV based on the running count (deck composition)
        double deckAdvantage = getDeckAdvantage(bjgs);

        // Count the occurrence of each card value to improve probability calculations
        int[] cardCounts = new int[11]; // Indices 1-10 for cards 1-10, and 10 for face cards
        for (FrenchCard card : remainingCards) {
            int cardValue = Math.min((int) card.number, 10); // face cards count as 10
            cardCounts[cardValue]++;
        }

        // Calculate the probability of hitting certain outcomes
        for (int cardValue = 1; cardValue <= 10; cardValue++) {
            int numCards = cardCounts[cardValue];
            if (numCards > 0) {
                double probability = (double) numCards / remainingCards.size();
                int newScore = currentScore + cardValue;

                double outcome;
                if (newScore > 21) {
                    outcome = -1.0; // Bust
                } else if (newScore == 21) {
                    outcome = 1.0; // Perfect hand
                } else if (newScore >= 17) {
                    outcome = 0.6; // Strong hand
                } else {
                    outcome = 0.3; // risky hand
                }
                totalEV += outcome * probability * playerHeuristicScore * deckAdvantage;
                count++;
            }
        }

        return count > 0 ? totalEV : -1;
    }

    private double getDeckAdvantage(BlackjackGameState bjgs) {
        // Using a simple card count approach (Hi lo) to estimate deck advantage
        int count = 0;
        Deck<FrenchCard> drawDeck = bjgs.getDrawDeck();
        List<FrenchCard> remainingCards = drawDeck.getComponents();

        for (FrenchCard card : remainingCards) {
            int cardValue = Math.min((int) card.number, 10); // face cards count as 10
            if (cardValue >= 2 && cardValue <= 6) {
                count++; // more low cards favor the player
            } else if (cardValue >= 10 || cardValue == 1) {
                count--; // high cards (10/face cards) favor the dealer
            }
        }

        return count / (double) remainingCards.size(); //return normalized count as advantage
    }

    private double estimateStandEV(BlackjackGameState bjgs, int playerID) {
        int myScore = (int) bjgs.getGameScore(playerID);
        int dealerId = bjgs.getDealerPlayer();
        int dealerScore = (int) bjgs.getGameScore(dealerId);

        // get heuristic score for the player
        double playerHeuristicScore = bjgs._getHeuristicScore(playerID);

        if (myScore > 21) {
            return -1.0; // Bust
        }

        // calculate dealer's outcome
        double dealerEV = estimateDealerEV(bjgs, dealerId);

        if (myScore == 21) {
            return 1.0; // Perfect hand, guaranteed win
        } else if (myScore > dealerScore && myScore <= 21) {
            return 0.7 * playerHeuristicScore;
        } else if (myScore == dealerScore) {
            return 0.5;
        } else if (myScore < dealerScore) {
            return 0.2;
        } else {
            return 0.0;
        }
    }

    private double estimateDealerEV(BlackjackGameState bjgs, int dealerID) {
        int dealerScore = (int) bjgs.getGameScore(dealerID);
        List<FrenchCard> remainingCards = bjgs.getDrawDeck().getComponents();

        double dealerEV = 0.0;

        while (dealerScore < 17) {
            int cardValue = Math.min((int) remainingCards.get(0).number, 10); //treat face cards as 10
            remainingCards.remove(0); // simulate the dealer drawing a card
            dealerScore += cardValue;

            if (dealerScore > 21) {
                return -1.0; //dealer busts
            } else if (dealerScore == 21) {
                return 1.0; //dealer has Blackjack
            }
        }

        //if dealer's score is 17 or more, they stand
        if (dealerScore >= 17) {
            return 0.5; // Neutral outcome, dealer stands
        }

        return dealerEV; // Default EV
    }

    private void updateCardCount(BlackjackGameState bjgs) {
        Deck<FrenchCard> drawDeck = bjgs.getDrawDeck();
        List<FrenchCard> remainingCards = drawDeck.getComponents();

        for (FrenchCard card : remainingCards) {
            int cardValue = Math.min((int) card.number, 10); // face cards count as 10
            // High-Low counting system
            if (cardValue >= 2 && cardValue <= 6) {
                runningCount++;
            } else if (cardValue >= 10 || cardValue == 1) {
                runningCount--;
            }
        }
    }

    private double calculateProbabilityOf21(int playerScore, BlackjackGameState bjgs) {
        int cardsNeeded = 21 - playerScore;
        Deck<FrenchCard> drawDeck = bjgs.getDrawDeck();
        List<FrenchCard> remainingCards = drawDeck.getComponents();

        if (cardsNeeded == 0) {
            return 1.0;
        }

        int countNeededCard = 0;
        for (FrenchCard card : remainingCards) {
            int cardValue = Math.min((int) card.number, 10); // face cards count as 10
            if (cardValue == cardsNeeded) {
                countNeededCard++;
            }
        }

        return (double) countNeededCard / remainingCards.size();
    }

    @Override
    public AbstractPlayer copy() {
        return new BlackjackHeuristicPlayer();
    }

    @Override
    public String toString() {
        return "EVBasedBlackjackAgent";
    }
}
