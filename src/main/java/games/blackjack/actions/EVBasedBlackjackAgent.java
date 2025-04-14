package games.blackjack.actions;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.blackjack.BlackjackGameState;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import players.PlayerParameters;

import java.util.List;

public class EVBasedBlackjackAgent extends AbstractPlayer {

    public EVBasedBlackjackAgent(PlayerParameters params, String name) {
        super(params, name);
    }

    public EVBasedBlackjackAgent() {
        super(null, "EVBasedBlackjackAgent");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> availableActions) {
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        int playerId = gameState.getCurrentPlayer();

        double evHit = estimateHitEV(bjgs, playerId);
        double evStand = estimateStandEV(bjgs, playerId);

        return evHit > evStand ? new Hit(playerId) : new Stand();
    }

    private double estimateHitEV(BlackjackGameState bjgs, int playerID) {
        PartialObservableDeck<FrenchCard> drawDeck = (PartialObservableDeck<FrenchCard>) bjgs.getDrawDeck();
        List<FrenchCard> remainingCards = drawDeck.getComponents();

        int currentScore = (int) bjgs.getGameScore(playerID);
        double totalEV = 0;
        int count = 0;

        for (FrenchCard card : remainingCards) {
            int cardValue = Math.min((int) card.getNumber(), 10);
            int newScore = currentScore + cardValue;

            double outcome;
            if (newScore > 21) {
                outcome = -1.0; // bust
            } else if (newScore == 21) {
                outcome = 1.0; // perfect
            } else if (newScore >= 17) {
                outcome = 0.5; // strong
            } else {
                outcome = 0.2; // risky
            }

            totalEV += outcome;
            count++;
        }

        return count > 0 ? totalEV / count : -1;
    }

    private double estimateStandEV(BlackjackGameState bjgs, int playerID) {
        int myScore = (int) bjgs.getGameScore(playerID);
        int dealerId = bjgs.getDealerPlayer();
        int highestOpponentScore = 0;

        for (int i = 0; i < bjgs.getNPlayers(); i++) {
            if (i != playerID && i != dealerId && !bjgs.isPlayerEliminated(i)) {
                int score = (int) bjgs.getGameScore(i);
                if (score <= 21) {
                    highestOpponentScore = Math.max(highestOpponentScore, score);
                }
            }
        }

        if (myScore < highestOpponentScore) {
            return 0.1; // very low EV if we know we’ll lose standing
        }

        if (myScore > 21) return -1.0;
        if (myScore == 21) return 1.0;
        if (myScore >= 18) return 0.6;
        if (myScore >= 15) return 0.3;
        return 0.1;
    }

    @Override
    public AbstractPlayer copy() {
        return new EVBasedBlackjackAgent();
    }

    @Override
    public String toString() {
        return "EVBasedBlackjackAgent";
    }
}
