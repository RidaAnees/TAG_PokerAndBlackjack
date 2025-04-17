package players.gr9.poker;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.poker.PokerGameState;
import players.gr9.LoggerUtility;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PreflopDecisionMaker {
    private static final Random random = new Random();
    private static final Logger logger = LoggerUtility.getLogger();

    public AbstractAction getPreflopAction(PokerGameState state, List<AbstractAction> possibleActions) {
        int player = state.getCurrentPlayer();
        FrenchCard c1 = state.getPlayerHand(player).get(0);
        FrenchCard c2 = state.getPlayerHand(player).get(1);

        String handCode = HandEncoder.encode(c1, c2);
        String position = PositionUtils.getPosition(state, player);

        PokerAction action = PokerProbabilisticLookup.getAction(handCode, position);
        logger.log(Level.FINEST, "Hand: " + ActionMatcher.match(action, possibleActions) + ", Position: " + position);
        return ActionMatcher.match(action, possibleActions);
    }
    private enum PokerAction { //for preflop logic keeping it simple
        CALL, FOLD, RAISE
    }
    private static class PokerProbabilisticLookup {
        // Separate raise maps by position
        private static final Map<String, Double> raiseMapEarly = new HashMap<>();
        private static final Map<String, Double> raiseMapMid = new HashMap<>();
        private static final Map<String, Double> raiseMapLate = new HashMap<>();

        static {
            // Sample entries, expand as needed
            raiseMapEarly.put("AA", 1.0);
            raiseMapEarly.put("KK", 1.0);
            raiseMapEarly.put("QQ", 1.0);
            raiseMapEarly.put("JJ", 1.0);
            raiseMapEarly.put("TT", 0.9);
            raiseMapEarly.put("99", 0.9);
            raiseMapEarly.put("88", 0.8);
            raiseMapEarly.put("77", 0.5);
            raiseMapEarly.put("66", 0.5);
            raiseMapEarly.put("55", 0.5);
            raiseMapEarly.put("AKs", 0.9);
            raiseMapEarly.put("AQs", 0.9);
            raiseMapEarly.put("AJs", 0.9);
            raiseMapEarly.put("ATs", 0.9);
            raiseMapEarly.put("AKo", 0.8);
            raiseMapEarly.put("AQo", 0.8);
            raiseMapEarly.put("KQs", 0.85);
            raiseMapEarly.put("KJs", 0.85);
            raiseMapEarly.put("QJs", 0.85);
            raiseMapEarly.put("JTs", 0.85);


            raiseMapMid.put("AA", 1.0);
            raiseMapMid.put("KK", 1.0);
            raiseMapMid.put("QQ", 1.0);
            raiseMapEarly.put("JJ", 1.0);
            raiseMapEarly.put("TT", 0.9);
            raiseMapEarly.put("99", 0.9);
            raiseMapEarly.put("88", 0.8);
            raiseMapEarly.put("77", 0.56);
            raiseMapEarly.put("66", 0.65);
            raiseMapEarly.put("55", 0.46);
            raiseMapMid.put("AKs", 0.9);
            raiseMapMid.put("AJs", 0.75);
            raiseMapMid.put("KQs", 0.7);
            raiseMapMid.put("KQo", 0.9);
            raiseMapMid.put("KJo", 0.9);
            raiseMapMid.put("KTo", 0.9);
            raiseMapMid.put("QJo", 0.9);
            raiseMapMid.put("QTo", 0.9);
            raiseMapMid.put("JTo", 0.9);
            raiseMapMid.put("J9o", 0.9);
            raiseMapMid.put("J9s", 0.9);
            raiseMapMid.put("J8s", 0.9);
            raiseMapMid.put("98s", 0.9);
            raiseMapMid.put("97s", 0.9);
            raiseMapMid.put("87s", 0.8);
            raiseMapMid.put("76s", 0.8);
            raiseMapMid.put("55", 0.7);
            raiseMapMid.put("44", 0.7);
            raiseMapMid.put("33", 0.7);


            raiseMapLate.put("AA", 1.0);
            raiseMapLate.put("KK", 1.0);
            raiseMapLate.put("QQ", 1.0);
            raiseMapEarly.put("JJ", 1.0);
            raiseMapEarly.put("TT", 0.9);
            raiseMapEarly.put("99", 0.9);
            raiseMapEarly.put("88", 0.8);
            raiseMapEarly.put("77", 0.5);
            raiseMapEarly.put("66", 0.5);
            raiseMapEarly.put("55", 0.5);
            raiseMapLate.put("AKs", 0.9);
            raiseMapLate.put("AJs", 0.75);
            raiseMapLate.put("AQs", 0.75);
            raiseMapLate.put("ATs", 0.95);
            raiseMapLate.put("KQs", 0.8);
            raiseMapLate.put("AJo", 0.85);
            raiseMapLate.put("A9o", 0.85);
            raiseMapLate.put("A8o", 0.85);
            raiseMapLate.put("A7o", 0.85);
            raiseMapLate.put("A6o", 0.85);
            raiseMapLate.put("A5o", 0.85);
            raiseMapLate.put("A4o", 0.85);
            raiseMapLate.put("A3o", 0.85);
            raiseMapLate.put("A2o", 0.85);
            raiseMapLate.put("A9s", 0.85);
            raiseMapLate.put("A8s", 0.95);
            raiseMapLate.put("A7s", 0.95);
            raiseMapLate.put("A6s", 0.95);
            raiseMapLate.put("A5s", 0.95);
            raiseMapLate.put("A4s", 0.95);
            raiseMapLate.put("A3s", 0.95);
            raiseMapLate.put("A2s", 0.95);
            raiseMapLate.put("K9s", 0.95);
            raiseMapLate.put("K8s", 0.95);
            raiseMapLate.put("K7s", 0.95);
            raiseMapLate.put("K6s", 0.95);
            raiseMapLate.put("Q9s", 0.95);
            raiseMapLate.put("Q8s", 0.95);
            raiseMapLate.put("Q7s", 0.95);
            raiseMapLate.put("86s", 0.95);
            raiseMapLate.put("75s", 0.95);
            raiseMapLate.put("65s", 0.95);
            raiseMapLate.put("64s", 0.95);
            raiseMapLate.put("54s", 0.95);
            raiseMapLate.put("53s", 0.95);
            raiseMapLate.put("45s", 0.95);
            raiseMapLate.put("42s", 0.95);
            raiseMapLate.put("32s", 0.95);


        }

        public static PokerAction getAction(String hand, String position) {
            Map<String, Double> raiseMap;

            switch (position) {
                case "EARLY":
                    raiseMap = raiseMapEarly;
                    break;
                case "MID":
                    raiseMap = raiseMapMid;
                    break;
                case "LATE":
                case "DEALER":
                    raiseMap = raiseMapLate;
                    break;
                default:
                    raiseMap = Collections.emptyMap();
            }

            if (raiseMap.containsKey(hand)) {
                double prob = raiseMap.get(hand);
                if (random.nextDouble() < prob) {
                    return PokerAction.RAISE;
                } else {
                    return random.nextBoolean() ? PokerAction.CALL : PokerAction.FOLD;
                }
            } else {
                return random.nextBoolean() ? PokerAction.CALL : PokerAction.FOLD;
            }
        }
    }

    // Encode hand as a string like "AKs" or "QJo"
    private static class HandEncoder {
        public static String encode(FrenchCard c1, FrenchCard c2) {
            int r1 = c1.number;
            int r2 = c2.number;
            boolean suited = c1.suite == c2.suite;

            // Swap to keep highest rank first
            if (r1 < r2) {
                FrenchCard temp = c1;
                c1 = c2;
                c2 = temp;
                r1 = c1.number;
                r2 = c2.number;
            }

            String rank1 = rankToCode(r1);
            String rank2 = rankToCode(r2);

            return rank1 + rank2 + (suited ? "s" : "o");
        }
    }
    private static String rankToCode(int card) {
        int rank = card;
        switch (rank) {
            case 1: return "A";   // Ace
            case 10:return "T";    //10 for easier; like TT instead of 1010
            case 11: return "J";  // Jack
            case 12: return "Q";  // Queen
            case 13: return "K";  // King
            case 14: return "A";  // Ace (in case it's treated as 14 in some systems)
            default: return String.valueOf(rank); // For numbers 2-10
        }
    }

    // Get simplified position (EARLY, MID, LATE)
    private static class PositionUtils {
        public static String getPosition(PokerGameState state, int player) {
            int dealer = state.getNextNonBankruptPlayer(state.getBigId(), -1);;//player to the left of big blind is dealer
            int n = state.getNPlayers();
            int rel = (player - dealer + n) % n; //calcualte relative position of a player

            //rel can take any value between 0 and n-1 so:
            if (rel == 0) return "DEALER";
            // late players
            int lateThreshold = n - 3;
            if (rel >= lateThreshold) return "LATE";
            // mid-players
            int midThresholdStart = n / 3;
            int midThresholdEnd = 2 * n / 3;
            if (rel >= midThresholdStart && rel < midThresholdEnd) return "MID";
            // EARLY players
            return "EARLY";
        }
    }

    private static class ActionMatcher {
        public static AbstractAction match(PokerAction action, List<AbstractAction> options) {
            if (options == null || options.isEmpty()) {
                throw new IllegalArgumentException("No available actions to choose from.");
            }
            for (AbstractAction a : options) {
                String name = a.toString().toLowerCase();
                if (action == PokerAction.RAISE) return a;
                if (action == PokerAction.CALL) return a;
                if (action == PokerAction.FOLD) return a;
            }
            // Fallback: randomly choose between call or fold
            List<AbstractAction> fallbackOptions = new ArrayList<>();
            for (AbstractAction a : options) {
                String name = a.toString().toLowerCase();
                if (name.contains("call") || name.contains("fold")|| name.contains("raise")) {
                    fallbackOptions.add(a);
                }
            }
            if (!fallbackOptions.isEmpty()) {
                Random random = new Random();
                return fallbackOptions.get(random.nextInt(fallbackOptions.size()));
            }
            // Final fallback
            return options.get(0);
        }
    }
}
