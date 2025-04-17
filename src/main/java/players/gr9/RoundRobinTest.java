package players.gr9;

import core.AbstractPlayer;
import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.summarisers.TAGNumericStatSummary;
import games.GameType;

import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import static core.Game.recordPlayerResults;
import static core.Game.runOne;

public class RoundRobinTest {

    // Run round-robin test, generate a winMatrix and export to CSV
    public static double[][] runRoundRobinWithHeatmap(List<GameType> gamesToPlay, List<AbstractPlayer> players, Long seed,
                                                      int nRepetitions, boolean randomizeParameters,
                                                      boolean detailedStatistics, List<IGameListener> listeners, int turnPause) {
        int nPlayers = players.size();
        double[][] winMatrix = new double[nPlayers][nPlayers];

        TAGNumericStatSummary[] overall = new TAGNumericStatSummary[nPlayers];
        String[] agentNames = new String[nPlayers];

        for (int i = 0; i < nPlayers; i++) {
            String[] split = players.get(i).getClass().toString().split("\\.");
            String agentName = split[split.length - 1] + "-" + i;
            overall[i] = new TAGNumericStatSummary("Overall " + agentName);
            agentNames[i] = agentName;
        }

        for (GameType gt : gamesToPlay) {
            for (int player1 = 0; player1 < nPlayers; player1++) {
                for (int player2 = player1 + 1; player2 < nPlayers; player2++) {

                    TAGNumericStatSummary[] statSummaries = new TAGNumericStatSummary[2];
                    statSummaries[0] = new TAGNumericStatSummary("{Game: " + gt.name() + "; Player: " + agentNames[player1] + "}");
                    statSummaries[1] = new TAGNumericStatSummary("{Game: " + gt.name() + "; Player: " + agentNames[player2] + "}");

                    for (int i = 0; i < nRepetitions; i++) {
                        Long s = seed != null ? seed : System.currentTimeMillis();
                        s += (player1 + player2) * i;

                        Game game = runOne(gt, null, List.of(players.get(player1), players.get(player2)), s,
                                randomizeParameters, listeners, null, turnPause);
                        if (game != null) {
                            recordPlayerResults(statSummaries, game);
                        }
                    }

                    double player1WinRate = statSummaries[0].mean();
                    double player2WinRate = statSummaries[1].mean();
                    winMatrix[player1][player2] = player1WinRate;
                    winMatrix[player2][player1] = player2WinRate;

                    overall[player1].add(statSummaries[0]);
                    overall[player2].add(statSummaries[1]);
                }
            }
        }

        // Print matrix to console
        System.out.println("\nHeatmap of win rates:");
        for (int i = 0; i < nPlayers; i++) {
            for (int j = 0; j < nPlayers; j++) {
                System.out.printf("%6.2f ", winMatrix[i][j]);
            }
            System.out.println();
        }

        // Print overall statistics
        System.out.println("\n=====================\n");
        for (int i = 0; i < nPlayers; i++) {
            if (detailedStatistics) {
                System.out.println(overall[i]);
            } else {
                System.out.println(overall[i].name + ": " + overall[i].mean());
            }
        }

        // Export to CSV
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("heatmap.csv"))) {
            writer.write("Player,");
            for (String name : agentNames) {
                writer.write(name + ",");
            }
            writer.newLine();

            for (int i = 0; i < winMatrix.length; i++) {
                writer.write(agentNames[i] + ",");
                for (int j = 0; j < winMatrix[i].length; j++) {
                    writer.write(String.format("%.2f", winMatrix[i][j]) + ",");
                }
                writer.newLine();
            }
            System.out.println("CSV heatmap exported as 'heatmap.csv'");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return winMatrix;
    }

    public static void exportHeatmapToCSV(double[][] winMatrix, String[] agentNames) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("heatmap.csv"))) {
            writer.write("Player,");
            for (String name : agentNames) {
                writer.write(name + ",");
            }
            writer.newLine();

            for (int i = 0; i < winMatrix.length; i++) {
                writer.write(agentNames[i] + ",");
                for (int j = 0; j < winMatrix[i].length; j++) {
                    writer.write(String.format("%.2f", winMatrix[i][j]) + ",");
                }
                writer.newLine();
            }
            System.out.println("CSV heatmap exported as 'heatmap.csv'");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
