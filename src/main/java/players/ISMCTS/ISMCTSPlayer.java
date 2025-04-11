package players.ISMCTS;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.poker.PokerForwardModel;
import games.poker.PokerGameState;
import players.IAnyTimePlayer;
import utilities.Pair;

import java.util.*;

public class ISMCTSPlayer extends AbstractPlayer implements IAnyTimePlayer {
    private final ISMCTSParams params;
    private final Random random;
    private PokerForwardModel forwardModel;
    private Pair<Integer, AbstractAction> lastAction;
    private ISMCTSTreeNode root;

    public static class ISMCTSTreeNode {
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
            InformationSet initialState = new InformationSet((PokerGameState) gameState.copy(), gameState.getCurrentPlayer());
            return new ISMCTSTreeNode(initialState, null, null);
        }

        public void addChild(AbstractAction action, ISMCTSTreeNode child) {
            children.put(action, child);
        }

        public ISMCTSTreeNode getChild(AbstractAction action) {
            return children.get(action);
        }

        public void mctsSearch(long timeLimitMillis, ISMCTSPlayer ismctsPlayer) {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeLimitMillis) {
                ISMCTSTreeNode selectedNode = this.selectNode(ismctsPlayer);
                if (selectedNode.state != null && !selectedNode.state.getGameState().isGameOver()) {
                    List<AbstractAction> possibleActions = ismctsPlayer.forwardModel.computeAvailableActions(selectedNode.state.getGameState());
                    if (!possibleActions.isEmpty()) {
                        ISMCTSTreeNode expandedNode = selectedNode.expand(possibleActions, ismctsPlayer.forwardModel, ismctsPlayer.random);
                        double reward = ismctsPlayer.rollout(expandedNode);
                        expandedNode.backpropagate(reward);
                    } else {
                        selectedNode.backpropagate(selectedNode.state.getGameState().getGameScore(selectedNode.state.getPlayerId()));
                    }
                } else if (selectedNode.state != null) {
                    selectedNode.backpropagate(selectedNode.state.getGameState().getGameScore(selectedNode.state.getPlayerId()));
                }
            }
        }

        private ISMCTSTreeNode selectNode(ISMCTSPlayer ismctsPlayer) {
            ISMCTSTreeNode current = this;
            while (!current.children.isEmpty()) {
                current = current.selectUCTChild(
                        current.parent != null ? current.parent.visitCount : 1,
                        current.state.getPlayerId(),
                        current.state.getGameState().getNPlayers(),
                        ismctsPlayer.getParameters()
                );
            }
            return current;
        }

        private ISMCTSTreeNode selectUCTChild(double parentVisits, int playerId, int nPlayers, ISMCTSParams params) {
            ISMCTSTreeNode bestChild = null;
            double bestValue = -Double.MAX_VALUE;

            for (ISMCTSTreeNode child : children.values()) {
                double uctValue = child.totalReward / (child.visitCount + 1e-6) +
                        params.K * Math.sqrt(Math.log(parentVisits + 1) / (child.visitCount + 1e-6));
                if (uctValue > bestValue) {
                    bestValue = uctValue;
                    bestChild = child;
                }
            }
            return bestChild;
        }

        private ISMCTSTreeNode expand(List<AbstractAction> possibleActions, PokerForwardModel forwardModel, Random random) {
            int actingPlayer = state.getGameState().getCurrentPlayer();
            for (AbstractAction action : possibleActions) {
                AbstractGameState nextGameState = state.getGameState().copy();
                forwardModel.next(nextGameState, action);
                InformationSet nextState = new InformationSet((PokerGameState) nextGameState, actingPlayer);
                ISMCTSTreeNode newNode = new ISMCTSTreeNode(nextState, this, action);
                children.put(action, newNode);
            }

            if (children.isEmpty()) return null;

            List<ISMCTSTreeNode> childList = new ArrayList<>(children.values());
            return childList.get(random.nextInt(childList.size()));
        }

        private void backpropagate(double reward) {
            visitCount++;
            totalReward += reward;
            if (parent != null) {
                parent.backpropagate(reward);
            }
        }

        public AbstractAction bestAction() {
            if (children.isEmpty()) return null;

            AbstractAction bestAction = null;
            double bestValue = -Double.MAX_VALUE;

            for (Map.Entry<AbstractAction, ISMCTSTreeNode> entry : children.entrySet()) {
                ISMCTSTreeNode childNode = entry.getValue();
                double value = childNode.totalReward / (childNode.visitCount + 1e-6);

                if (value > bestValue) {
                    bestValue = value;
                    bestAction = entry.getKey();
                }
            }
            return bestAction;
        }
    }

    public ISMCTSPlayer(ISMCTSParams params) {
        super(params, "ISMCTSPlayer");
        this.params = params;
        this.random = new Random();
        this.forwardModel = new PokerForwardModel();
    }

    @Override
    public ISMCTSParams getParameters() {
        return (ISMCTSParams) parameters;
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        if (getParameters().resetSeedEachGame) {
            random.setSeed(parameters.getRandomSeed());
        }
        root = null;
    }

    @Override
    public void registerUpdatedObservation(AbstractGameState gameState) {
        super.registerUpdatedObservation(gameState);
        if (!getParameters().reuseTree) {
            root = null;
        }
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        long currentTimeNano = System.nanoTime();
        createRootNode(gameState);
        long timeTaken = System.nanoTime() - currentTimeNano;

        if (root != null) {
            root.mctsSearch((timeTaken / 1_000_000) + params.budget, this);
            lastAction = new Pair<>(gameState.getCurrentPlayer(), root.bestAction());
            return lastAction.b.copy();
        } else {
            return possibleActions.get(random.nextInt(possibleActions.size()));
        }
    }

    protected void createRootNode(AbstractGameState gameState) {
        if (root == null) {
            root = ISMCTSTreeNode.createRootNode(this, gameState, random);
        } else if (params.reuseTree) {
            ISMCTSTreeNode newRoot = backtrack(root, gameState);
            root = (newRoot != null) ? newRoot : ISMCTSTreeNode.createRootNode(this, gameState, random);
        } else {
            root = ISMCTSTreeNode.createRootNode(this, gameState, random);
        }
    }

    protected ISMCTSTreeNode backtrack(ISMCTSTreeNode startingRoot, AbstractGameState gameState) {
        List<Pair<Integer, AbstractAction>> history = gameState.getHistory();
        ISMCTSTreeNode newRoot = startingRoot;
        int rootPlayer = startingRoot.state.getPlayerId();

        for (int i = history.size() - 1; i >= 0; i--) {
            Pair<Integer, AbstractAction> playedAction = history.get(i);
            if (playedAction.a == rootPlayer && newRoot.children.containsKey(playedAction.b)) {
                newRoot = newRoot.children.get(playedAction.b);
            } else {
                return null;
            }
        }
        return newRoot;
    }

    private double rollout(ISMCTSTreeNode node) {
        AbstractGameState rolloutState = node.state.getGameState().copy();
        int rolloutLength = params.rolloutLength;

        for (int i = 0; i < rolloutLength && !rolloutState.isGameOver(); i++) {
            List<AbstractAction> possibleActions = forwardModel.computeAvailableActions(rolloutState);
            if (possibleActions.isEmpty()) break;
            AbstractAction action = possibleActions.get(random.nextInt(possibleActions.size()));
            forwardModel.next(rolloutState, action);
        }

        return rolloutState.getGameScore(node.state.getPlayerId());
    }

    @Override
    public void finalizePlayer(AbstractGameState state) {
        // Optional clean-up
    }

    @Override
    public ISMCTSPlayer copy() {
        return new ISMCTSPlayer((ISMCTSParams) getParameters().copy());
    }

    @Override
    public void setBudget(int budget) {
        params.budget = budget;
        params.setParameterValue("budget", budget);
    }

    @Override
    public int getBudget() {
        return params.budget;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
