package players.gr9.blackjack;

import players.PlayerParameters;

public class BlackjackAIParams extends PlayerParameters {
    //
    public double aggressionLevel = 0.5;  // 0.0 (passive) to 1.0 (very aggressive)
    public double riskTolerance = 0.5;    // 0.0 (risk-averse) to 1.0 (risk-taker)

    public BlackjackAIParams() {
        addTunableParameter("aggressionLevel", aggressionLevel);
        addTunableParameter("riskTolerance", riskTolerance);
    }

    @Override
    public void _reset() {
        aggressionLevel = (double) getParameterValue("aggressionLevel");
        riskTolerance = (double) getParameterValue("riskTolerance");
    }

    @Override
    protected BlackjackAIParams _copy() {
        BlackjackAIParams copy = new BlackjackAIParams();
        copy.aggressionLevel = this.aggressionLevel;
        copy.riskTolerance = this.riskTolerance;
        return copy;
    }
}
