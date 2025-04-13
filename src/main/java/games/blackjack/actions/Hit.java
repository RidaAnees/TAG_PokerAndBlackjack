package games.blackjack.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.blackjack.BlackjackForwardModel;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;

import java.util.Arrays;
import java.util.Objects;

public class Hit extends AbstractAction implements IPrintable {
    public final int playerID;
    public final boolean advanceTurnOrder;
    public final boolean hidden;

    public Hit(int playerID){
        this.playerID = playerID;
        this.advanceTurnOrder = false;
        this.hidden = false;
    }

    public Hit(int playerID, boolean advanceTurnOrder, boolean hidden){
        this.playerID = playerID;
        this.advanceTurnOrder = advanceTurnOrder;
        this.hidden = hidden;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        PartialObservableDeck<FrenchCard> playerHand = bjgs.getPlayerDecks().get(playerID);

        // Ensure that the drawDeck is correctly initialized (not null)
        if (bjgs.getDrawDeck() == null) {
            throw new IllegalStateException("Draw deck is not initialized.");
        }

        if (bjgs.getDrawDeck().getSize() == 0) {
            throw new IllegalStateException("Draw deck is empty. Cannot draw a card.");
        }
        // Draw a card from the deck
        FrenchCard drawnCard = bjgs.getDrawDeck().draw();  // Draw card from the deck

        // Check if the drawn card is null (it should never be null)
        if (drawnCard == null) {
            throw new IllegalStateException("Draw returned a null card. This should not happen.");
        }
        if (playerID != bjgs.getDealerPlayer()) {
            playerHand.add(drawnCard);
        } else {
            boolean[] visibility = new boolean[gameState.getNPlayers()];
            Arrays.fill(visibility, !hidden);  // Dealer cards are visible based on the 'hidden' flag
            playerHand.add(drawnCard, visibility);
        }

        return true;
    }


    @Override
    public AbstractAction copy() {
        return this; // immutable state
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hit)) return false;
        Hit hit = (Hit) o;
        return hidden == hit.hidden && playerID == hit.playerID && advanceTurnOrder == hit.advanceTurnOrder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, advanceTurnOrder, hidden);
    }

    @Override
    public void printToConsole(){
        System.out.println("Hit");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Hit";
    }

    @Override
    public String toString() {
        return "Hit";
    }

    public int getPlayerID() {
        return this.playerID;
    }
}
