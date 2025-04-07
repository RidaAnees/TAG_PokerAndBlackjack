package players.ISMCTS;

import core.AbstractGameState;
import core.actions.AbstractAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import games.poker.PokerGameState;
import players.ISMCTS.InformationSet;

public class ISMCTSTreeNode {
    public InformationSet state;
    public ISMCTSTreeNode parent;
    public AbstractAction action;
    public Map<AbstractAction, ISMCTSTreeNode> children = new HashMap<>();
    public double visitCount;
    public double totalReward;

    public ISMCTSTreeNode(InformationSet state, ISMCTSTreeNode parent, AbstractAction action) {
        this.state = state;
        this.parent = parent;
        this.action = action;
        this.visitCount = 0;
        this.totalReward = 0;
    }

    public static ISMCTSTreeNode createRootNode(ISMCTSPlayer ismctsPlayer, AbstractGameState gameState, Random random) {
        InformationSet initialState = new InformationSet((PokerGameState) gameState, gameState.getCurrentPlayer());
        // Create and return the root node
        return new ISMCTSTreeNode(initialState, null, null);
    }

    // Add methods for node operations if needed (e.g., expansion, selection, etc.)
    public void addChild(AbstractAction action, ISMCTSTreeNode child) {
        children.put(action, child);
    }

    public ISMCTSTreeNode getChild(AbstractAction action) {
        return children.get(action);
    }

    public void mctsSearch(long l) {
    }

    public AbstractAction bestAction() {
        if (children.isEmpty()) {
            return null; // No children, no action to select
        }

        AbstractAction bestAction = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (Map.Entry<AbstractAction, ISMCTSTreeNode> entry : children.entrySet()) {
            ISMCTSTreeNode childNode = entry.getValue();
            double value = childNode.visitCount; // Or: childNode.totalReward / childNode.visitCount;

            if (value > bestValue) {
                bestValue = value;
                bestAction = entry.getKey();
            }
        }
        return bestAction;
    }
}