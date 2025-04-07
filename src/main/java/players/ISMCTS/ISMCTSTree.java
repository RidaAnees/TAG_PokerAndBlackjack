package players.ISMCTS;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.poker.PokerGameState;
import games.poker.PokerForwardModel;
import java.util.List;

public class ISMCTSTree {
    public ISMCTSTreeNode root;
    private PokerForwardModel PokerForwardModel; // Add PokerForwardModel field.

    public ISMCTSTree(InformationSet initialState, PokerForwardModel PokerForwardModel, ISMCTSParams params) {
        this.root = new ISMCTSTreeNode(initialState, null, null);
        this.PokerForwardModel = PokerForwardModel; // Initialize PokerForwardModel.
    }

    public ISMCTSTreeNode select(ISMCTSParams params) {
        ISMCTSTreeNode current = root;
        while (current != null && current.state != null && current.state.getGameState() != null && !current.state.getGameState().isGameOver() && !current.children.isEmpty()) {
            current = selectChild(current, params);
        }
        return current;
    }

    private ISMCTSTreeNode selectChild(ISMCTSTreeNode node, ISMCTSParams params) {
        AbstractAction bestAction = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (AbstractAction action : node.children.keySet()) {
            ISMCTSTreeNode child = node.children.get(action);
            double value = calculateUCB(child, params);
            if (value > bestValue) {
                bestValue = value;
                bestAction = action;
            }
        }
        return node.children.get(bestAction);
    }

    private double calculateUCB(ISMCTSTreeNode node, ISMCTSParams params) {
        if (node.visitCount == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return (node.totalReward / node.visitCount) + params.K * Math.sqrt(Math.log(node.parent.visitCount) / node.visitCount);
    }

    public void expand(ISMCTSTreeNode node, List<AbstractAction> possibleActions) {
        for (AbstractAction action : possibleActions) {
            InformationSet nextState = createNextInformationSet(node.state, action, node.state.getGameState().getCurrentPlayer());
            node.children.put(action, new ISMCTSTreeNode(nextState, node, action));
        }
    }

    public double rollout(ISMCTSTreeNode node, ISMCTSParams params) { // Add ISMCTSTreeNode node parameter.
        InformationSet currentState = node.state;
        int actingPlayer = currentState.getGameState().getCurrentPlayer();
        PokerForwardModel PokerForwardModel = new PokerForwardModel(); // Create a forward model instance.

        for (int i = 0; i < params.rolloutLength && !currentState.getGameState().isGameOver(); i++) {
            List<AbstractAction> possibleActions = PokerForwardModel.computeAvailableActions(currentState.getGameState()); // Use computeAvailableActions.
            AbstractAction action = selectRolloutAction(possibleActions, params);
            currentState = createNextInformationSet(currentState, action, actingPlayer);
            actingPlayer = currentState.getGameState().getCurrentPlayer();
        }

        // Accessing AbstractGameState methods.
        AbstractGameState rolledOutGameState = currentState.getGameState();
        double gameScore = rolledOutGameState.getGameScore(node.state.getGameState().getCurrentPlayer());
        double heuristicScore = rolledOutGameState.getHeuristicScore(node.state.getGameState().getCurrentPlayer());
        List<Integer> unknownComponents = rolledOutGameState.getUnknownComponentsIds(node.state.getGameState().getCurrentPlayer());

        // Example of using AbstractGameState methods. You can adapt these for your needs.
        if (params.useHeuristicRollout) {
            return heuristicScore;
        } else {
            return gameScore;
        }
    }

    private AbstractAction selectRolloutAction(List<AbstractAction> possibleActions, ISMCTSParams params) {
        // Simple random rollout policy for now
        return possibleActions.get(new java.util.Random().nextInt(possibleActions.size()));
    }

    private InformationSet createNextInformationSet(InformationSet currentState, AbstractAction action, int actingPlayer) {
        PokerGameState pokerGameState = currentState.getGameState();
        PokerGameState nextGameState = (PokerGameState) pokerGameState.copy();
        action.execute(nextGameState);

        return new InformationSet(nextGameState, actingPlayer);
    }

    public void backpropagate(ISMCTSTreeNode node, double reward) {
        while (node != null) {
            node.visitCount++;
            node.totalReward += reward;
            node = node.parent;
        }
    }

    public AbstractAction getBestAction() {
        ISMCTSTreeNode bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (ISMCTSTreeNode child : root.children.values()) {
            double value = child.totalReward / child.visitCount;
            if (value > bestValue) {
                bestValue = value;
                bestChild = child;
            }
        }
        return bestChild != null ? bestChild.action : null;
    }
}