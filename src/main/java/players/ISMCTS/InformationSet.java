package players.ISMCTS;

import games.poker.PokerGameState;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.Counter;

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

    public int getPlayerId() {
        return gameState.getCurrentPlayer();
    }

    public Deck<FrenchCard> getPlayerHand() {
        return gameState.getPlayerDecks().get(getPlayerId());
    }

    public Counter getPlayerMoney() {
        return gameState.getPlayerMoney()[getPlayerId()];
    }

    public Counter getPlayerBet() {
        return gameState.getPlayerBet()[getPlayerId()];
    }


    // TODO: add hand evaluation and bucketing logic and sample from buckets

    @Override
    public String toString() {
        return "InformationSet{" +
                "playerId=" + getPlayerId() +
                ", hand=" + getPlayerHand() +
                ", money=" + getPlayerMoney().getValue() +
                ", bet=" + getPlayerBet().getValue() +
                '}';
    }
}
