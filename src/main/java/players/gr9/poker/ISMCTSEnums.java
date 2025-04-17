package players.gr9.poker;

public class ISMCTSEnums {

    public enum SelectionStrategies {
        RANDOM, //purely random selection
        BUCKET_BASED, //based on hand strength buckets //TODO
        PARAMS, //Parametrized strategies
        DEFAULT_SELECTION //default fallback
    }

    public enum ExplorationStrategies{
        MAST,
        HEURISTIC,
        HYBRID, //TODO
        DEFAULT_EXPLORATION
    }

    public enum Information {
        Information_Set
    }

    public enum MASTType {
        None,
        Rollout,
        Tree,
        Both,
        Global
    }

    public enum SelectionPolicy {
        ROBUST,
        SIMPLE,
        UCB1
    }

    // Hand Bucket Strategies (Poker Specific)
    public enum BucketActionSelection {
        RANDOM, // Random action within bucket
        AGGRESSIVE, // raise more often
        PASSIVE, // /call more often
        BALANCED // Mixed strategy
    }

    public enum TreePolicy {
        UCB,
        UCB_Tuned,
        AlphaGo,
        EXP3,
        RegretMatching,
        Uniform,
        Greedy
    }

    public enum BackupPolicy {
        MonteCarlo,
        Lambda,
        MaxLambda,
        MaxMC
    }

    public enum RolloutIncrement {
        TICK,
        TURN,
        ROUND
    }

    public enum RolloutTermination {
        DEFAULT,
        END_ACTION,
        END_TURN,
        START_ACTION,
        END_ROUND
    }

    public enum OpponentTreePolicy {
        SelfOnly(true),
        OneTree(false),
        MultiTree(true),
        OMA(false),
        OMA_All(false),
        MCGS(false),
        MCGSSelfOnly(true);

        public final boolean selfOnlyTree;

        OpponentTreePolicy(boolean selfOnlyTree) {
            this.selfOnlyTree = selfOnlyTree;
        }
    }
}