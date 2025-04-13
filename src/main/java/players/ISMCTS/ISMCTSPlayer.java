package players.ISMCTS;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.poker.PokerGameState;
import players.IAnyTimePlayer;
import players.ISMCTS.poker.AllProbabilities;
import players.ISMCTS.poker.PokerHandBuckets;
import players.ISMCTS.poker.PokerHandSampler;
import utilities.Pair;
import games.poker.PokerForwardModel;

import java.util.logging.Logger;
import java.util.*;

import static games.poker.PokerGameState.PokerGamePhase.*;

public class ISMCTSPlayer extends AbstractPlayer implements IAnyTimePlayer {

    private int actionCallCount = 0;

    private static final Logger logger = LoggerUtility.getLogger();

    private final ISMCTSParams params;
    final Random random;
    protected PokerForwardModel forwardModel;
    private Pair<Integer, AbstractAction> lastAction;
    private ISMCTSTreeNode root;

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
    }

    public List<AbstractAction> computeAvailableActions(AbstractGameState gameState) {
        return forwardModel._computeAvailableActions(gameState);
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        logger.info("Action call count: " + actionCallCount);  // Check how many times _getAction is called
        PokerGameState pokerGameState = (PokerGameState) gameState;
        int currentPlayer = pokerGameState.getCurrentPlayer();
        List<FrenchCard> holeCards = pokerGameState.getPlayerCards(currentPlayer);
        logger.warning("ISMCTSplayer hole cards: " + holeCards);

        PokerGameState.PokerGamePhase phase = (PokerGameState.PokerGamePhase) pokerGameState.getGamePhase();
        if (phase == Preflop) {
            logger.warning("handleGamePhase: Preflop");
            return choosePreflopAction(possibleActions);
        }else {
            logger.warning("handleGamePhase: " + phase);
            // Postflop: Flop, Turn, or River
            PokerHandBuckets handBuckets = new PokerHandBuckets(pokerGameState, holeCards, false);
            AllProbabilities allProbabilities = new AllProbabilities();
            int numPlayers = pokerGameState.getNPlayers();
            PokerHandSampler handSampler = new PokerHandSampler((PokerGameState) gameState, numPlayers, handBuckets, allProbabilities);
            List<String> playingStyles = new ArrayList<>();

            for (int i = 0; i < numPlayers; i++) {
                if (i == currentPlayer) {
                    continue; // Skip the current player as they are not being analyzed
                }
                String style = handSampler.deriveStyle((PokerGameState) gameState, currentPlayer);
                playingStyles.add(style); // Store the style for the player
                logger.fine("playing style of player " + i+1 + " is " + style);

            }
            List<FrenchCard> sampled = handSampler.sampleOpponentCards(pokerGameState, currentPlayer);
            logger.finest("Sampled hands for current phase: " + sampled);
            root = ISMCTSTreeNode.createRootNode(this, pokerGameState, random);//root creation logic in ISMCTSTreeNode class
            long endTime = params.budget ;
            root.mctsSearch(endTime, this);
            AbstractAction bestAction = root.bestAction();
            lastAction = new Pair<>(currentPlayer, bestAction);
            logger.fine("Best action selected :"+ bestAction);
            return bestAction.copy();
        }
    }

    private AbstractAction choosePreflopAction(List<AbstractAction> actions) {
        // Preflop logic to choose an action
        for (AbstractAction action : actions) {
            if (action.toString().toLowerCase().contains("call")) return action;
            logger.fine("pre flop action: call");
        }
        for (AbstractAction action : actions) {
            if (action.toString().toLowerCase().contains("fold")) return action;
            logger.fine("pre flop action: fold");
        }
        return actions.get(random.nextInt(actions.size()));
    }

    protected ISMCTSTreeNode backtrack(ISMCTSTreeNode startingRoot, AbstractGameState gameState) {
        List<Pair<Integer, AbstractAction>> history = gameState.getHistory();
        ISMCTSTreeNode newRoot = startingRoot;
        int rootPlayer = startingRoot.state.getCurrentPlayer();

        for (int i = history.size() - 1; i >= 0; i--) {
            Pair<Integer, AbstractAction> playedAction = history.get(i);
            if (playedAction.a == rootPlayer && newRoot.children.containsKey(playedAction.b)) {
                newRoot = newRoot.children.get(playedAction.b);
            } else {
                return ISMCTSTreeNode.createRootNode(this, gameState, random);
            }
        }
        return newRoot;
    }

    double rollout(ISMCTSTreeNode node) {
        AbstractGameState rolloutState = node.state.copy();
        int rolloutLength = Math.max(params.rolloutLength, 50);

        for (int i = 0; i < rolloutLength && !rolloutState.isGameOver(); i++) {
            List<AbstractAction> possibleActions = forwardModel.computeAvailableActions(rolloutState);
            if (possibleActions.isEmpty()) break;
            AbstractAction action = possibleActions.get(random.nextInt(possibleActions.size()));
            forwardModel.next(rolloutState, action);
        }

        return rolloutState.getGameScore(node.state.getCurrentPlayer());
    }

    @Override
    public void finalizePlayer(AbstractGameState state) {}

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
