//package games.poker;
//
//import games.poker.PokerGameState;
//
//public class Action {
//
//    public enum ActionType {
//        CALL,
//        RAISE,
//        FOLD,
//        CHECK,
//        BET,
//        ALLIN
//    }
//
//    private ActionType actionType;
//    private int amount;  // For actions like RAISE
//
//    public Action(ActionType actionType) {
//        this.actionType = actionType;
//        this.amount = 0;
//    }
//
//    public Action(ActionType actionType, int amount) {
//        this.actionType = actionType;
//        this.amount = amount;
//    }
//
//    public int getAmount() {
//        return amount;
//    }
//
//
//
//    @Override
//    public String toString() {
//        if (actionType == ActionType.RAISE) {
//            return "RAISE " + amount ;
//        }
//        return actionType.name();
//    }
//}
//
//
