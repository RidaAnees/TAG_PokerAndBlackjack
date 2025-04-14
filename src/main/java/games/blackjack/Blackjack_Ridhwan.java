package games.blackjack;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import core.CoreConstants.GameResult;

public class Blackjack_Ridhwan implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (!(gs instanceof BlackjackGameState)) {
            throw new IllegalArgumentException("Expected BlackjackGameState");
        }

        BlackjackGameState bjgs = (BlackjackGameState) gs;

        GameResult result = bjgs.getPlayerResults()[playerId];

        // Game over? Return a big win/loss score
        if (result == GameResult.WIN_GAME) return 100.0;
        if (result == GameResult.LOSE_GAME) return -100.0;
        if (result == GameResult.DRAW_GAME) return 0.0;

        // Otherwise, return their current hand value (up to 21)
        double score = bjgs.calculatePoints(playerId);
        if (score > ((BlackjackParameters) gs.getGameParameters()).winScore) {
            return -50.0; // Bust
        }

        return score; // The closer to 21, the better
    }
}