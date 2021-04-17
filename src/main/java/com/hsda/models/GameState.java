package com.hsda.models;

import com.hsda.analyzer.GameStateAnalyzer;
import com.hsda.service.CardFetcherService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameState {
    Pattern cardIdRegex = Pattern.compile("(?<=cardId=).*?(?=player=)");
    Pattern cardNameRegex = Pattern.compile("(?<=entityName=).*?(?=id=)");

    CardFetcherService service;

    private Map<String, Card> cardIdMap;

    private List<Card> friendlyHand;
    private List<Card> friendlyBoard;
    private List<Card> friendlySecrets;
    private List<Card> opponentBoard;

    private int mana;
    private int savedCurrentMana;

    private int turnCount;

    private int opponentHandSize;

    private int opponentLifeTotal;

    private boolean playersTurn;
    private boolean inMulligan;
    private boolean mulliganWaiting;
    private int mulliganMarkerCount;

    private int waitingForDraws;

    private boolean bowEquipped;

    private GameStateAnalyzer analyzer;

    private boolean wolpertingerBattlecry;

    public GameState() throws IOException, InterruptedException {
        cardIdMap = new HashMap<>();
        service = new CardFetcherService();

        friendlyHand = new ArrayList<>();
        friendlyBoard = new ArrayList<>();
        friendlySecrets = new ArrayList<>();

        opponentBoard = new ArrayList<>();
        opponentHandSize = 0;

        opponentLifeTotal = 30;

        playersTurn = true;

        mulliganMarkerCount = 0;
        mulliganWaiting = false;

        mana = 0;
        savedCurrentMana = 0;

        turnCount = 0;

        waitingForDraws = 0;

        bowEquipped = false;

        wolpertingerBattlecry = false;

        for (Card c : service.getDeckCards()) {
            cardIdMap.put(c.getCardId(), c);
        }
    }

    // GETTER METHODS USED BY THE ANALYZER

    public List<Card> getFriendlyHand() {
        return friendlyHand;
    }

    public List<Card> getFriendlyBoard() {
        return friendlyBoard;
    }

    public int getEnemyHandSize() {
        return opponentHandSize;
    }

    public List<Card> getEnemyBoard() {
        return opponentBoard;
    }

    public boolean isBowEquipped() {
        return bowEquipped;
    }

    // METHODS FOR SETUP AND COMMUNICATION WITH THE ANALYZER

    public void setAnalyzer(GameStateAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public void beginMulligan() {
        mulliganMarkerCount++;
        if (mulliganMarkerCount == 2) {
            inMulligan = true;
        } else if (mulliganMarkerCount == 3) {
            analyzer.notifyMulliganBegins();
            mulliganMarkerCount = 0;
        }
    }

    public boolean isInMulligan() {
        return inMulligan;
    }

    public void beginGame() {
        inMulligan = false;
        mulliganWaiting = false;
        if (opponentHandSize == 3) {
            playersTurn = false;
        } else {
            playersTurn = true;
            mana = 1;
        }
    }

    public void gameEnded() {
        friendlyHand = new ArrayList<>();
        friendlyBoard = new ArrayList<>();
        friendlySecrets = new ArrayList<>();

        opponentBoard = new ArrayList<>();
        opponentHandSize = 0;

        opponentLifeTotal = 30;

        playersTurn = true;

        mulliganMarkerCount = 0;
        mulliganWaiting = false;

        mana = 0;

        savedCurrentMana = 0;
        waitingForDraws = 0;
        bowEquipped = false;
        wolpertingerBattlecry = false;
    }

    public void notifyAfterNDraws(int drawCount, int currentMana) {
        savedCurrentMana = currentMana;
        waitingForDraws = drawCount;
    }

    // METHODS FOR MANAGING USER'S GAMESTATE

    public void addCardToHand(String logLine) throws IOException, InterruptedException {
        Matcher matcher = cardIdRegex.matcher(logLine);
        String cardId;
        if (matcher.find()) {
            cardId = matcher.group(0).substring(0,matcher.group(0).length() - 1);
        } else {
            System.out.println("Error while adding card to player hand.");
            return;
        }

        if (cardIdMap.containsKey(cardId)) {
            friendlyHand.add(new Card(cardIdMap.get(cardId)));
            if (cardIdMap.get(cardId).getName().equalsIgnoreCase("the coin")) {
                //This means we're going second, so set this variable accordingly.
                playersTurn = false;
            }
        } else {
            Card fetchedCard = service.getCardInfo(cardId);
            cardIdMap.put(cardId, fetchedCard);
            friendlyHand.add(new Card(fetchedCard));
            if (fetchedCard.getName().equalsIgnoreCase("the coin")) {
                //This means we're going second, so set this variable accordingly.
                playersTurn = false;
            }
        }

        if (inMulligan) {
            //Do nothing
        } else if (waitingForDraws > 0) {
            waitingForDraws--;
            if (waitingForDraws == 0) {
                analyzer.notifyStart(savedCurrentMana, true);
            }
        } else {
            if (!playersTurn) {
                playersTurn = true;
                if (mana < 10) {
                    mana++;
                }
            }
            turnCount++;
            System.out.println("\n--- BEGIN TURN " + turnCount + " ---");
            analyzer.notifyStart(mana, false);
        }
    }

    public void addCardToFriendlyBoard(String logLine) throws IOException, InterruptedException {
        Matcher matcher = cardIdRegex.matcher(logLine);
        String cardId;
        if (matcher.find()) {
            cardId = matcher.group(0).substring(0,matcher.group(0).length() - 1);
        } else {
            System.out.println("Error while adding card to player board.");
            return;
        }

        if (cardIdMap.containsKey(cardId)) {
            Card playedCard = cardIdMap.get(cardId);
            if (friendlyHand.contains(playedCard)) {
                if (playedCard.getName().equalsIgnoreCase("wolpertinger")
                    && !wolpertingerBattlecry) {
                    wolpertingerBattlecry = true;
                    friendlyHand.remove(playedCard);
                } else if (playedCard.getName().equalsIgnoreCase("wolpertinger")
                        && wolpertingerBattlecry) {
                    wolpertingerBattlecry = false;
                } else {
                    friendlyHand.remove(playedCard);
                }
            }

            if (playedCard.getName().equalsIgnoreCase("Eaglehorn Bow")) {
                bowEquipped = true;
            } else {
                friendlyBoard.add(playedCard);
            }
        } else {
            Card fetchedCard = service.getCardInfo(cardId);
            cardIdMap.put(cardId, fetchedCard);
            if (friendlyHand.contains(fetchedCard)) {
                friendlyHand.remove(fetchedCard);
            }

            if (fetchedCard.getName().equalsIgnoreCase("Eaglehorn Bow")) {
                bowEquipped = true;
            } else {
                friendlyBoard.add(fetchedCard);
            }
        }
    }

    public void addFriendlySecret(String logLine) throws IOException, InterruptedException {
        Matcher matcher = cardIdRegex.matcher(logLine);
        String cardId;
        if (matcher.find()) {
            cardId = matcher.group(0).substring(0,matcher.group(0).length() - 1);
        } else {
            System.out.println("Error while adding secret to player board.");
            return;
        }

        if (cardIdMap.containsKey(cardId)) {
            Card playedCard = cardIdMap.get(cardId);
            if (friendlyHand.contains(playedCard)) {
                friendlyHand.remove(playedCard);
            }
            friendlySecrets.add(cardIdMap.get(cardId));
        } else {
            Card fetchedCard = service.getCardInfo(cardId);
            cardIdMap.put(cardId, fetchedCard);
            if (friendlyHand.contains(fetchedCard)) {
                friendlyHand.remove(fetchedCard);
            }
            friendlySecrets.add(fetchedCard);
        }
    }

    public void cardToFriendlyGraveyard(String logLine) throws IOException, InterruptedException {
        Matcher matcher = cardIdRegex.matcher(logLine);
        String cardId;
        if (matcher.find()) {
            cardId = matcher.group(0).substring(0,matcher.group(0).length() - 1);
        } else {
            System.out.println("Error while adding card to player graveyard.");
            return;
        }

        if (cardIdMap.containsKey(cardId)) {
            Card playedCard = cardIdMap.get(cardId);
            if (playedCard.getType().equalsIgnoreCase("minion")) {
                friendlyBoard.remove(playedCard);
            } else if (playedCard.getName().equalsIgnoreCase("Freezing Trap")) {
                friendlySecrets.remove(playedCard);
            } else if (playedCard.getName().equalsIgnoreCase("Eaglehorn Bow")) {
                bowEquipped = false;
            } else if (!playedCard.getName().equalsIgnoreCase("Master's Call")) {
                friendlyHand.remove(playedCard);
            }
        } else {
            Card fetchedCard = service.getCardInfo(cardId);
            cardIdMap.put(cardId, fetchedCard);
            if (fetchedCard.getType().equalsIgnoreCase("minion")) {
                friendlyBoard.remove(fetchedCard);
            } else if (fetchedCard.getName().equalsIgnoreCase("Freezing Trap")) {
                friendlySecrets.remove(fetchedCard);
            } else if (fetchedCard.getName().equalsIgnoreCase("Eaglehorn Bow")) {
                bowEquipped = false;
            } else if (!fetchedCard.getName().equalsIgnoreCase("Master's Call")) {
                friendlyHand.remove(fetchedCard);
            }
        }
    }

    public void mulliganCard(String logLine) throws IOException, InterruptedException {
        Matcher matcher = cardNameRegex.matcher(logLine);
        String cardName;
        if (matcher.find()) {
            cardName = matcher.group(0).substring(0,matcher.group(0).length() - 1);
        } else {
            System.out.println("Error occurred during mulligan.");
            return;
        }
        Card toRemove = null;
        for (Card c : friendlyHand) {
            if (c.getName().equals(cardName)) {
                toRemove = c;
            }
        }
        friendlyHand.remove(toRemove);
    }

    public void burnedCard() {
        if (inMulligan) {
            //Do nothing
        } else if (waitingForDraws > 0) {
            waitingForDraws--;
            if (waitingForDraws == 0) {
                analyzer.notifyStart(savedCurrentMana, true);
            }
        } else {
            if (!playersTurn) {
                playersTurn = true;
                if (mana < 10) {
                    mana++;
                    System.out.println("Mana increased to " + mana);
                }
            }
            analyzer.notifyStart(mana, false);
        }
    }

    // METHODS FOR MANAGING OPPONENT'S GAMESTATE

    public void addCardToOpposingHand() {
        opponentHandSize++;

        if (playersTurn) {
            playersTurn = false;
        }
    }

    public void addCardToOpposingBoard(String logLine) throws IOException, InterruptedException {
        Matcher matcher = cardIdRegex.matcher(logLine);
        String cardId;
        if (matcher.find()) {
            cardId = matcher.group(0).substring(0,matcher.group(0).length() - 1);
        } else {
            System.out.println("Error while adding card to player graveyard.");
            return;
        }

        if (cardIdMap.containsKey(cardId)) {
            opponentHandSize--;
            if (cardIdMap.get(cardId).getType().equalsIgnoreCase("minion")) {
                opponentBoard.add(cardIdMap.get(cardId));
            }
        } else {
            Card fetchedCard = service.getCardInfo(cardId);
            cardIdMap.put(cardId, fetchedCard);
            opponentHandSize--;
            if (fetchedCard.getType().equalsIgnoreCase("minion")) {
                opponentBoard.add(fetchedCard);
            }
        }
    }

    public void cardToOpposingGraveyard(String logLine) throws IOException, InterruptedException {
        Matcher matcher = cardIdRegex.matcher(logLine);
        String cardId;
        if (matcher.find()) {
            cardId = matcher.group(0).substring(0,matcher.group(0).length() - 1);
        } else {
            System.out.println("Error while adding card to player graveyard.");
            return;
        }

        if (cardIdMap.containsKey(cardId)) {
            Card playedCard = cardIdMap.get(cardId);
            if (playedCard.getType().equalsIgnoreCase("minion")) {
                opponentBoard.remove(playedCard);
            } else {
                opponentHandSize--;
            }
        } else {
            Card fetchedCard = service.getCardInfo(cardId);
            cardIdMap.put(cardId, fetchedCard);
            if (fetchedCard.getType().equalsIgnoreCase("minion")) {
                opponentBoard.remove(fetchedCard);
            } else {
                opponentHandSize--;
            }
        }
    }

    public void damageOpponent(int damage) {
        opponentLifeTotal -= damage;
    }

    public void opponentMinionBounced(String logLine) {
        Matcher matcher = cardNameRegex.matcher(logLine);
        String cardName;
        if (matcher.find()) {
            cardName = matcher.group(0).substring(0,matcher.group(0).length() - 1);
        } else {
            System.out.println("Error occurred during bounce.");
            return;
        }
        Card bounced = Card.getCardFromListByName(cardName, opponentBoard);
        opponentBoard.remove(bounced);
    }
}
