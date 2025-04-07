//package players.ISMCTS;
//
//import core.actions.AbstractAction;
//import games.poker.Action;
//import games.poker.PokerForwardModel;
//import games.poker.PokerGameState;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//public class ISMCTS {
//    private PokerGameState currentState;
//    private PokerForwardModel forwardModel;
//    private int nSimulations; // Number of simulations to run for MCTS
//    private Random rnd;
//    private ISMCTSNode root;
//
//    public ISMCTS(PokerGameState initialState, PokerForwardModel forwardModel, int nSimulations) {
//        this.currentState = initialState;
//        this.forwardModel = forwardModel;
//        this.nSimulations = nSimulations;
//        this.rnd = new Random();
//        this.root = new ISMCTSNode(initialState, initialState.getCurrentPlayer(), null);
//    }
//
//    public Action getBestAction() {
//        // Simulate several rounds of the game
//        for (int i = 0; i < nSimulations; i++) {
//            simulateRound();
//        }
//
//        // Once simulations are done, choose the best action based on the outcomes
//        return root.bestAction();
//    }
//
//    private void simulateRound() {
//        ISMCTSNode node = root;
//        PokerGameState stateCopy = (PokerGameState) currentState.copy();
//        PokerForwardModel forwardModelCopy = (PokerForwardModel) forwardModel.copy();
//
//        List<ISMCTSNode> visitedNodes = new ArrayList<>();
//        visitedNodes.add(node);
//
//        while (!node.isTerminal()) {
//            if (!node.hasChildren()) {
//                node.expand();
//                if (node.children.isEmpty()) {
//                    break;
//                }
//                node = node.children.get(rnd.nextInt(node.children.size()));
//            } else {
//                node = node.selectChild();
//            }
//            visitedNodes.add(node);
//            stateCopy = (PokerGameState) node.state.copy();
//        }
//
//        int result = node.simulateGame(stateCopy);
//
//        for (ISMCTSNode visitedNode : visitedNodes) {
//            visitedNode.backpropagate(result);
//        }
//    }
//
//    private class ISMCTSNode {
//        private PokerGameState state;
//        private int player;
//        private ISMCTSNode parent;
//        private List<ISMCTSNode> children;
//        private int visitCount;
//        private double winScore;
//        private Action action;  // Action is kept for storing the action performed at this node
//
//        public ISMCTSNode(PokerGameState state, int player, ISMCTSNode parent) {
//            this.state = state;
//            this.player = player;
//            this.parent = parent;
//            this.children = new ArrayList<>();
//            this.visitCount = 0;
//            this.winScore = 0.0;
//        }
//
//        public boolean isTerminal() {
//            return state.isRoundOver();
//        }
//
//        public ISMCTSNode selectChild() {
//            ISMCTSNode bestChild = null;
//            double bestValue = Double.NEGATIVE_INFINITY;
//
//            for (ISMCTSNode child : children) {
//                double uctValue = child.winScore / (child.visitCount + 1) +
//                        Math.sqrt(2 * Math.log(visitCount + 1) / (child.visitCount + 1));
//                if (uctValue > bestValue) {
//                    bestValue = uctValue;
//                    bestChild = child;
//                }
//            }
//            return bestChild;
//        }
//
//        public void expand() {
//            List<Action> availableActions = getAvailableActions();
//            for (Action act : availableActions) {
//                PokerGameState childState = (PokerGameState) state.copy();
//
//                forwardModel._afterAction(childState, act);
//
//                ISMCTSNode childNode = new ISMCTSNode(childState, player, this);
//                childNode.action = act;
//                children.add(childNode);
//            }
//        }
//
//        public int simulateGame(PokerGameState state) {
//            Random random = new Random();
//            int currentPlayer = player;
//            while (!state.isRoundOver()) {
//                List<Action> availableActions = getAvailableActions();
//                AbstractAction randomAction = (AbstractAction) availableActions.get(random.nextInt(availableActions.size())); // Explicit cast
//
//                forwardModel._afterAction(state, randomAction);
//
//                currentPlayer = state.getNextActingPlayer(currentPlayer, 1);
//            }
//            return (int) state.getGameScore(player);
//        }
//        public Action bestAction() {
//            ISMCTSNode bestChild = null;
//            int maxVisits = Integer.MIN_VALUE;
//
//            for (ISMCTSNode child : children) {
//                if (child.visitCount > maxVisits) {
//                    maxVisits = child.visitCount;
//                    bestChild = child;
//                }
//            }
//            return bestChild != null ? bestChild.action : null;
//        }
//
//        private List<Action> getAvailableActions() {
//            List<Action> availableActions = new ArrayList<>();
//
//            if (!state.getPlayerFold()[player]) {
//                if (!state.isBet()) {
//                    availableActions.add(new Action(Action.ActionType.CALL));
//                    availableActions.add(new Action(Action.ActionType.FOLD));
//                } else {
//                    availableActions.add(new Action(Action.ActionType.CALL));
//                    availableActions.add(new Action(Action.ActionType.RAISE, 10));
//                    availableActions.add(new Action(Action.ActionType.FOLD));
//                }
//            }
//            return availableActions;
//        }
//
//        public boolean hasChildren() {
//            return !children.isEmpty();
//        }
//
//        public void backpropagate(int result) {
//            visitCount++;
//            winScore += result;
//
//            if (parent != null) {
//                parent.backpropagate(result);
//            }
//        }
//    }
//}