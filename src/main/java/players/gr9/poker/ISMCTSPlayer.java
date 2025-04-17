package players.gr9.poker;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.poker.PokerForwardModel;
import games.poker.PokerGameState;
import players.IAnyTimePlayer;
import players.gr9.LoggerUtility;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ISMCTSPlayer extends AbstractPlayer implements IAnyTimePlayer {

    private static final Logger logger = LoggerUtility.getLogger();
    private final ISMCTSParams params;
    private final Random random;
    private PokerForwardModel forwardModel;
    private ISMCTSTreeNode root;
    private PreflopDecisionMaker preflopDecisionMaker;

    public ISMCTSPlayer(ISMCTSParams params) {
        super(params, "ISMCTSPlayer");
        this.params = params;
        this.random = new Random();
        this.forwardModel = new PokerForwardModel();
        this.preflopDecisionMaker = new PreflopDecisionMaker();
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

        if (pokerState.getGamePhase() == PokerGameState.PokerGamePhase.Preflop) {
            AbstractAction action = preflopDecisionMaker.getPreflopAction(pokerState, possibleActions);
            logger.log(Level.INFO, "Preflop decision: " + action);
            return action;
        }

        root = ISMCTSTreeNode.createRootNode(this, pokerState, random);
        root.mctsSearch(params.budget, this);
        return root.bestAction().copy();
    }

    double rollout(ISMCTSTreeNode node) {
        AbstractGameState rolloutState = node.state.copy();
        int player = rolloutState.getCurrentPlayer();

        for (int i = 0; i < params.rolloutLength && !rolloutState.isGameOver(); i++) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(rolloutState);
            if (actions.isEmpty()) break;
            AbstractAction action = rolloutPolicy(actions, rolloutState);
            forwardModel.next(rolloutState, action);
        }

        // Normalize heuristic score
        double score = rolloutState.getHeuristicScore(player);
        double retScore = Math.max(0.0, Math.min(1.0, (score + 100) / 200.0));
        logger.log(Level.WARNING, "Score by rollout policy: " + retScore);
        return retScore;
    }

    private AbstractAction rolloutPolicy(List<AbstractAction> actions, AbstractGameState state) {
        PokerGameState pokerState = (PokerGameState) state;
        int player = pokerState.getCurrentPlayer();
        List<FrenchCard> holeCards = pokerState.getPlayerCards(player);

        PokerHandBuckets buckets = new PokerHandBuckets(pokerState, holeCards, false);
        PokerHandSampler sampler = new PokerHandSampler(pokerState, pokerState.getNPlayers(), buckets, new AllProbabilities());

        int wins = 0;
        int simulations = 100;

        for (int i = 0; i < simulations; i++) {
            List<List<FrenchCard>> sampledHands = sampler.sampleOpponentCards(pokerState, player);

            List<FrenchCard> oppHand = new ArrayList<>();
            for (List<FrenchCard> hand : sampledHands) {
                oppHand.addAll(hand);
            }            int result = PokerHandEvaluator.compareHands(holeCards, oppHand, pokerState.getCommunityCards().getComponents());
            if (result >= 0) wins++; // Win or tie
        }

        double winRate = wins / (double) simulations;

        if (winRate > 0.7) {
            return actions.stream().filter(a -> a.toString().toLowerCase().contains("raise")).findFirst().orElse(actions.get(0));
        } else if (winRate > 0.5) {
            return actions.stream().filter(a -> a.toString().toLowerCase().contains("call")).findFirst().orElse(actions.get(0));
        } else {
            return actions.stream().filter(a -> a.toString().toLowerCase().contains("fold")).findFirst().orElse(actions.get(0));
        }
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

    // ------------------------
    // Private Inner Evaluator
    // ------------------------
    private static class PokerHandEvaluator {
        /**
         * Simplified hand comparison: compares highest card value.
         * Returns:
         *   > 0 if player wins
         *   = 0 if tie
         *   < 0 if opponent wins
         */
        public static int compareHands(List<FrenchCard> playerHand, List<FrenchCard> opponentHand, List<FrenchCard> communityCards) {
            int playerScore = getMaxCardValue(playerHand, communityCards);
            int opponentScore = getMaxCardValue(opponentHand, communityCards);
            return Integer.compare(playerScore, opponentScore);
        }

        private static int getMaxCardValue(List<FrenchCard> hand, List<FrenchCard> community) {
            return hand.stream()
                    .mapToInt(FrenchCard::getValue)
                    .max()
                    .orElse(0); // fallback if hand is empty
        }

    }
}
