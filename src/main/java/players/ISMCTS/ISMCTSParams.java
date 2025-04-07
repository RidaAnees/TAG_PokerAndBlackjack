package players.ISMCTS;

import core.interfaces.IStateHeuristic;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.util.Arrays;
import java.util.Random;

public class ISMCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public boolean useMAST = true;
    public double MASTBoltzmann = 0.1;
    public SelectionPolicy selectionPolicy = SelectionPolicy.UCB1;
    public TreePolicy treePolicy = TreePolicy.UCB;
    public OpponentTreePolicy opponentTreePolicy = OpponentTreePolicy.OneTree;
    public IStateHeuristic heuristic;
    public boolean useHeuristicRollout = false; // Add this parameter

    // New bucket system for categorizing iterations/rollouts/resources
    public Bucket[] rolloutBuckets;
    public boolean reuseTree;

    public ISMCTSParams() {
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("MASTBoltzmann", 0.1);
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 3, 10, 30, 100));
        addTunableParameter("selectionPolicy", SelectionPolicy.UCB1, Arrays.asList(SelectionPolicy.values()));
        addTunableParameter("treePolicy", TreePolicy.UCB, Arrays.asList(TreePolicy.values()));
        addTunableParameter("useHeuristicRollout", false); // Add this parameter

        // Initialize buckets
        rolloutBuckets = new Bucket[] {
                new Bucket("Quick Rollouts", 10, 100),
                new Bucket("Standard Rollouts", 100, 1000),
                new Bucket("Deep Rollouts", 1000, 10000)
        };
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        selectionPolicy = (SelectionPolicy) getParameterValue("selectionPolicy");
        treePolicy = (TreePolicy) getParameterValue("treePolicy");
        useHeuristicRollout = (boolean) getParameterValue("useHeuristicRollout");

        // Reset buckets
        for (Bucket bucket : rolloutBuckets) {
            bucket.reset();
        }
    }

    @Override
    protected ISMCTSParams _copy() {
        ISMCTSParams copy = new ISMCTSParams();
        copy.rolloutBuckets = new Bucket[rolloutBuckets.length];
        for (int i = 0; i < rolloutBuckets.length; i++) {
            copy.rolloutBuckets[i] = new Bucket(rolloutBuckets[i].name, rolloutBuckets[i].minSize, rolloutBuckets[i].maxSize);
        }
        copy.useHeuristicRollout = this.useHeuristicRollout; // Copy the new parameter
        return copy;
    }

    // Use bucket strategy for rollouts
    public RandomPlayer getRolloutStrategy() {
        // Select bucket based on current rollout length or other conditions
        Bucket selectedBucket = selectBucketForRollout();
        return new RandomPlayer(new Random(getRandomSeed()));  // Example using RandomPlayer, modify as needed
    }

    // Bucket selection logic based on rollout length
    private Bucket selectBucketForRollout() {
        for (Bucket bucket : rolloutBuckets) {
            if (rolloutLength >= bucket.minSize && rolloutLength <= bucket.maxSize) {
                return bucket;
            }
        }
        return rolloutBuckets[0];  // Default to the first bucket if no match
    }

    public int getIterations() {
        return rolloutLength;
    }

    // Nested Bucket class for categorizing rollouts
    public static class Bucket {
        String name;
        int minSize;
        int maxSize;
        int currentSize;

        public Bucket(String name, int minSize, int maxSize) {
            this.name = name;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.currentSize = 0;
        }

        // Reset bucket's state
        public void reset() {
            currentSize = 0;
        }

        public void addItem() {
            if (currentSize < maxSize) {
                currentSize++;
            }
        }

        @Override
        public String toString() {
            return name + ": " + currentSize + "/" + maxSize;
        }
    }

    public enum SelectionPolicy {
        UCB1,
        ALPHA,
        /*etc*/
    }

    public enum TreePolicy {
        UCB,
        /*etc*/
    }

    public enum OpponentTreePolicy {
        OneTree,
        /*etc*/
    }
}