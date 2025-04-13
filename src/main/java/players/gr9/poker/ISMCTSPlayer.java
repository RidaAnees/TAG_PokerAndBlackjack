package players.gr9.poker;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.poker.PokerForwardModel;
import games.poker.PokerGameState;
import players.IAnyTimePlayer;
import players.gr9.LoggerUtility;
import games.poker.PokerHeuristic;


import java.util.*;
import java.util.logging.Logger;

public class ISMCTSPlayer extends AbstractPlayer implements IAnyTimePlayer {

    private static final Logger logger = LoggerUtility.getLogger();
    private final ISMCTSParams params;
    private final Random random;
    private final PokerForwardModel forwardModel = new PokerForwardModel();
    private ISMCTSTreeNode root;

    public ISMCTSPlayer(ISMCTSParams params) {
        super(params, "ISMCTSPlayer");
        this.params = params;
        this.random = new Random();
    }

    @Override
    public ISMCTSParams getParameters() {
        return params;
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        if (params.resetSeedEachGame) {
            random.setSeed(params.getRandomSeed());
        }
        root = null;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        PokerGameState pokerState = (PokerGameState) gameState;
        int currentPlayer = pokerState.getCurrentPlayer();

        if (pokerState.getGamePhase() == PokerGameState.PokerGamePhase.Preflop) {
            return choosePreflopAction(possibleActions);
        }

        root = ISMCTSTreeNode.createRootNode(this, pokerState, random);
        root.mctsSearch(params.budget, this);

        return root.bestAction().copy();
    }

    private AbstractAction choosePreflopAction(List<AbstractAction> actions) {
        for (AbstractAction action : actions) {
            if (action.toString().toLowerCase().contains("call")) return action;
        }
        for (AbstractAction action : actions) {
            if (action.toString().toLowerCase().contains("check")) return action;
        }
        return actions.get(random.nextInt(actions.size()));
    }

    double rollout(ISMCTSTreeNode node) {
        AbstractGameState rolloutState = node.state.copy();
        int player = node.state.getCurrentPlayer();

        for (int i = 0; i < params.rolloutLength && !rolloutState.isGameOver(); i++) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(rolloutState);
            if (actions.isEmpty()) break;
            AbstractAction action = actions.get(random.nextInt(actions.size()));
            forwardModel.next(rolloutState, action);
        }

        // Normalize reward to [0, 1]
        double score = rolloutState.getGameScore(player);
        double normScore = (score + 100) / 200.0; // assuming -100 to +100 range
        return Math.max(0.0, Math.min(1.0, normScore));
    }

    @Override
    public void finalizePlayer(AbstractGameState state) {}

    @Override
    public ISMCTSPlayer copy() {
        return new ISMCTSPlayer((ISMCTSParams) params.copy());
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
}
