package players.ISMCTS;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.poker.PokerForwardModel;
import players.IAnyTimePlayer;
import utilities.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ISMCTSPlayer extends AbstractPlayer implements IAnyTimePlayer {
    private final ISMCTSParams params;
    private final Random random;
    private Map<Integer, ISMCTSTree> playerTrees;
    private PokerForwardModel forwardModel;
    private Pair<Integer, AbstractAction> lastAction;
    private ISMCTSTreeNode root;

    public ISMCTSPlayer(ISMCTSParams params) {
        super(params, "ISMCTSPlayer");
        this.params = params;
        this.random = new Random();
        this.playerTrees = new HashMap<>();
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

        root.mctsSearch(timeTaken / 1000000);

        lastAction = new Pair<>(gameState.getCurrentPlayer(), root.bestAction());
        return lastAction.b.copy();
    }

    protected void createRootNode(AbstractGameState gameState) {
        ISMCTSTreeNode newRoot = newRootNode(gameState);
        if (newRoot == null) {
            root = ISMCTSTreeNode.createRootNode(this, gameState, random);
        } else {
            root = newRoot;
        }
    }

    protected ISMCTSTreeNode newRootNode(AbstractGameState gameState) {
        if (params.reuseTree && root != null) {
            return backtrack(root, gameState);
        }
        return null;
    }

    protected ISMCTSTreeNode backtrack(ISMCTSTreeNode startingRoot, AbstractGameState gameState) {
        List<Pair<Integer, AbstractAction>> history = gameState.getHistory();
        Pair<Integer, AbstractAction> lastExpected = lastAction;
        ISMCTSTreeNode newRoot = startingRoot;
        int rootPlayer = startingRoot.state.getPlayerId();
        for (int backwardLoop = history.size() - 1; backwardLoop >= 0; backwardLoop--) {
            if (history.get(backwardLoop).equals(lastExpected)) {
                for (int forwardLoop = backwardLoop; forwardLoop < history.size(); forwardLoop++) {
                    if (history.get(forwardLoop).a != rootPlayer) continue;
                    AbstractAction action = history.get(forwardLoop).b;
                    int nextActionPlayer = forwardLoop < history.size() - 1 ? history.get(forwardLoop + 1).a : gameState.getCurrentPlayer();
                    if (newRoot.children != null && newRoot.children.get(action) != null) {
                        newRoot = newRoot.children.get(action);
                    } else {
                        newRoot = null;
                    }
                    if (newRoot == null) break;
                }
                break;
            }
        }
        return newRoot;
    }

    @Override
    public void finalizePlayer(AbstractGameState state) {
        // Implement game over event handling here
    }

    @Override
    public ISMCTSPlayer copy() {
        ISMCTSPlayer retValue = new ISMCTSPlayer((ISMCTSParams) getParameters().copy());
        return retValue;
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

