package players.gr9.poker;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.poker.PokerForwardModel;
import games.poker.PokerGameState;
import players.gr9.LoggerUtility;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ISMCTSTreeNode {

    public PokerGameState state;
    public ISMCTSTreeNode parent;
    public AbstractAction action;
    public Map<AbstractAction, ISMCTSTreeNode> children = new HashMap<>();
    public double visitCount = 0;
    public double totalReward = 0;

    private static final PokerForwardModel forwardModel = new PokerForwardModel();
    private static final Logger logger = LoggerUtility.getLogger();

    public ISMCTSTreeNode(PokerGameState state, ISMCTSTreeNode parent, AbstractAction action) {
        this.state = state;
        this.parent = parent;
        this.action = action;
    }

    public static ISMCTSTreeNode createRootNode(ISMCTSPlayer player, AbstractGameState state, Random rng) {
        PokerGameState rootState = (PokerGameState) state.copy();
        int currentPlayer = rootState.getCurrentPlayer();
        rootState.hideAllOpponentCards(currentPlayer); //from current player’s perspective they only know their own cards
        logger.warning("Hole cards: " + rootState.getPlayerCards(currentPlayer));

        PokerHandBuckets buckets = new PokerHandBuckets(rootState, rootState.getPlayerCards(currentPlayer), false);
        PokerHandSampler sampler = new PokerHandSampler(rootState, rootState.getNPlayers(), buckets, new AllProbabilities());

        PokerGameState determinized = (PokerGameState) rootState.copy();
        for (int i = 0; i < rootState.getNPlayers(); i++) {
            if (i != currentPlayer && !rootState.getPlayerFold()[i]) {
                List<List<FrenchCard>> sampledHands = sampler.sampleOpponentCards(rootState, currentPlayer);

                List<FrenchCard> sampled = new ArrayList<>();
                for (List<FrenchCard> hand : sampledHands) {
                    sampled.addAll(hand);
                }                determinized.setPlayerCards(sampled, i);
                logger.log(Level.WARNING, "Derived style for opponent {0}: {1}", new Object[]{currentPlayer, sampled});
            }
        }
        logger.log(Level.WARNING, "null root node");
        return new ISMCTSTreeNode(determinized, null, null);
    }

    public void mctsSearch(long budget, ISMCTSPlayer player) {
        long endTime = System.currentTimeMillis() + budget;

        while (System.currentTimeMillis() < endTime) {
            ISMCTSTreeNode selected = treePolicy(player);
            double reward = player.rollout(selected);
            selected.backpropagate(reward);
        }
    }

    private ISMCTSTreeNode treePolicy(ISMCTSPlayer player) {
        ISMCTSTreeNode node = this;

        while (!node.state.isGameOver()) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(node.state);
            for (AbstractAction action : actions) {
                if (!node.children.containsKey(action)) {
                    PokerGameState newState = (PokerGameState) node.state.copy();
                    forwardModel.next(newState, action);
                    ISMCTSTreeNode child = new ISMCTSTreeNode(newState, node, action);
                    node.children.put(action, child);
                    return child;
                }
            }
            node = node.selectChild();
        }

        return node;
    }

    private ISMCTSTreeNode selectChild() {
        double bestUCT = -1;
        ISMCTSTreeNode bestChild = null;

        for (Map.Entry<AbstractAction, ISMCTSTreeNode> entry : children.entrySet()) {
            ISMCTSTreeNode child = entry.getValue();

            double meanReward = child.totalReward / (child.visitCount + 1e-4);
            double exploration = Math.sqrt(Math.log(this.visitCount + 1) / (child.visitCount + 1e-4));
            double uct = meanReward + 1.41 * exploration;

            if (Double.isFinite(uct) && uct > bestUCT) {
                bestUCT = uct;
                bestChild = child;
            }
        }

        return bestChild != null ? bestChild : children.values().iterator().next();
    }

    private void backpropagate(double reward) {
        ISMCTSTreeNode node = this;
        while (node != null) {
            node.visitCount += 1;
            node.totalReward += reward;
            node = node.parent;
        }
    }

    public AbstractAction bestAction() {
        return children.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().visitCount))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
