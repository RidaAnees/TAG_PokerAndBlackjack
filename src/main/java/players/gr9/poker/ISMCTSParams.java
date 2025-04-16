package players.gr9.poker;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;

public class ISMCTSParams extends PlayerParameters {
    public double K = Math.sqrt(2);// UCB1 exploration
    public int rolloutLength = 20;
    public boolean reuseTree = false;
    public int budget = 1000;// Time or iteration budget in milliseconds
    public double epsilon = 1e-6;
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;

    public ISMCTSParams() {
        addTunableParameter("K", K);
        addTunableParameter("rolloutLength", rolloutLength);
        addTunableParameter("reuseTree", reuseTree);
        addTunableParameter("budget", budget);
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
    }

    @Override
    public void _reset() {
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        budget = (int) getParameterValue("budget");
        reuseTree = (boolean) getParameterValue("reuseTree");
    }

    @Override
    protected ISMCTSParams _copy() {
        ISMCTSParams copy = new ISMCTSParams();
        copy.K = this.K;
        copy.rolloutLength = this.rolloutLength;
        copy.reuseTree = this.reuseTree;
        copy.budget = this.budget;
        return copy;
    }

    public int getIterations() {
        return budget;
    }
}