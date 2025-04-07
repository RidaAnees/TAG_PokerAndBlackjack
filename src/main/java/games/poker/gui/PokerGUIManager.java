package games.poker.gui;

import core.*;
import gui.AbstractGUIManager;
import games.poker.PokerForwardModel;
import games.poker.PokerGameParameters;
import games.poker.PokerGameState;
import games.poker.components.MoneyPot;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.ImageIO;
import utilities.Pair;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PokerGUIManager extends AbstractGUIManager {
    // Settings for display areas
    final static int playerAreaWidth = 300;
    final static int playerAreaHeight = 130;
    final static int pokerCardWidth = 90;
    final static int pokerCardHeight = 115;

    // Width and height of total window
    int width, height;
    // List of player hand views
    PokerPlayerView[] playerHands;
    // Draw pile view
    PokerDeckView communityPile;

    // Currently active player
    int activePlayer = -1;

    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);
    Border highlightFirst = BorderFactory.createLineBorder(new Color(246, 209, 72), 3);
    Border highlightFold = BorderFactory.createLineBorder(new Color(150, 162, 170), 3);
    Border highlightEliminated = BorderFactory.createLineBorder(new Color(236, 72, 126), 3);

    Border[] playerViewBorders;
    Border[] playerViewCompoundBordersHighlight;
    Border[] playerViewCompoundBordersFold;
    Border[] playerViewCompoundBordersFirst;
    Border[] playerViewCompoundBordersEliminated;

    JLabel potMoney;
    JLabel currentBets;

    PokerGameState pgs;
    PokerForwardModel pfm;
    CoreParameters coreParameters;

    public PokerGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);
        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        if (game != null) {
            AbstractGameState gameState = game.getGameState();
            coreParameters = game.getCoreParameters();
            if (gameState != null) {
                JTabbedPane pane = new JTabbedPane();

                JPanel main = new JPanel();
                main.setOpaque(false);
                main.setLayout(new BorderLayout());

                JPanel rules = new JPanel();

                JPanel scores = new JPanel();
                JPanel calculations = new JPanel();


                pane.add("Main", main);
                pane.add("Rules", rules);
                pane.add("Scores", scores);
                pane.add("Calculations", calculations);


                JLabel ruleText = new JLabel(getRuleText());
                rules.add(ruleText);
                rules.setBackground(new Color(43, 108, 25, 111));
                scores.setBackground(new Color(23, 108, 25, 111));
                calculations.setBackground(new Color(23, 108, 25, 111));

                potMoney = new JLabel();
                currentBets = new JLabel();


                // Initialise active player
                activePlayer = gameState.getCurrentPlayer();

                // Find required size of window
                int nPlayers = gameState.getNPlayers();
                int nHorizAreas = 1 + (nPlayers <= 3 ? 2 : nPlayers == 4 ? 3 : nPlayers <= 8 ? 4 : 5);
                double nVertAreas = 3.5;
                this.width = playerAreaWidth * nHorizAreas;
                this.height = (int) (playerAreaHeight * nVertAreas);

                pgs = (PokerGameState) gameState.copy();
                pfm = (PokerForwardModel) game.getForwardModel();
                PokerGameParameters pgp = (PokerGameParameters) gameState.getGameParameters();
                ruleText.setPreferredSize(new Dimension(width*2/3+60, height*2/3+100));

                parent.setBackground(ImageIO.GetInstance().getImage("data/FrenchCards/table-background.jpg"));

                // Create main game area that will hold all game views
                playerHands = new PokerPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                playerViewCompoundBordersHighlight = new Border[nPlayers];
                playerViewCompoundBordersFold = new Border[nPlayers];
                playerViewCompoundBordersFirst = new Border[nPlayers];
                playerViewCompoundBordersEliminated = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setOpaque(false);
                mainGameArea.setLayout(new BorderLayout());

                // Player hands go on the edges
                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
                JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
                int next = 0;
                for (int i = 0; i < nPlayers; i++) {
                    PokerPlayerView playerHand = new PokerPlayerView(pgs.getPlayerDecks().get(i), i, pgp.getDataPath());
                    playerHand.setOpaque(false);

                    // Get agent name
                    String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                    String agentName = split[split.length - 1];

                    // Create border, layouts and keep track of this view
                    TitledBorder title = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                            TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    TitledBorder titleFold = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "] - Fold",
                            TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    TitledBorder titleFirst = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "] - First",
                            TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    TitledBorder titleEliminated = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "] - Eliminated",
                            TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    playerViewBorders[i] = title;
                    playerViewCompoundBordersHighlight[i] = BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i]);
                    playerViewCompoundBordersFold[i] = BorderFactory.createCompoundBorder(highlightFold, titleFold);
                    playerViewCompoundBordersFirst[i] = BorderFactory.createCompoundBorder(highlightFirst, titleFirst);
                    playerViewCompoundBordersEliminated[i] = BorderFactory.createCompoundBorder(highlightEliminated, titleEliminated);
                    playerHand.setBorder(title);

                    sides[next].add(playerHand);
                    sides[next].setLayout(new GridBagLayout());
                    sides[next].setOpaque(false);
                    next = (next + 1) % (locations.length);
                    playerHands[i] = playerHand;
                }
                for (int i = 0; i < locations.length; i++) {
                    mainGameArea.add(sides[i], locations[i]);
                }

                // Discard and draw piles go in the center
                JPanel centerArea = new JPanel();
                centerArea.setOpaque(false);
                centerArea.setLayout(new BoxLayout(centerArea, BoxLayout.Y_AXIS));
                communityPile = new PokerDeckView(pgs.getCommunityCards(), true, pgp.getDataPath());
                communityPile.setFront(true);
                centerArea.add(communityPile);
                JPanel jp = new JPanel();
                jp.setOpaque(false);
                jp.setLayout(new GridBagLayout());
                jp.add(centerArea);
                mainGameArea.add(jp, BorderLayout.CENTER);

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Poker", gameState, width, defaultInfoPanelHeight +15);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false);

                // Add all views to frame
                main.add(mainGameArea, BorderLayout.CENTER);
                scores.add(infoPanel, BorderLayout.CENTER);
                main.add(actionPanel, BorderLayout.SOUTH);
                // TODO: calculations.add(calcsPanel, BorderLayout.CENTER);

                pane.add("Main", main);
                pane.add("Rules", rules);
                pane.add("Scores", scores);
                pane.add("Calculations", calculations);

                parent.setLayout(new BorderLayout());
                parent.add(pane, BorderLayout.CENTER);
                parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 35));
                parent.revalidate();
                parent.setVisible(true);
                parent.repaint();
            }
        }
    }

    @Override
    public int getMaxActionSpace() {
        return 15;
    }

    @Override
    protected void updateGameStateInfo(AbstractGameState gameState) {
        super.updateGameStateInfo(gameState);
        StringBuilder pots = new StringBuilder();
        for (MoneyPot c: ((PokerGameState)gameState).getMoneyPots()) {
            pots.append(c.getValue()).append(" / ");
        }
        potMoney.setText("Pot: " + pots);
        currentBets.setText("Bets: " + Arrays.toString(((PokerGameState) gameState).getPlayerBet()));
    }

    @Override
    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel gameInfo = new JPanel();
        gameInfo.setOpaque(false);
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(playerScores);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(potMoney);
        gameInfo.add(currentBets);

        gameInfo.setPreferredSize(new Dimension(width/2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        historyInfo.setPreferredSize(new Dimension(width/2 - 10, height));
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setPreferredSize(new Dimension(width/2 - 25, height));
        wrapper.add(historyContainer);
        historyInfo.setOpaque(false);
        historyContainer.setOpaque(false);
        historyContainer.getViewport().setBackground(new Color(43, 108, 25, 111));
//        historyContainer.getViewport().setOpaque(false);
        historyInfo.setEditable(false);
        return wrapper;
    }


    @Override
    protected JComponent createActionPanel(IScreenHighlight[] highlights, int width, int height, boolean boxLayout) {
        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        if (boxLayout) {
            actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        }

        actionButtons = new ActionButton[maxActionSpace];
        for (int i = 0; i < maxActionSpace; i++) {
            ActionButton ab = new ActionButton(ac, highlights);
            actionButtons[i] = ab;
            actionButtons[i].setVisible(false);
            actionPanel.add(actionButtons[i]);
        }
        for (ActionButton actionButton : actionButtons) {
            actionButton.informAllActionButtons(actionButtons);
        }

        JScrollPane pane = new JScrollPane(actionPanel);
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.setPreferredSize(new Dimension(width, height));
        if (boxLayout) {
            pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }
        return pane;
    }

    // Method to toggle card visibility
    private void toggleCardsVisibility() {
        for (PokerPlayerView playerHand : playerHands) {
            playerHand.setFront(true);  // Toggle each player's hand visibility

        }
    }


    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (pgs.getRoundCounter() != gameState.getRoundCounter()) {
                // New round
                // Paint final state of previous round, showing all hands
                for (int i = 0; i < pgs.getNPlayers(); i++) {
                    playerHands[i].setFront(true);
                    // Highlight fold and eliminated players
                    if (pgs.getPlayerResults()[i] == CoreConstants.GameResult.LOSE_GAME) {
                        playerHands[i].setBorder(playerViewCompoundBordersEliminated[i]);
                    } else if (pgs.getPlayerFold()[i]) {
                        playerHands[i].setBorder(playerViewCompoundBordersFold[i]);
                    } else {
                        playerHands[i].setBorder(playerViewBorders[i]);
                    }
                }

                Pair<Map<Integer, Integer>, Map<Integer, Set<Integer>>> translated = pfm.translatePokerHands(pgs);
                Map<Integer, Integer> ranks = translated.a;
                Map<Integer, Set<Integer>> hands = translated.b;

                int p = 0;
                String winnerString = "";
                for (MoneyPot pot: pgs.getMoneyPots()) {
                    // Calculate winners separately for each money pot
                    p++;
                    Set<Integer> winners = pfm.getWinner(pgs, pot, ranks, hands);
                    if (winners != null) {
                        winnerString += "pot" + p + " {";
                        for (int win: winners) {
                            winnerString += "Player "+ win + " won amount: " + (pot.getValue() / winners.size()) + ",";
                        }
                        winnerString += "}";
                    }
                }
                toggleCardsVisibility();
                winnerString = winnerString.replace(",}", "}");
                JOptionPane.showMessageDialog(parent, "Round over! Winners: \n" + winnerString + ". \nNext round begins!");
            }

            // Update player
            if (gameState.getCurrentPlayer() != activePlayer) {
                playerHands[activePlayer].setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }

            // Update decks and visibility
            pgs = (PokerGameState)gameState.copy();
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update(pgs);
                if (i == gameState.getCurrentPlayer() && coreParameters.alwaysDisplayCurrentPlayer
                        || humanPlayerIds.contains(i)
                        || coreParameters.alwaysDisplayFullObservable) {
                    playerHands[i].setFront(true);
                    playerHands[i].setFocusable(true);
                } else {
                    playerHands[i].setFront(false);
                }

                // Highlight active, first and fold players
                if (gameState.getPlayerResults()[i] == CoreConstants.GameResult.LOSE_GAME) {
                    playerHands[i].setBorder(playerViewCompoundBordersEliminated[i]);
                } else if (i == gameState.getFirstPlayer()) {
                    playerHands[i].setBorder(playerViewCompoundBordersFirst[i]);
                } else if (pgs.getPlayerFold()[i]) {
                    playerHands[i].setBorder(playerViewCompoundBordersFold[i]);
                } else if (i == gameState.getCurrentPlayer()) {
                    playerHands[i].setBorder(playerViewCompoundBordersHighlight[i]);
                } else {
                    playerHands[i].setBorder(playerViewBorders[i]);
                }
            }
            communityPile.updateComponent(pgs.getCommunityCards());
            communityPile.setFocusable(true);

        }
    }


    private String getRuleText() {
        String rules = "<html><center><h1>Poker</h1></center><br/><hr><br/>";
        rules += """
                <p>Objective: Win chips by having the best hand at showdown or forcing opponents to fold.<br><br>

                <strong>Blinds:</strong> Two players post forced bets—Small Blind (SB) and Big Blind (BB)—before cards are dealt.<br><br>

                <strong>Hole Cards:</strong> Each player receives two private cards.<br><br>

                <strong>Community Cards:</strong> Five cards are placed face-up on the table in three phases:<br>
                        - <strong>Flop</strong> (3 cards) <strong>Turn</strong> (1 card) <strong>River</strong> (1 card)<br><br>

                <strong>Betting Rounds:</strong><br>
                        - <strong>Pre-Flop</strong> (after hole cards are dealt)<br>
                        - <strong>Post-Flop</strong> (after 3 community cards)<br>
                        - <strong>Post-Turn</strong> (after 4th community card)<br>
                        - <strong>Post-River</strong> (after 5th community card)<br><br>

                <strong>Winning:</strong> The best 5-card combination (using any hole and community cards) wins. Players can also win by making all others fold.</p>
                
               
                <p><strong>Poker Hand Rankings (Highest to Lowest):</strong><br>
                <strong>1. Royal Flush:</strong> A, K, Q, J, 10 of the same suit (e.g., A♠ K♠ Q♠ J♠ 10♠).<br>
                <strong>2. Straight Flush:</strong> Five consecutive cards of the same suit (e.g., 9♦ 8♦ 7♦ 6♦ 5♦).<br>
                <strong>3. Four of a Kind:</strong> Four cards of the same rank (e.g., 8♣ 8♦ 8♠ 8♥ K♠).<br>
                <strong>4. Full House:</strong> Three of a kind + a pair (e.g., Q♥ Q♦ Q♠ 7♣ 7♠).<br>
                <strong>5. Flush:</strong> Five cards of the same suit, not in sequence (e.g., A♦ J♦ 9♦ 6♦ 3♦).<br>
                <strong>6. Straight:</strong> Five consecutive cards of mixed suits (e.g., 10♣ 9♦ 8♠ 7♥ 6♠).<br>
                <strong>7. Three of a Kind:</strong> Three cards of the same rank (e.g., 5♣ 5♠ 5♦ Q♥ 8♠).<br>
                <strong>8. Two Pair:</strong> Two different pairs (e.g., K♠ K♦ 4♣ 4♠ 10♦).<br>
                <strong>9. One Pair:</strong> Two cards of the same rank (e.g., J♣ J♦ 7♠ 4♣ 2♥).<br>
                <strong>10. High Card:</strong> If no one has a pair or better, the highest single card wins (e.g., A♠ 10♦ 7♣ 5♥ 3♠ → "Ace High").<br>

                <strong>Tiebreakers:</strong> If two players have the same hand type, the highest rank wins. If still tied, the kicker (highest unmatched card) is considered.<br><br>

                </p>""";
        rules += "<hr><p><b>INTERFACE: </b> Choose action at the bottom of the screen.</p>";
        rules += "</html>";
        return rules;
    }
}
