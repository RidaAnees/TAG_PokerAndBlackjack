package players.ISMCTS;

import games.poker.PokerGameState;

/**
 * InformationSet as a Wrapper for PokerGameState, using player-specific copy.
 */
public class InformationSet {

    public PokerGameState gameState;

    public InformationSet(PokerGameState gameState, int playerId) {
        this.gameState = (PokerGameState) gameState.copy(playerId); // Use player-specific copy
    }

    public PokerGameState getGameState() {
        return gameState;
    }

    @Override
    public String toString() {
        return "InformationSet{" +
                "gameState=" + gameState +
                '}';
    }

    public Integer getPlayerId() {
        return gameState.getCurrentPlayer();
    }
}