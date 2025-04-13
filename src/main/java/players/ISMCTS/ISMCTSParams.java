package players.ISMCTS;

import core.interfaces.IStateHeuristic;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.util.Arrays;
import java.util.Random;
public class ISMCTSParams extends PlayerParameters {
    public double K = 1.33;// UCB1 exploration
    public int rolloutLength = 10;
    public boolean reuseTree = false;
    public int budget = 3000;// Time or iteration budget in milliseconds

    public ISMCTSParams() {
        addTunableParameter("K", K);
        addTunableParameter("rolloutLength", rolloutLength);
        addTunableParameter("budget", budget);
        addTunableParameter("reuseTree", reuseTree);
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
